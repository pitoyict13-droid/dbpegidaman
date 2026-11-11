package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.model.Disposisi;
import id.smpunggulanbanjarbaru.kepegawaian.model.Surat;
import id.smpunggulanbanjarbaru.kepegawaian.service.SuratExportService;
import id.smpunggulanbanjarbaru.kepegawaian.service.SuratService;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/**
 * REST API manajemen persuratan (arsip surat masuk & keluar).
 * Lihat daftar/detail: wajib login; tambah/ubah/hapus: khusus ADMIN
 * (dijaga di SecurityConfig).
 */
@RestController
@RequestMapping("/api/surat")
public class SuratController {

    private final SuratService service;
    private final SuratExportService exportService;

    public SuratController(SuratService service, SuratExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    /** GET /api/surat?tipe=MASUK&cari=umum — daftar arsip surat. */
    @GetMapping
    public List<Surat> daftar(@RequestParam(required = false) String tipe,
                              @RequestParam(required = false) String cari) {
        return service.semua(tipe, cari);
    }

    @GetMapping("/{id}")
    public Surat detail(@PathVariable Long id) {
        return service.byId(id);
    }

    /** Unduh lampiran (file scan/softcopy surat). */
    @GetMapping("/{id}/lampiran")
    public ResponseEntity<Resource> lampiran(@PathVariable Long id) {
        Surat s = service.byId(id);
        java.nio.file.Path file = service.fileLampiran(s);
        if (file == null || !Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Surat ini tidak memiliki lampiran");
        }
        String nama = (s.getLampiranNama() != null) ? s.getLampiranNama() : "lampiran";
        MediaType tipe = (s.getLampiranTipe() != null)
                ? MediaType.parseMediaType(s.getLampiranTipe())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(tipe)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nama + "\"")
                .body(new PathResource(file));
    }

    /** POST /api/surat — data dikirim sebagai form-data + file lampiran opsional. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Surat tambah(@RequestParam String tipe,
                        @RequestParam String nomorSurat,
                        @RequestParam String klasifikasi,
                        @RequestParam String tanggalSurat,
                        @RequestParam(required = false) String pengirim,
                        @RequestParam(required = false) String tujuan,
                        @RequestParam String perihal,
                        @RequestParam(required = false) String sifatSurat,
                        @RequestParam(required = false) String statusSurat,
                        @RequestParam(required = false) String catatan,
                        @RequestParam(value = "lampiran", required = false) MultipartFile lampiran) {
        return service.tambah(tipe, nomorSurat, klasifikasi, parseTanggal(tanggalSurat),
                pengirim, tujuan, perihal, sifatSurat, statusSurat, catatan, lampiran);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Surat ubah(@PathVariable Long id,
                      @RequestParam String nomorSurat,
                      @RequestParam String klasifikasi,
                      @RequestParam String tanggalSurat,
                      @RequestParam(required = false) String pengirim,
                      @RequestParam(required = false) String tujuan,
                      @RequestParam String perihal,
                      @RequestParam(required = false) String sifatSurat,
                      @RequestParam(required = false) String statusSurat,
                      @RequestParam(required = false) String catatan,
                      @RequestParam(value = "lampiran", required = false) MultipartFile lampiran) {
        return service.ubah(id, nomorSurat, klasifikasi, parseTanggal(tanggalSurat),
                pengirim, tujuan, perihal, sifatSurat, statusSurat, catatan, lampiran);
    }

    @DeleteMapping("/{id}")
    public void hapus(@PathVariable Long id) {
        service.hapus(id);
    }

    /* ---------- disposisi surat masuk ---------- */

    /** GET /api/surat/{id}/disposisi — daftar disposisi suatu surat. */
    @GetMapping("/{id}/disposisi")
    public List<Disposisi> daftarDisposisi(@PathVariable Long id) {
        return service.disposisi(id);
    }

    /** POST /api/surat/{id}/disposisi — buat disposisi (khusus ADMIN). */
    @PostMapping("/{id}/disposisi")
    public Disposisi tambahDisposisi(@PathVariable Long id,
                                     @RequestParam(required = false) String tanggalDisposisi,
                                     @RequestParam String diteruskanKepada,
                                     @RequestParam(required = false) String instruksi,
                                     @RequestParam(required = false) String batasTanggal,
                                     @RequestParam(required = false) String status) {
        return service.tambahDisposisi(id,
                (tanggalDisposisi == null || tanggalDisposisi.isBlank()) ? null : LocalDate.parse(tanggalDisposisi),
                diteruskanKepada, instruksi,
                (batasTanggal == null || batasTanggal.isBlank()) ? null : LocalDate.parse(batasTanggal),
                status);
    }

    /** DELETE /api/surat/{id}/disposisi/{disposisiId} — hapus disposisi (khusus ADMIN). */
    @DeleteMapping("/{id}/disposisi/{disposisiId}")
    public void hapusDisposisi(@PathVariable Long id, @PathVariable Long disposisiId) {
        service.hapusDisposisi(id, disposisiId);
    }

    /* ---------- laporan / export ---------- */

    /** GET /api/surat/export?tipe=MASUK — laporan Excel (buku agenda). */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String tipe,
                                         @RequestParam(required = false) String cari) {
        String tipeFix = tipe.trim().toUpperCase();
        List<Surat> daftar = service.semua(tipeFix, cari);
        byte[] data = exportService.buatLaporan(tipeFix, daftar);
        String namaFile = "buku-agenda-surat-" + tipeFix.toLowerCase() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + namaFile + "\"")
                .body(data);
    }

    private LocalDate parseTanggal(String nilai) {
        try {
            return LocalDate.parse(nilai);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format tanggal tidak valid (gunakan format YYYY-MM-DD)");
        }
    }
}
