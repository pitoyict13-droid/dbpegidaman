package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.repository.SuratRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Template dan import massal arsip persuratan (surat masuk & keluar).
 * Nomor agenda tetap dibuat otomatis oleh {@link SuratService}.
 */
@Service
public class SuratExcelImporter {

    private static final String[] KOLOM = {
            "Tipe (MASUK/KELUAR)*", "Nomor Surat*", "Klasifikasi*", "Tanggal Surat (yyyy-MM-dd)*",
            "Pengirim (wajib jika MASUK)", "Tujuan (wajib jika KELUAR)", "Perihal*",
            "Sifat Surat", "Status Surat", "Tanggal Catat (opsional)", "Catatan"
    };

    private static final Set<String> KLASIFIKASI = new TreeSet<>(List.of(
            "UM", "PT", "SN", "KP", "UN", "SI", "PG", "PU", "KR",
            "RH", "RT", "ST", "PM", "PR", "SS", "DU", "LD"));

    private static final Map<String, String> SIFAT = Map.of(
            "biasa", "Biasa",
            "penting", "Penting",
            "segera", "Segera",
            "rahasia", "Rahasia"
    );

    private static final Map<String, String> STATUS_MASUK = Map.of(
            "belum diproses", "Belum Diproses",
            "diproses", "Diproses",
            "selesai", "Selesai"
    );

    private static final Map<String, String> STATUS_KELUAR = Map.of(
            "terkirim", "Terkirim",
            "arsip", "Arsip"
    );

    private final SuratService suratService;
    private final SuratRepository repository;

    public SuratExcelImporter(SuratService suratService, SuratRepository repository) {
        this.suratService = suratService;
        this.repository = repository;
    }

    /* ================= template ================= */

    public byte[] template() {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Data Surat");
        ExcelSupport.tulisHeader(sheet, ExcelSupport.gayaHeader(wb), KOLOM);
        ExcelSupport.tulisPetunjuk(wb, new String[]{
                "PETUNJUK PENGISIAN ARSIP PERSURATAN",
                "1. Isi data mulai baris ke-2 pada sheet \"Data Surat\". Jangan mengubah/menggeser header.",
                "2. Tipe wajib MASUK atau KELUAR.",
                "3. Nomor Surat wajib dan harus unik (baris dengan nomor surat yang sudah ada akan dilewati).",
                "4. Klasifikasi wajib berupa kode: UM, PT, SN, KP, UN, SI, PG, PU, KR, RH, RT, ST, PM, PR, SS, DU, LD.",
                "5. Tanggal Surat wajib, format yyyy-MM-dd, contoh: 2026-01-15.",
                "6. Surat MASUK wajib mengisi Pengirim; surat KELUAR wajib mengisi Tujuan.",
                "7. Perihal wajib diisi.",
                "8. Sifat Surat: Biasa / Penting / Segera / Rahasia. Kosong = Biasa.",
                "9. Status surat MASUK: Belum Diproses / Diproses / Selesai.",
                "   Status surat KELUAR: Terkirim / Arsip. Kosong = mengikuti tipe.",
                "10. Tanggal Catat opsional (default hari ini). Nomor agenda dibuat otomatis oleh sistem.",
                "11. Simpan sebagai .xlsx lalu unggah lewat tombol Import Excel. Maksimal 1.000 baris per file.",
                "",
                "CONTOH PENGISIAN (jangan diketik di sheet data):",
                "Tipe: MASUK | Nomor Surat: 421.2/123/DISDIK/2026 | Klasifikasi: UM",
                "Tanggal Surat: 2026-01-15 | Pengirim: Dinas Pendidikan | Perihal: Undangan Rapat | Sifat: Penting"
        });
        return ExcelSupport.jadikanXlsx(wb);
    }

    /* ================= import ================= */

    public void impor(byte[] data, ImportJob job) {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Sheet sheet = wb.getSheetAt(0);
            ExcelSupport.validasiHeader(sheet, KOLOM, 3);

            int total = 0;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                if (!ExcelSupport.barisKosong(sheet.getRow(r), KOLOM.length)) total++;
            }
            if (total == 0) {
                throw new IllegalArgumentException(
                        "Tidak ada data pada sheet \"" + sheet.getSheetName() + "\". Isi mulai baris ke-2.");
            }
            if (total > ExcelSupport.MAKS_BARIS) {
                throw new IllegalArgumentException(
                        "File berisi " + total + " baris. Maksimal " + ExcelSupport.MAKS_BARIS + " baris per file — silakan dibagi.");
            }
            job.setTotal(total);

            Set<String> nomorAda = new HashSet<>(repository.daftarNomorSurat());
            Set<String> nomorFile = new HashSet<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelSupport.barisKosong(row, KOLOM.length)) continue;
                int noBaris = r + 1;
                try {
                    String tipe = tipe(row.getCell(0));
                    String nomorSurat = wajib(ExcelSupport.teks(row.getCell(1)), "Nomor Surat", 100);
                    if (!nomorFile.add(nomorSurat) || nomorAda.contains(nomorSurat)) {
                        throw new IllegalArgumentException(
                                "Nomor Surat " + nomorSurat + " sudah ada (baris dilewati)");
                    }

                    String kode = ExcelSupport.teks(row.getCell(2));
                    if (kode == null) throw new IllegalArgumentException("kolom Klasifikasi wajib diisi");
                    String klasifikasi = kode.trim().toUpperCase(Locale.ROOT);
                    if (!KLASIFIKASI.contains(klasifikasi)) {
                        throw new IllegalArgumentException(
                                "Klasifikasi \"" + kode + "\" tidak dikenal. Kode yang tersedia: " + KLASIFIKASI);
                    }

                    LocalDate tanggalSurat = ExcelSupport.tanggal(row.getCell(3), "Tanggal Surat");
                    if (tanggalSurat == null) {
                        throw new IllegalArgumentException("kolom Tanggal Surat wajib diisi");
                    }

                    String pengirim = batas(ExcelSupport.teks(row.getCell(4)), 150, "Pengirim");
                    String tujuan = batas(ExcelSupport.teks(row.getCell(5)), 150, "Tujuan");
                    String perihal = wajib(ExcelSupport.teks(row.getCell(6)), "Perihal", 255);
                    String sifat = pilihan(ExcelSupport.teks(row.getCell(7)), SIFAT, "Sifat Surat");
                    String status = statusSurat(ExcelSupport.teks(row.getCell(8)), tipe);
                    LocalDate tanggalCatat = ExcelSupport.tanggal(row.getCell(9), "Tanggal Catat");
                    String catatan = batas(ExcelSupport.teks(row.getCell(10)), 255, "Catatan");

                    suratService.tambahDariImport(tipe, nomorSurat, klasifikasi, tanggalSurat,
                            pengirim, tujuan, perihal, sifat == null ? "Biasa" : sifat,
                            status, catatan, tanggalCatat);
                    job.sukses();
                } catch (ResponseStatusException e) {
                    job.gagal("Baris " + noBaris + ": " + e.getReason());
                } catch (Exception e) {
                    job.gagal("Baris " + noBaris + ": " + pesanSingkat(e));
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "File Excel tidak dapat dibaca. Pastikan memakai template terbaru. (" + e.getMessage() + ")");
        }
    }

    /* ================= helper validasi ================= */

    private String tipe(org.apache.poi.ss.usermodel.Cell cell) {
        String nilai = ExcelSupport.teks(cell);
        if (nilai == null) throw new IllegalArgumentException("kolom Tipe wajib diisi (MASUK/KELUAR)");
        String t = nilai.trim().toUpperCase(Locale.ROOT);
        if (!t.equals("MASUK") && !t.equals("KELUAR")) {
            throw new IllegalArgumentException("Tipe harus MASUK atau KELUAR");
        }
        return t;
    }

    private String statusSurat(String nilai, String tipe) {
        Map<String, String> daftar = "MASUK".equals(tipe) ? STATUS_MASUK : STATUS_KELUAR;
        String status = pilihan(nilai, daftar, "Status Surat");
        if (status != null) return status;
        return "MASUK".equals(tipe) ? "Belum Diproses" : "Terkirim";
    }

    private String wajib(String nilai, String label, int maks) {
        if (nilai == null) throw new IllegalArgumentException("kolom " + label + " wajib diisi");
        return batas(nilai, maks, label);
    }

    private String batas(String nilai, int maks, String label) {
        if (nilai == null) return null;
        String v = nilai.trim();
        if (v.isEmpty()) return null;
        if (v.length() > maks) {
            throw new IllegalArgumentException("kolom " + label + " terlalu panjang (maks " + maks + " karakter)");
        }
        return v;
    }

    private String pilihan(String nilai, Map<String, String> daftar, String label) {
        if (nilai == null) return null;
        String kunci = nilai.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        String baku = daftar.get(kunci);
        if (baku == null) {
            throw new IllegalArgumentException(
                    "kolom " + label + " \"" + nilai + "\" tidak dikenal. Pilihan: "
                            + new TreeSet<>(daftar.values()));
        }
        return baku;
    }

    private String pesanSingkat(Exception e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > 250 ? m.substring(0, 250) + "..." : m;
    }
}
