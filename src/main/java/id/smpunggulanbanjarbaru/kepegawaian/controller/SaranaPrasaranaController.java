package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.model.SaranaPrasarana;
import id.smpunggulanbanjarbaru.kepegawaian.service.SaranaPrasaranaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sarana-prasarana")
public class SaranaPrasaranaController {

    private final SaranaPrasaranaService service;

    public SaranaPrasaranaController(SaranaPrasaranaService service) {
        this.service = service;
    }

    /** GET /api/sarana-prasarana?cari=...&kategori=... — daftar sarpras (wajib login). */
    @GetMapping
    public List<SaranaPrasarana> daftar(@RequestParam(required = false) String cari,
                                        @RequestParam(required = false) String kategori) {
        return service.semua(cari, kategori);
    }

    @GetMapping("/{id}")
    public SaranaPrasarana detail(@PathVariable Long id) {
        return service.byId(id);
    }

    @PostMapping
    public SaranaPrasarana tambah(@Valid @RequestBody SaranaPrasarana data) {
        return service.tambah(data);
    }

    @PutMapping("/{id}")
    public SaranaPrasarana ubah(@PathVariable Long id, @Valid @RequestBody SaranaPrasarana data) {
        return service.ubah(id, data);
    }

    @DeleteMapping("/{id}")
    public void hapus(@PathVariable Long id) {
        service.hapus(id);
    }
}
