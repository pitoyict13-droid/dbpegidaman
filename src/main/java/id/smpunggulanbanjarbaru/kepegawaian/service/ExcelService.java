package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.model.Pegawai;
import id.smpunggulanbanjarbaru.kepegawaian.repository.PegawaiRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Membuat template Excel untuk data pegawai dan melakukan import
 * massal dari file Excel (.xlsx) ke database.
 */
@Service
public class ExcelService {

    /** Urutan kolom pada template (harus sama dengan header yang ditulis). */
    private static final String[] HEADER = {
            "NIP", "Nama", "Jenis Kelamin (L/P)", "Jabatan", "Status Kepegawaian",
            "Mata Pelajaran", "Pendidikan Terakhir", "Tanggal Lahir (yyyy-MM-dd)",
            "Tanggal Masuk (yyyy-MM-dd)", "Telepon", "Email", "Gaji Pokok (Rp)", "Alamat"
    };

    private final PegawaiRepository repository;

    public ExcelService(PegawaiRepository repository) {
        this.repository = repository;
    }

    /* ================= TEMPLATE ================= */

    public byte[] buatTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data Pegawai");

            // Style header tebal + latar biru
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Baris header
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADER.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADER[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Baris contoh
            Row contoh = sheet.createRow(1);
            String[] contohData = {
                    "198705122010011005", "Budi Santoso (contoh - hapus baris ini)", "L", "Guru",
                    "PNS", "Matematika", "S1", "1987-05-12", "2010-01-10",
                    "081234567890", "budi.santoso@contoh.id", "4500000", "Jl. Merdeka No. 10, Banjarbaru"
            };
            for (int i = 0; i < contohData.length; i++) {
                contoh.createCell(i).setCellValue(contohData[i]);
            }

            // Sheet petunjuk
            Sheet petunjuk = wb.createSheet("Petunjuk");
            String[] catatan = {
                    "PETUNJUK PENGISIAN:",
                    "1. Isi data mulai dari baris ke-3 (baris contoh boleh dihapus).",
                    "2. Kolom wajib: Nama, Jabatan, Status Kepegawaian.",
                    "3. NIP boleh kosong untuk pegawai non-ASN (GTT/PTT/Honorer).",
                    "4. Jenis Kelamin hanya boleh huruf L atau P.",
                    "5. Status Kepegawaian: PNS / PPPK / GTT / PTT / Honorer.",
                    "6. Pendidikan Terakhir: SMA/SMK / D3 / S1 / S2 / S3.",
                    "7. Tanggal gunakan format yyyy-MM-dd, contoh: 1990-05-17.",
                    "8. Gaji Pokok hanya angka tanpa titik/koma, contoh: 4500000.",
                    "9. Jangan menambah/menggeser kolom header.",
                    "10. Simpan file dalam format .xlsx lalu unggah melalui aplikasi."
            };
            for (int i = 0; i < catatan.length; i++) {
                petunjuk.createRow(i).createCell(0).setCellValue(catatan[i]);
            }
            petunjuk.setColumnWidth(0, 70 * 256);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal membuat template: " + e.getMessage());
        }
    }

    /* ================= IMPORT ================= */

    public Map<String, Object> importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File kosong. Silakan pilih file Excel.");
        }
        String namaFile = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        if (!namaFile.endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format file harus .xlsx (Excel).");
        }

        int sukses = 0, gagal = 0;
        List<String> pesanGagal = new ArrayList<>();
        Set<String> nipDipakai = new HashSet<>();

        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int barisMulai = 1; // lewati baris header (indeks 0)

            for (int r = barisMulai; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || barisKosong(row)) continue;

                String nama = stringOf(row.getCell(1));
                String jabatan = stringOf(row.getCell(3));
                String status = stringOf(row.getCell(4));
                String nip = stringOf(row.getCell(0));

                // Validasi wajib
                if (nama == null || nama.isBlank()) {
                    gagal++;
                    pesanGagal.add("Baris " + (r + 1) + ": kolom Nama kosong");
                    continue;
                }
                if (jabatan == null || jabatan.isBlank()) {
                    gagal++;
                    pesanGagal.add("Baris " + (r + 1) + ": kolom Jabatan kosong");
                    continue;
                }
                if (status == null || status.isBlank()) {
                    gagal++;
                    pesanGagal.add("Baris " + (r + 1) + ": kolom Status Kepegawaian kosong");
                    continue;
                }
                // NIP duplikat (dalam file maupun di database)
                if (nip != null && !nip.isBlank()) {
                    if (!nipDipakai.add(nip) || repository.existsByNip(nip)) {
                        gagal++;
                        pesanGagal.add("Baris " + (r + 1) + ": NIP " + nip + " sudah terdaftar");
                        continue;
                    }
                }

                try {
                    repository.save(kePegawai(row, nip, nama, jabatan, status));
                    sukses++;
                } catch (Exception e) {
                    gagal++;
                    pesanGagal.add("Baris " + (r + 1) + ": " + e.getMessage());
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File Excel tidak dapat dibaca. Pastikan sesuai template. (" + e.getMessage() + ")");
        }

        Map<String, Object> hasil = new LinkedHashMap<>();
        hasil.put("totalBaris", sukses + gagal);
        hasil.put("berhasil", sukses);
        hasil.put("gagal", gagal);
        hasil.put("pesanGagal", pesanGagal);
        return hasil;
    }

    private Pegawai kePegawai(Row row, String nip, String nama, String jabatan, String status) {
        Pegawai p = new Pegawai();
        p.setNip((nip == null || nip.isBlank()) ? null : nip);
        p.setNama(nama.trim());
        String jk = stringOf(row.getCell(2));
        p.setJenisKelamin(jk == null ? null : jk.toUpperCase().substring(0, Math.min(1, jk.length())));
        p.setJabatan(jabatan.trim());
        p.setStatusKepegawaian(status.trim());
        p.setMataPelajaran(stringOf(row.getCell(5)));
        p.setPendidikanTerakhir(stringOf(row.getCell(6)));
        p.setTanggalLahir(tanggalOf(row.getCell(7)));
        p.setTanggalMasuk(tanggalOf(row.getCell(8)));
        p.setTelepon(stringOf(row.getCell(9)));
        p.setEmail(stringOf(row.getCell(10)));
        p.setGajiPokok(angkaOf(row.getCell(11)));
        p.setAlamat(stringOf(row.getCell(12)));
        return p;
    }

    /* ================= helper baca sel ================= */

    private boolean barisKosong(Row row) {
        for (int i = 0; i < HEADER.length; i++) {
            String s = stringOf(row.getCell(i));
            if (s != null && !s.isBlank()) return false;
        }
        return true;
    }

    private String stringOf(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield null;
                double d = cell.getNumericCellValue();
                // angka bulat (NIP/telepon) tanpa notasi ilmiah
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> null;
        };
    }

    private LocalDate tanggalOf(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = stringOf(cell);
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException e) {
                return null; // tanggal tidak valid -> biarkan kosong
            }
        }
    }

    private BigDecimal angkaOf(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String s = stringOf(cell);
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
