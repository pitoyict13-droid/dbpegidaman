package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.model.Disposisi;
import id.smpunggulanbanjarbaru.kepegawaian.model.Surat;
import id.smpunggulanbanjarbaru.kepegawaian.repository.DisposisiRepository;
import id.smpunggulanbanjarbaru.kepegawaian.repository.SuratRepository;
import id.smpunggulanbanjarbaru.kepegawaian.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Notifikasi web "Disposisi Saya":
 * daftar surat masuk yang didisposisikan kepada pengguna yang sedang login.
 * Pencocokan berdasarkan Nama Lengkap akun = nama tujuan disposisi.
 */
@RestController
@RequestMapping("/api/disposisi-saya")
public class DisposisiSayaController {

    private final DisposisiRepository disposisiRepository;
    private final SuratRepository suratRepository;
    private final UserRepository userRepository;

    public DisposisiSayaController(DisposisiRepository disposisiRepository,
                                   SuratRepository suratRepository,
                                   UserRepository userRepository) {
        this.disposisiRepository = disposisiRepository;
        this.suratRepository = suratRepository;
        this.userRepository = userRepository;
    }

    /** GET /api/disposisi-saya — daftar disposisi untuk saya (beserta info suratnya). */
    @GetMapping
    public List<Map<String, Object>> saya(@AuthenticationPrincipal UserDetails principal) {
        String namaSaya = namaPengguna(principal);
        List<Disposisi> daftar = disposisiRepository
                .findByDiteruskanKepadaIgnoreCaseOrderByTanggalDisposisiDescIdDesc(namaSaya);
        return daftar.stream().map(this::tampilan).toList();
    }

    /** POST /api/disposisi-saya/{id}/selesai — tandai disposisi sudah dikerjakan (hanya penerima). */
    @PostMapping("/{id}/selesai")
    @Transactional
    public Map<String, Object> tandaiSelesai(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails principal) {
        Disposisi d = disposisiRepository.findByIdAndDiteruskanKepadaIgnoreCase(id, namaPengguna(principal))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Disposisi tidak ditemukan / bukan milik Anda"));
        d.setStatus("Selesai");
        return tampilan(disposisiRepository.save(d));
    }

    private Map<String, Object> tampilan(Disposisi d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("suratId", d.getSuratId());
        m.put("tanggalDisposisi", d.getTanggalDisposisi());
        m.put("diteruskanKepada", d.getDiteruskanKepada());
        m.put("instruksi", d.getInstruksi());
        m.put("batasTanggal", d.getBatasTanggal());
        m.put("status", d.getStatus());
        Surat s = suratRepository.findById(d.getSuratId()).orElse(null);
        if (s != null) {
            m.put("nomorAgenda", s.getNomorAgenda());
            m.put("nomorSurat", s.getNomorSurat());
            m.put("perihal", s.getPerihal());
            m.put("pengirim", s.getPengirim());
            m.put("tanggalSurat", s.getTanggalSurat());
            m.put("klasifikasi", s.getKlasifikasi());
        }
        return m;
    }

    private String namaPengguna(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Pengguna tidak ditemukan"))
                .getNamaLengkap();
    }
}
