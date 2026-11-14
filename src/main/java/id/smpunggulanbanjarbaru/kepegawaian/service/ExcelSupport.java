package id.smpunggulanbanjarbaru.kepegawaian.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

/**
 * Helper bersama untuk membaca/menulis file Excel (.xlsx) pada fitur
 * template & import massal (pegawai, peserta didik, sarana, persuratan).
 */
public final class ExcelSupport {

    public static final String MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Batas baris per file import. Diturunkan dari uji memori container
     * 256 MB: 1.000 baris masih aman, sedangkan 1.500+ baris berisiko OOM.
     */
    static final int MAKS_BARIS = 1000;

    /** Format tanggal yang diterima saat import. */
    private static final DateTimeFormatter[] FORMAT_TANGGAL = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
    };

    private ExcelSupport() {
    }

    /* ================= validasi file unggahan ================= */

    static void validasiFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File kosong. Silakan pilih file Excel (.xlsx).");
        }
        String nama = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!nama.endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format file harus .xlsx (Excel 2007 ke atas).");
        }
    }

    /* ================= membaca sel ================= */

    /** Teks sel apa adanya; angka bulat tidak memakai notasi ilmiah. */
    static String teks(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> {
                String s = cell.getStringCellValue().trim();
                yield s.isEmpty() ? null : s;
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield null;
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    String s = cell.getStringCellValue().trim();
                    yield s.isEmpty() ? null : s;
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> null;
        };
    }

    /**
     * Membaca tanggal. Kosong -> null. Terisi tapi tidak valid -> IllegalArgumentException
     * dengan pesan yang menyebut nama kolom, supaya baris tersebut dilaporkan gagal.
     */
    static LocalDate tanggal(Cell cell, String namaKolom) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = teks(cell);
        if (s == null) return null;
        s = s.trim();
        for (DateTimeFormatter f : FORMAT_TANGGAL) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException ignored) {
                // coba format berikutnya
            }
        }
        throw new IllegalArgumentException(
                "kolom \"" + namaKolom + "\" tidak valid: \"" + s + "\" (gunakan format yyyy-MM-dd)");
    }

    /** Angka bulat; kosong -> null; tidak valid -> IllegalArgumentException. */
    static Integer bilangan(Cell cell, String namaKolom) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) Math.round(cell.getNumericCellValue());
        }
        String s = teks(cell);
        if (s == null) return null;
        String bersih = s.replace(" ", "");
        // pemisah ribuan gaya Indonesia: 1.200 atau 1.200.000
        if (bersih.matches("-?\\d{1,3}(\\.\\d{3})+")) bersih = bersih.replace(".", "");
        bersih = bersih.replace(",", ".");
        try {
            return new BigDecimal(bersih).intValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "kolom \"" + namaKolom + "\" harus berupa angka: \"" + s + "\"");
        }
    }

    /** Angka desimal (mis. luas m²); kosong -> null; tidak valid -> IllegalArgumentException. */
    static Double desimal(Cell cell, String namaKolom) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        String s = teks(cell);
        if (s == null) return null;
        String bersih = s.replace(" ", "").replace(",", ".");
        try {
            return new BigDecimal(bersih).doubleValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "kolom \"" + namaKolom + "\" harus berupa angka: \"" + s + "\"");
        }
    }

    static boolean barisKosong(Row row, int jumlahKolom) {
        if (row == null) return true;
        for (int i = 0; i < jumlahKolom; i++) {
            if (teks(row.getCell(i)) != null) return false;
        }
        return true;
    }

    /**
     * Memastikan file yang diunggah memang memakai template yang benar
     * (dicek dari beberapa header pertama).
     */
    static void validasiHeader(Sheet sheet, String[] kolom, int cek) {
        Row header = sheet.getRow(0);
        int n = Math.min(cek, kolom.length);
        for (int i = 0; i < n; i++) {
            String isi = teks(header == null ? null : header.getCell(i));
            String harap = kolom[i];
            if (isi == null || !isi.toLowerCase(Locale.ROOT).startsWith(awalan(harap))) {
                throw new IllegalArgumentException(
                        "File tidak sesuai template. Kolom ke-" + (i + 1)
                                + " seharusnya \"" + harap + "\". Unduh ulang template lalu isi sesuai petunjuk.");
            }
        }
    }

    private static String awalan(String header) {
        // bandingkan tanpa tanda bintang & keterangan dalam kurung
        String s = header.replace("*", "").split("\\(")[0].trim().toLowerCase(Locale.ROOT);
        return s;
    }

    /* ================= menulis template ================= */

    static CellStyle gayaHeader(Workbook wb) {
        CellStyle gaya = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        gaya.setFont(font);
        gaya.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        gaya.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        gaya.setAlignment(HorizontalAlignment.CENTER);
        gaya.setVerticalAlignment(VerticalAlignment.CENTER);
        gaya.setWrapText(true);
        return gaya;
    }

    static void tulisHeader(Sheet sheet, CellStyle gaya, String[] kolom) {
        Row header = sheet.createRow(0);
        header.setHeightInPoints(32);
        for (int i = 0; i < kolom.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(kolom[i]);
            c.setCellStyle(gaya);
            sheet.setColumnWidth(i, Math.min(45, Math.max(14, kolom[i].length() + 4)) * 256);
        }
        if (sheet instanceof org.apache.poi.xssf.usermodel.XSSFSheet xssf) {
            xssf.createFreezePane(0, 1);
        }
    }

    static void tulisPetunjuk(Workbook wb, String[] catatan) {
        Sheet sheet = wb.createSheet("Petunjuk");
        CellStyle judul = wb.createCellStyle();
        Font tebal = wb.createFont();
        tebal.setBold(true);
        judul.setFont(tebal);

        for (int i = 0; i < catatan.length; i++) {
            Row row = sheet.createRow(i);
            Cell c = row.createCell(0);
            c.setCellValue(catatan[i]);
            if (i == 0) c.setCellStyle(judul);
        }
        sheet.setColumnWidth(0, 110 * 256);
    }

    static byte[] jadikanXlsx(Workbook wb) {
        try (wb) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Gagal membuat file Excel: " + e.getMessage());
        }
    }
}
