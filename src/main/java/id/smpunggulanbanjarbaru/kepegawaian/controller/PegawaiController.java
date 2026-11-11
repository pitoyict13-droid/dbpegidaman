package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.dto.PegawaiRequest;
import id.smpunggulanbanjarbaru.kepegawaian.model.Pegawai;
import id.smpunggulanbanjarbaru.kepegawaian.service.PegawaiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pegawai")
public class PegawaiController {

    private final PegawaiService service;

    public PegawaiController(PegawaiService service) {
        this.service = service;
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
