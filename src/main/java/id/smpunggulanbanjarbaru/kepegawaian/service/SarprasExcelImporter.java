package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.model.SaranaPrasarana;
import id.smpunggulanbanjarbaru.kepegawaian.repository.SaranaPrasaranaRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Map;

/**
 * Template dan import massal data sarana prasarana dari Excel (.xlsx).
 */
@Service
public class SarprasExcelImporter {

    private static final String[] KOLOM = {
            "Kategori*", "Nama*", "Kode/No. Inventaris", "Jumlah", "Kondisi*",
            "Luas (m2)", "Tahun Pengadaan", "Sumber Dana", "Keterangan"
    };

    private static final Map<String, String> KATEGORI = Map.ofEntries(
            Map.entry("gedung", "Gedung"),
            Map.entry("ruang kelas", "Ruang Kelas"),
            Map.entry("laboratorium", "Laboratorium"),
            Map.entry("perpustakaan", "Perpustakaan"),
            Map.entry("ruang kantor", "Ruang Kantor"),
            Map.entry("toilet/mck", "Toilet/MCK"),
            Map.entry("toilet", "Toilet/MCK"),
            Map.entry("lapangan/olahraga", "Lapangan/Olahraga"),
            Map.entry("lapangan", "Lapangan/Olahraga"),
            Map.entry("alat peraga/peralatan", "Alat Peraga/Peralatan"),
            Map.entry("alat peraga", "Alat Peraga/Peralatan"),
            Map.entry("furnitur/mebel", "Furnitur/Mebel"),
            Map.entry("furnitur", "Furnitur/Mebel"),
            Map.entry("buku & media", "Buku & Media"),
            Map.entry("buku dan media", "Buku & Media"),
            Map.entry("lainnya", "Lainnya")
    );

    private static final Map<String, String> KONDISI = Map.of(
            "baik", "Baik",
            "rusak ringan", "Rusak Ringan",
            "rusak sedang", "Rusak Sedang",
            "rusak berat", "Rusak Berat"
    );

    private static final Map<String, String> SUMBER_DANA = Map.of(
            "bos", "BOS",
            "apbd", "APBD",
            "apbn", "APBN",
            "dana mandiri", "Dana Mandiri",
            "lainnya", "Lainnya"
    );

    private final SaranaPrasaranaRepository repository;

    public SarprasExcelImporter(SaranaPrasaranaRepository repository) {
        this.repository = repository;
    }

    /* ================= template ================= */

    public byte[] template() {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Data Sarpras");
        ExcelSupport.tulisHeader(sheet, ExcelSupport.gayaHeader(wb), KOLOM);
        ExcelSupport.tulisPetunjuk(wb, new String[]{
                "PETUNJUK PENGISIAN DATA SARANA PRASARANA",
                "1. Isi data mulai baris ke-2 pada sheet \"Data Sarpras\". Jangan mengubah/menggeser header.",
                "2. Kolom wajib: Kategori dan Nama.",
                "3. Kategori yang dikenal: Gedung; Ruang Kelas; Laboratorium; Perpustakaan; Ruang Kantor;",
                "   Toilet/MCK; Lapangan/Olahraga; Alat Peraga/Peralatan; Furnitur/Mebel; Buku & Media; Lainnya.",
                "4. Kondisi: Baik / Rusak Ringan / Rusak Sedang / Rusak Berat. Kosong = Baik.",
                "5. Jumlah dan Luas diisi angka (Luas dalam m2, boleh desimal).",
                "6. Tahun Pengadaan 4 digit (1900-2100).",
                "7. Sumber Dana: BOS / APBD / APBN / Dana Mandiri / Lainnya (boleh dikosongkan).",
                "8. Simpan sebagai .xlsx lalu unggah lewat tombol Import Excel. Maksimal 1.000 baris per file.",
                "",
                "CONTOH PENGISIAN (jangan diketik di sheet data):",
                "Kategori: Ruang Kelas | Nama: Ruang Kelas VII-A | Kode: INV-2024-001 | Jumlah: 1",
                "Kondisi: Baik | Luas: 63 | Tahun: 2020 | Sumber Dana: BOS | Keterangan: lantai 1"
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

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelSupport.barisKosong(row, KOLOM.length)) continue;
                int noBaris = r + 1;
                try {
                    SaranaPrasarana s = new SaranaPrasarana();
                    s.setKategori(wajibPilihan(ExcelSupport.teks(row.getCell(0)), KATEGORI, "Kategori"));
                    s.setNama(wajib(ExcelSupport.teks(row.getCell(1)), "Nama", 150));
                    s.setKode(batas(ExcelSupport.teks(row.getCell(2)), 50, "Kode"));

                    Integer jumlah = ExcelSupport.bilangan(row.getCell(3), "Jumlah");
                    if (jumlah != null && jumlah < 0) {
                        throw new IllegalArgumentException("Jumlah tidak boleh negatif");
                    }
                    s.setJumlah(jumlah);

                    String kondisi = pilihan(ExcelSupport.teks(row.getCell(4)), KONDISI, "Kondisi");
                    s.setKondisi(kondisi == null ? "Baik" : kondisi);

                    Double luas = ExcelSupport.desimal(row.getCell(5), "Luas");
                    if (luas != null && luas < 0) {
                        throw new IllegalArgumentException("Luas tidak boleh negatif");
                    }
                    s.setLuas(luas);

                    Integer tahun = ExcelSupport.bilangan(row.getCell(6), "Tahun Pengadaan");
                    if (tahun != null && (tahun < 1900 || tahun > 2100)) {
                        throw new IllegalArgumentException("Tahun Pengadaan harus 1900-2100");
                    }
                    s.setTahunPengadaan(tahun);
                    s.setSumberDana(pilihan(ExcelSupport.teks(row.getCell(7)), SUMBER_DANA, "Sumber Dana"));
                    s.setKeterangan(batas(ExcelSupport.teks(row.getCell(8)), 500, "Keterangan"));

                    repository.save(s);
                    job.sukses();
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

    private String wajib(String nilai, String label, int maks) {
        if (nilai == null) throw new IllegalArgumentException("kolom " + label + " wajib diisi");
        return batas(nilai, maks, label);
    }

    private String wajibPilihan(String nilai, Map<String, String> daftar, String label) {
        String hasil = pilihan(nilai, daftar, label);
        if (hasil == null) throw new IllegalArgumentException("kolom " + label + " wajib diisi");
        return hasil;
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
                            + new java.util.TreeSet<>(daftar.values()));
        }
        return baku;
    }

    private String pesanSingkat(Exception e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > 250 ? m.substring(0, 250) + "..." : m;
    }
}
