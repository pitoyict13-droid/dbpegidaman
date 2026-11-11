package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.dto.PegawaiRequest;
import id.smpunggulanbanjarbaru.kepegawaian.model.Pegawai;
import id.smpunggulanbanjarbaru.kepegawaian.service.ExcelService;
import id.smpunggulanbanjarbaru.kepegawaian.service.PegawaiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pegawai")
public class PegawaiController {

    private final PegawaiService service;
    private final ExcelService excelService;

    public PegawaiController(PegawaiService service, ExcelService excelService) {
        this.service = service;
        this.excelService = excelService;
    }

    /** GET /api/pegawai?cari=nama — daftar pegawai (wajib login). */
    @GetMapping
    public List<Pegawai> daftar(@RequestParam(required = false) String cari) {
        return service.semua(cari);
    }

    @GetMapping("/{id}")
    public Pegawai detail(@PathVariable Long id) {
        return service.byId(id);
    }

    /** GET /api/pegawai/template — unduh template Excel untuk import data. */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        byte[] data = excelService.buatTemplate();
        String namaFile = "template-pegawai.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + namaFile + "\"")
                .body(data);
    }

    /** POST /api/pegawai/import — unggah file Excel berisi data pegawai (khusus ADMIN). */
    @PostMapping("/import")
    public Map<String, Object> importExcel(@RequestParam("file") MultipartFile file) {
        return excelService.importExcel(file);
    }

    @PostMapping
    public Pegawai tambah(@Valid @RequestBody PegawaiRequest request) {
        return service.tambah(request);
    }

    @PutMapping("/{id}")
    public Pegawai ubah(@PathVariable Long id, @Valid @RequestBody PegawaiRequest request) {
        return service.ubah(id, request);
    }

    @DeleteMapping("/{id}")
    public void hapus(@PathVariable Long id) {
        service.hapus(id);
    }
}
