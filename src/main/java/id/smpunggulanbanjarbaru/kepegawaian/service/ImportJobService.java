package id.smpunggulanbanjarbaru.kepegawaian.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Menjalankan import Excel di latar belakang agar tidak terkena timeout
 * (hosting gratis membatasi waktu respons, sedangkan menulis ribuan baris
 * ke database cloud bisa memakan waktu).
 */
@Service
public class ImportJobService {

    private static final Logger log = LoggerFactory.getLogger(ImportJobService.class);
    private static final Set<String> JENIS = Set.of("siswa", "sarpras", "surat");
    private static final long MAKS_UKURAN_FILE = 8L * 1024 * 1024; // 8 MB
    private static final Duration UMUR_JOB = Duration.ofMinutes(60);

    private final Map<String, ImportJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService eksekutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "import-excel");
        t.setDaemon(true);
        return t;
    });

    private final SiswaExcelImporter siswaImporter;
    private final SarprasExcelImporter sarprasImporter;
    private final SuratExcelImporter suratImporter;

    public ImportJobService(SiswaExcelImporter siswaImporter,
                            SarprasExcelImporter sarprasImporter,
                            SuratExcelImporter suratImporter) {
        this.siswaImporter = siswaImporter;
        this.sarprasImporter = sarprasImporter;
        this.suratImporter = suratImporter;
    }

    /* ================= template ================= */

    public byte[] template(String jenis) {
        return switch (normalisasi(jenis)) {
            case "siswa" -> siswaImporter.template();
            case "sarpras" -> sarprasImporter.template();
            case "surat" -> suratImporter.template();
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Jenis import tidak dikenal");
        };
    }

    /* ================= mulai import ================= */

    public synchronized ImportJob mulai(String jenis, MultipartFile file) {
        String j = normalisasi(jenis);
        ExcelSupport.validasiFile(file);
        if (file.getSize() > MAKS_UKURAN_FILE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ukuran file terlalu besar (maks 8 MB).");
        }
        // Hanya satu import pada satu waktu: file yang menunggu di antrean
        // ikut menahan memori (byte[]), berisiko OOM di container 256 MB.
        boolean sibuk = jobs.values().stream().anyMatch(k ->
                k.getStatus() == ImportJob.Status.MENUNGGU || k.getStatus() == ImportJob.Status.BERJALAN);
        if (sibuk) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Masih ada proses import yang berjalan. Tunggu sampai selesai lalu coba lagi.");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File tidak dapat dibaca: " + e.getMessage());
        }

        ImportJob job = new ImportJob(UUID.randomUUID().toString().substring(0, 8), j);
        bersihkanJobLama();
        jobs.put(job.getId(), job);

        // Pakai execute(), bukan submit(): submit() menelan Throwable ke dalam Future
        // yang tidak pernah dibaca, sehingga job bisa menggantung selamanya di BERJALAN.
        eksekutor.execute(() -> jalankan(job, data));
        return job;
    }

    private void jalankan(ImportJob job, byte[] data) {
        job.mulai();
        try {
            switch (job.getJenis()) {
                case "siswa" -> siswaImporter.impor(data, job);
                case "sarpras" -> sarprasImporter.impor(data, job);
                case "surat" -> suratImporter.impor(data, job);
                default -> throw new IllegalArgumentException("Jenis import tidak dikenal");
            }
            job.selesai();
            log.info("Import {} selesai: {} berhasil, {} gagal", job.getJenis(), job.getBerhasil(), job.getGagal());
        } catch (IllegalArgumentException e) {
            job.gagalTotal(e.getMessage());
            log.warn("Import {} gagal: {}", job.getJenis(), e.getMessage());
        } catch (OutOfMemoryError e) {
            job.gagalTotal("Server kehabisan memori saat memproses file. "
                    + "Silakan bagi file menjadi beberapa bagian (mis. 500 baris) lalu unggah ulang.");
            log.error("Import {} kehabisan memori", job.getJenis(), e);
        } catch (Throwable e) {
            job.gagalTotal("Import gagal: " + pesan(e));
            log.error("Import {} gagal", job.getJenis(), e);
        }
    }

    private String pesan(Throwable e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) return e.getClass().getSimpleName();
        return m.length() > 300 ? m.substring(0, 300) + "..." : m;
    }

    /* ================= status ================= */

    public ImportJob status(String jobId) {
        ImportJob job = jobs.get(jobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Proses import tidak ditemukan atau sudah kedaluwarsa. Silakan unggah ulang.");
        }
        return job;
    }

    private String normalisasi(String jenis) {
        if (jenis == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jenis import wajib diisi");
        }
        String j = jenis.trim().toLowerCase(Locale.ROOT);
        if (!JENIS.contains(j)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Jenis import \"" + jenis + "\" tidak dikenal. Pilihan: " + JENIS);
        }
        return j;
    }

    private void bersihkanJobLama() {
        Instant batas = Instant.now().minus(UMUR_JOB);
        jobs.entrySet().removeIf(e -> {
            Instant selesai = e.getValue().getSelesai();
            return selesai != null && selesai.isBefore(batas);
        });
    }

    @PreDestroy
    void hentikan() {
        eksekutor.shutdownNow();
        try {
            eksekutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
