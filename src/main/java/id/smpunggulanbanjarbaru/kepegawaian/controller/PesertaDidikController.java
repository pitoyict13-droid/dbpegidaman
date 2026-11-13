package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.model.PesertaDidik;
import id.smpunggulanbanjarbaru.kepegawaian.service.PesertaDidikService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/peserta-didik")
public class PesertaDidikController {

    private final PesertaDidikService service;

    public PesertaDidikController(PesertaDidikService service) {
        this.service = service;
    }

    /** GET /api/peserta-didik?cari=nama — daftar peserta didik (wajib login). */
    @GetMapping
    public List<PesertaDidik> daftar(@RequestParam(required = false) String cari) {
        return service.semua(cari);
    }

    @GetMapping("/{id}")
    public PesertaDidik detail(@PathVariable Long id) {
        return service.byId(id);
    }

    @PostMapping
    public PesertaDidik tambah(@Valid @RequestBody PesertaDidik data) {
        return service.tambah(data);
    }

    @PutMapping("/{id}")
    public PesertaDidik ubah(@PathVariable Long id, @Valid @RequestBody PesertaDidik data) {
        return service.ubah(id, data);
    }

    @DeleteMapping("/{id}")
    public void hapus(@PathVariable Long id) {
        service.hapus(id);
    }
}
