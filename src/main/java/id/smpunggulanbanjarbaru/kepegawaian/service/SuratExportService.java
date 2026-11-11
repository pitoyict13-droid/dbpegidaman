package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.model.Surat;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Membuat laporan Excel daftar surat masuk / keluar (buku agenda).
 */
@Service
public class SuratExportService {

    private static final DateTimeFormatter TGL = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public byte[] buatLaporan(String tipe, List<Surat> daftar) {
        boolean masuk = "MASUK".equalsIgnoreCase(tipe);
        String[] header = {
                "No", "Nomor Agenda", "Nomor Surat", "Tanggal Surat",
                masuk ? "Pengirim" : "Tujuan", "Klasifikasi", "Perihal", "Sifat", "Status"
        };

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Surat " + (masuk ? "Masuk" : "Keluar"));

            /* ---------- judul laporan ---------- */
            CellStyle judulStyle = wb.createCellStyle();
            Font fontJudul = wb.createFont();
            fontJudul.setBold(true);
            fontJudul.setFontHeightInPoints((short) 14);
            judulStyle.setFont(fontJudul);
            judulStyle.setAlignment(HorizontalAlignment.CENTER);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("SMP UNGGULAN IDAMAN BANJARBARU");
            c0.setCellStyle(judulStyle);
            r0.createCell(header.length - 1);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("BUKU AGENDA SURAT " + (masuk ? "MASUK" : "KELUAR"));
            c1.setCellStyle(judulStyle);
            r1.createCell(header.length - 1);

            /* ---------- header tabel ---------- */
            CellStyle headerStyle = wb.createCellStyle();
            Font fontHeader = wb.createFont();
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(fontHeader);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row hr = sheet.createRow(3);
            for (int i = 0; i < header.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(header[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, i == 6 ? 46 * 256 : 20 * 256);
            }

            /* ---------- isi ---------- */
            CellStyle tanggalStyle = wb.createCellStyle();
            tanggalStyle.setAlignment(HorizontalAlignment.CENTER);

            int row = 4, no = 1;
            for (Surat s : daftar) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(no++);
                r.createCell(1).setCellValue(s.getNomorAgenda() == null ? "" : s.getNomorAgenda());
                r.createCell(2).setCellValue(s.getNomorSurat());
                Cell ct = r.createCell(3);
                ct.setCellValue(s.getTanggalSurat().format(TGL));
                ct.setCellStyle(tanggalStyle);
                r.createCell(4).setCellValue(masuk
                        ? (s.getPengirim() == null ? "" : s.getPengirim())
                        : (s.getTujuan() == null ? "" : s.getTujuan()));
                r.createCell(5).setCellValue(s.getKlasifikasi());
                r.createCell(6).setCellValue(s.getPerihal());
                r.createCell(7).setCellValue(s.getSifatSurat() == null ? "Biasa" : s.getSifatSurat());
                r.createCell(8).setCellValue(s.getStatusSurat() == null ? "" : s.getStatusSurat());
            }

            /* ---------- tanda tangan ---------- */
            row++;
            Row r1tt = sheet.createRow(row);
            r1tt.createCell(header.length - 3).setCellValue("Banjarbaru, " + java.time.LocalDate.now().format(TGL));
            Row r2tt = sheet.createRow(row + 1);
            r2tt.createCell(header.length - 3).setCellValue("Kepala SMP Unggulan Idaman Banjarbaru");
            Row r4tt = sheet.createRow(row + 4);
            r4tt.createCell(header.length - 3).setCellValue("( ______________________ )");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membuat laporan Excel", e);
        }
    }
}
