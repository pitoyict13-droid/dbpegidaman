package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.model.ProfilSekolah;
import id.smpunggulanbanjarbaru.kepegawaian.service.ProfilSekolahService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profil-sekolah")
public class ProfilSekolahController {

    private final ProfilSekolahService service;

    public ProfilSekolahController(ProfilSekolahService service) {
        this.service = service;
    }

    /** GET /api/profil-sekolah — profil sekolah (wajib login). */
    @GetMapping
    public ProfilSekolah get() {
        return service.get();
    }

    /** PUT /api/profil-sekolah — perbarui profil sekolah (khusus ADMIN). */
    @PutMapping
    public ProfilSekolah simpan(@Valid @RequestBody ProfilSekolah data) {
        return service.simpan(data);
    }
}
