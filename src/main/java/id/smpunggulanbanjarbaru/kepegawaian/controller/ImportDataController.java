package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.service.ExcelSupport;
import id.smpunggulanbanjarbaru.kepegawaian.service.ImportJob;
import id.smpunggulanbanjarbaru.kepegawaian.service.ImportJobService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unduh template dan import massal data (siswa, sarana prasarana, persuratan)
 * dari file Excel. Import dijalankan sebagai job latar belakang; frontend
 * memantau progres lewat endpoint status.
 */
@RestController
@RequestMapping("/api/import")
public class ImportDataController {

    private final ImportJobService service;

    public ImportDataController(ImportJobService service) {
        this.service = service;
    }

    /** GET /api/import/template/{jenis} — unduh template Excel. */
    @GetMapping("/template/{jenis}")
    public ResponseEntity<byte[]> template(@PathVariable String jenis) {
        byte[] data = service.template(jenis);
        String namaFile = "template-" + jenis.toLowerCase() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExcelSupport.MIME_XLSX))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + namaFile + "\"")
                .body(data);
    }

    /** POST /api/import/{jenis} — unggah file Excel, langsung mengembalikan jobId. */
    @PostMapping("/{jenis}")
    public Map<String, Object> mulai(@PathVariable String jenis,
                                     @RequestParam("file") MultipartFile file) {
        ImportJob job = service.mulai(jenis, file);
        Map<String, Object> hasil = new LinkedHashMap<>();
        hasil.put("jobId", job.getId());
        hasil.put("jenis", job.getJenis());
        hasil.put("status", job.getStatus().name());
        return hasil;
    }

    /** GET /api/import/status/{jobId} — progres dan hasil import. */
    @GetMapping("/status/{jobId}")
    public Map<String, Object> status(@PathVariable String jobId) {
        return service.status(jobId).keMap();
    }
}
