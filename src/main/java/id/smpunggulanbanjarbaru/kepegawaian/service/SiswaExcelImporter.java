package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.model.PesertaDidik;
import id.smpunggulanbanjarbaru.kepegawaian.repository.PesertaDidikRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Template dan import massal data peserta didik dari Excel (.xlsx).
 * Baris dengan NISN/NIS yang sudah terdaftar akan dilewati dan dilaporkan.
 */
@Service
public class SiswaExcelImporter {

    private static final String[] KOLOM = {
            "NISN", "NIS", "Nama*", "Jenis Kelamin (L/P)", "Tempat Lahir",
            "Tanggal Lahir (yyyy-MM-dd)", "Agama", "Alamat", "Kelurahan", "Kecamatan",
            "Kab/Kota", "No. HP", "Nama Ayah/Wali", "Nama Ibu/Wali",
            "Pekerjaan Ayah", "Pekerjaan Ibu", "Penghasilan Orang Tua",
            "No. HP Orang Tua", "Kelas", "Angkatan", "Status",
            "Tanggal Masuk (yyyy-MM-dd)", "Catatan"
    };

    private static final Map<String, String> AGAMA = Map.of(
            "islam", "Islam", "kristen", "Kristen", "katolik", "Katolik",
            "hindu", "Hindu", "buddha", "Buddha", "konghucu", "Konghucu");
    private static final Map<String, String> STATUS = Map.of(
            "aktif", "Aktif", "lulus", "Lulus", "pindah", "Pindah",
            "keluar", "Keluar", "meninggal", "Meninggal");

    private final PesertaDidikRepository repository;

    public SiswaExcelImporter(PesertaDidikRepository repository) {
        this.repository = repository;
    }

    /* ================= template ================= */

    public byte[] template() {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Data Siswa");
        ExcelSupport.tulisHeader(sheet, ExcelSupport.gayaHeader(wb), KOLOM);
        ExcelSupport.tulisPetunjuk(wb, new String[]{
                "PETUNJUK PENGISIAN DATA PESERTA DIDIK",
                "1. Isi data mulai baris ke-2 pada sheet \"Data Siswa\". Jangan mengubah/menggeser header.",
                "2. Kolom wajib: Nama. Kolom lain boleh dikosongkan.",
                "3. NISN dan NIS harus unik. Baris dengan NISN/NIS yang sudah ada akan dilewati.",
                "4. Jenis Kelamin: L atau P (boleh juga Laki-laki / Perempuan).",
                "5. Tanggal memakai format yyyy-MM-dd, contoh: 2012-07-17.",
                "6. Agama: Islam / Kristen / Katolik / Hindu / Buddha / Konghucu.",
                "7. Status: Aktif / Lulus / Pindah / Keluar / Meninggal. Kosong = Aktif.",
                "8. Angkatan = tahun masuk, 4 digit, contoh: 2024.",
                "9. Nomor HP sebaiknya diformat sebagai Teks agar angka 0 di depan tidak hilang.",
                "10. Simpan sebagai .xlsx lalu unggah lewat tombol Import Excel. Maksimal 1.000 baris per file.",
                "",
                "CONTOH PENGISIAN (jangan diketik di sheet data):",
                "NISN: 0071234567 | NIS: 2425001 | Nama: Budi Santoso | JK: L | Tempat Lahir: Banjarbaru",
                "Tanggal Lahir: 2012-07-17 | Agama: Islam | Kelas: 7A | Angkatan: 2024 | Status: Aktif"
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

            Set<String> nisnAda = new HashSet<>(repository.daftarNisn());
            Set<String> nisAda = new HashSet<>(repository.daftarNis());
            Set<String> nisnFile = new HashSet<>();
            Set<String> nisFile = new HashSet<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelSupport.barisKosong(row, KOLOM.length)) continue;
                int noBaris = r + 1;
                try {
                    String nisn = batas(ExcelSupport.teks(row.getCell(0)), 20, "NISN");
                    String nis = batas(ExcelSupport.teks(row.getCell(1)), 20, "NIS");
                    if (nisn != null && (!nisnFile.add(nisn) || nisnAda.contains(nisn))) {
                        throw new IllegalArgumentException("NISN " + nisn + " sudah terdaftar (baris dilewati)");
                    }
                    if (nis != null && (!nisFile.add(nis) || nisAda.contains(nis))) {
                        throw new IllegalArgumentException("NIS " + nis + " sudah terdaftar (baris dilewati)");
                    }

                    PesertaDidik p = new PesertaDidik();
                    p.setNisn(nisn);
                    p.setNis(nis);
                    p.setNama(wajib(ExcelSupport.teks(row.getCell(2)), "Nama", 150));
                    p.setJenisKelamin(jenisKelamin(ExcelSupport.teks(row.getCell(3))));
                    p.setTempatLahir(batas(ExcelSupport.teks(row.getCell(4)), 100, "Tempat Lahir"));
                    p.setTanggalLahir(ExcelSupport.tanggal(row.getCell(5), "Tanggal Lahir"));
                    p.setAgama(pilihan(ExcelSupport.teks(row.getCell(6)), AGAMA, "Agama"));
                    p.setAlamat(batas(ExcelSupport.teks(row.getCell(7)), 255, "Alamat"));
                    p.setKelurahan(batas(ExcelSupport.teks(row.getCell(8)), 100, "Kelurahan"));
                    p.setKecamatan(batas(ExcelSupport.teks(row.getCell(9)), 100, "Kecamatan"));
                    p.setKotaKabupaten(batas(ExcelSupport.teks(row.getCell(10)), 100, "Kab/Kota"));
                    p.setNomorHp(batas(ExcelSupport.teks(row.getCell(11)), 25, "No. HP"));
                    p.setNamaAyah(batas(ExcelSupport.teks(row.getCell(12)), 150, "Nama Ayah/Wali"));
                    p.setNamaIbu(batas(ExcelSupport.teks(row.getCell(13)), 150, "Nama Ibu/Wali"));
                    p.setPekerjaanAyah(batas(ExcelSupport.teks(row.getCell(14)), 50, "Pekerjaan Ayah"));
                    p.setPekerjaanIbu(batas(ExcelSupport.teks(row.getCell(15)), 50, "Pekerjaan Ibu"));
                    p.setPenghasilanOrangTua(batas(ExcelSupport.teks(row.getCell(16)), 50, "Penghasilan Orang Tua"));
                    p.setNomorHpOrangTua(batas(ExcelSupport.teks(row.getCell(17)), 25, "No. HP Orang Tua"));
                    p.setKelas(batas(ExcelSupport.teks(row.getCell(18)), 10, "Kelas"));

                    Integer angkatan = ExcelSupport.bilangan(row.getCell(19), "Angkatan");
                    if (angkatan != null && (angkatan < 1900 || angkatan > 2100)) {
                        throw new IllegalArgumentException("Angkatan harus tahun 4 digit (mis. 2024)");
                    }
                    p.setAngkatan(angkatan);

                    String status = pilihan(ExcelSupport.teks(row.getCell(20)), STATUS, "Status");
                    p.setStatus(status == null ? "Aktif" : status);
                    p.setTanggalMasuk(ExcelSupport.tanggal(row.getCell(21), "Tanggal Masuk"));
                    p.setCatatan(batas(ExcelSupport.teks(row.getCell(22)), 255, "Catatan"));

                    repository.save(p);
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

    private String batas(String nilai, int maks, String label) {
        if (nilai == null) return null;
        String v = nilai.trim();
        if (v.isEmpty()) return null;
        if (v.length() > maks) {
            throw new IllegalArgumentException("kolom " + label + " terlalu panjang (maks " + maks + " karakter)");
        }
        return v;
    }

    private String jenisKelamin(String nilai) {
        if (nilai == null) return null;
        String u = nilai.trim().toUpperCase(Locale.ROOT);
        if (u.equals("L") || u.startsWith("LAKI") || u.equals("PRIA") || u.equals("M")) return "L";
        if (u.equals("P") || u.startsWith("PEREMPUAN") || u.equals("WANITA") || u.equals("F")) return "P";
        throw new IllegalArgumentException("jenis kelamin harus L atau P");
    }

    /** Cocokkan pilihan dengan daftar resmi (tanpa peduli huruf besar/kecil). */
    private String pilihan(String nilai, Map<String, String> daftar, String label) {
        if (nilai == null) return null;
        String kunci = nilai.trim().toLowerCase(Locale.ROOT);
        String baku = daftar.get(kunci);
        if (baku == null) {
            throw new IllegalArgumentException(
                    "kolom " + label + " \"" + nilai + "\" tidak dikenal. Pilihan: " + daftar.values());
        }
        return baku;
    }

    private String pesanSingkat(Exception e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > 250 ? m.substring(0, 250) + "..." : m;
    }
}
