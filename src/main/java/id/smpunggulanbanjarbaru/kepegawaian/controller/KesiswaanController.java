package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.model.Kesiswaan;
import id.smpunggulanbanjarbaru.kepegawaian.service.KesiswaanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kesiswaan")
public class KesiswaanController {

    private final KesiswaanService service;

    public KesiswaanController(KesiswaanService service) {
        this.service = service;
    }

    /** GET /api/kesiswaan?cari=nama — daftar catatan kesiswaan (wajib login). */
    @GetMapping
    public List<Kesiswaan> daftar(@RequestParam(required = false) String cari) {
        return service.semua(cari);
    }

    @GetMapping("/{id}")
    public Kesiswaan detail(@PathVariable Long id) {
        return service.byId(id);
    }

    @PostMapping
    public Kesiswaan tambah(@Valid @RequestBody Kesiswaan data) {
        return service.tambah(data);
    }

    @PutMapping("/{id}")
    public Kesiswaan ubah(@PathVariable Long id, @Valid @RequestBody Kesiswaan data) {
        return service.ubah(id, data);
    }

    @DeleteMapping("/{id}")
    public void hapus(@PathVariable Long id) {
        service.hapus(id);
    }
}
