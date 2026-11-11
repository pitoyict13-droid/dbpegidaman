package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.model.Surat;
import id.smpunggulanbanjarbaru.kepegawaian.repository.SuratRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Manajemen persuratan: arsip surat masuk & surat keluar.
 * Nomor agenda dibuat otomatis seperti Srikandi (Kalsel):
 * <urut 3 digit>/<kode klasifikasi>/<tahun>, contoh: 001/UM/2026.
 */
@Service
public class SuratService {

    private static final Set<String> TIPE_VALID = Set.of("MASUK", "KELUAR");

    private final SuratRepository repository;
    private final Path folderLampiran;

    public SuratService(SuratRepository repository,
                        @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.repository = repository;
        this.folderLampiran = Paths.get(uploadDir, "surat").toAbsolutePath().normalize();
    }

    /* ---------- daftar & detail ---------- */

    @Transactional(readOnly = true)
    public List<Surat> semua(String tipe, String cari) {
        String tipeFix = (tipe == null) ? null : tipe.trim().toUpperCase(Locale.ROOT);
        if (tipeFix != null && !TIPE_VALID.contains(tipeFix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipe surat harus MASUK atau KELUAR");
        }
        List<Surat> dasar = (tipeFix == null)
                ? repository.findAll()
                : repository.findByTipeOrderByIdAsc(tipeFix);

        if (cari == null || cari.isBlank()) return dasar;
        String q = cari.trim().toLowerCase(Locale.ROOT);
        return dasar.stream()
                .filter(s -> (s.getNomorAgenda() != null && s.getNomorAgenda().toLowerCase().contains(q))
                        || (s.getNomorSurat() != null && s.getNomorSurat().toLowerCase().contains(q))
                        || (s.getPerihal() != null && s.getPerihal().toLowerCase().contains(q))
                        || (s.getPengirim() != null && s.getPengirim().toLowerCase().contains(q))
                        || (s.getTujuan() != null && s.getTujuan().toLowerCase().contains(q))
                        || (s.getKlasifikasi() != null && s.getKlasifikasi().toLowerCase().contains(q)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Surat byId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data surat tidak ditemukan"));
    }

    /* ---------- simpan (tambah / ubah) ---------- */

    /** Membuat surat baru; nomor agenda otomatis, lampiran opsional. */
    @Transactional
    public Surat tambah(String tipe, String nomorSurat, String klasifikasi, LocalDate tanggalSurat,
                        String pengirim, String tujuan, String perihal, String sifatSurat,
                        String statusSurat, String catatan, MultipartFile lampiran) {
        String tipeFix = normalisasiTipe(tipe);
        Surat s = new Surat();
        s.setTipe(tipeFix);
        isiData(s, nomorSurat, klasifikasi, tanggalSurat, pengirim, tujuan, perihal, sifatSurat, statusSurat, catatan);
        s.setTanggalCatat(LocalDate.now());
        s.setNomorAgenda(buatNomorAgenda(tipeFix, klasifikasi));
        simpanLampiran(s, lampiran);
        return repository.save(s);
    }

    @Transactional
    public Surat ubah(Long id, String nomorSurat, String klasifikasi, LocalDate tanggalSurat,
                      String pengirim, String tujuan, String perihal, String sifatSurat,
                      String statusSurat, String catatan, MultipartFile lampiran) {
        Surat s = byId(id);
        isiData(s, nomorSurat, klasifikasi, tanggalSurat, pengirim, tujuan, perihal, sifatSurat, statusSurat, catatan);
        if (lampiran != null && !lampiran.isEmpty()) {
            hapusFileLama(s);
            simpanLampiran(s, lampiran);
        }
        return repository.save(s);
    }

    @Transactional
    public void hapus(Long id) {
        Surat s = byId(id);
        hapusFileLama(s);
        repository.delete(s);
    }

    /* ---------- nomor agenda gaya Srikandi ---------- */

    /** Format: 001/UM/2026 — urutan direset tiap tahun, dihitung per tipe surat. */
    private String buatNomorAgenda(String tipe, String klasifikasi) {
        int tahun = LocalDate.now().getYear();
        String kode = klasifikasi.trim().toUpperCase(Locale.ROOT);
        int urut = 1;
        String kandidat;
        do {
            kandidat = String.format("%03d/%s/%d", urut, kode, tahun);
            urut++;
        } while (repository.existsByNomorAgenda(kandidat) && urut < 1000);
        return kandidat;
    }

    /* ---------- lampiran file ---------- */

    public Path fileLampiran(Surat s) {
        if (s.getLampiranPath() == null) return null;
        Path file = folderLampiran.resolve(s.getLampiranPath()).normalize();
        if (!file.startsWith(folderLampiran)) return null; // cegah path traversal
        return file;
    }

    private void simpanLampiran(Surat s, MultipartFile lampiran) {
        if (lampiran == null || lampiran.isEmpty()) return;
        try {
            Files.createDirectories(folderLampiran);
            String asli = lampiran.getOriginalFilename();
            String namaServer = UUID.randomUUID().toString().replace("-", "")
                    + (asli != null && asli.contains(".")
                        ? asli.substring(asli.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "");
            Path tujuan = folderLampiran.resolve(namaServer);
            try (var in = lampiran.getInputStream()) {
                Files.copy(in, tujuan, StandardCopyOption.REPLACE_EXISTING);
            }
            s.setLampiranNama(asli);
            s.setLampiranPath(namaServer);
            s.setLampiranTipe(lampiran.getContentType());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal menyimpan lampiran surat");
        }
    }

    private void hapusFileLama(Surat s) {
        if (s.getLampiranPath() == null) return;
        try {
            Files.deleteIfExists(folderLampiran.resolve(s.getLampiranPath()));
        } catch (IOException ignored) { /* abaikan */ }
        s.setLampiranNama(null);
        s.setLampiranPath(null);
        s.setLampiranTipe(null);
    }

    /* ---------- helper validasi ---------- */

    private String normalisasiTipe(String tipe) {
        if (tipe == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipe surat wajib diisi");
        String t = tipe.trim().toUpperCase(Locale.ROOT);
        if (!TIPE_VALID.contains(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipe surat harus MASUK atau KELUAR");
        }
        return t;
    }

    private void isiData(Surat s, String nomorSurat, String klasifikasi, LocalDate tanggalSurat,
                         String pengirim, String tujuan, String perihal, String sifatSurat,
                         String statusSurat, String catatan) {
        if (nomorSurat == null || nomorSurat.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nomor surat wajib diisi");
        }
        if (klasifikasi == null || klasifikasi.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Klasifikasi surat wajib diisi");
        }
        if (tanggalSurat == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal surat wajib diisi");
        }
        if (perihal == null || perihal.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perihal wajib diisi");
        }
        if ("MASUK".equals(s.getTipe()) && (pengirim == null || pengirim.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pengirim wajib diisi untuk surat masuk");
        }
        if ("KELUAR".equals(s.getTipe()) && (tujuan == null || tujuan.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tujuan surat wajib diisi untuk surat keluar");
        }
        s.setNomorSurat(nomorSurat.trim());
        s.setKlasifikasi(klasifikasi.trim().toUpperCase(Locale.ROOT));
        s.setTanggalSurat(tanggalSurat);
        s.setPengirim(trimAtauNull(pengirim));
        s.setTujuan(trimAtauNull(tujuan));
        s.setPerihal(perihal.trim());
        s.setSifatSurat(trimAtauNull(sifatSurat));
        s.setStatusSurat(trimAtauNull(statusSurat));
        s.setCatatan(trimAtauNull(catatan));
    }

    private String trimAtauNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
