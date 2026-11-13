package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.model.PesertaDidik;
import id.smpunggulanbanjarbaru.kepegawaian.repository.PesertaDidikRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD peserta didik (gaya Dapodik).
 */
@Service
public class PesertaDidikService {

    private final PesertaDidikRepository repository;

    public PesertaDidikService(PesertaDidikRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PesertaDidik> semua(String cari) {
        if (cari != null && !cari.isBlank()) {
            return repository.findByNamaContainingIgnoreCaseOrderByIdAsc(cari.trim());
        }
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public PesertaDidik byId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data peserta didik tidak ditemukan"));
    }

    @Transactional
    public PesertaDidik tambah(PesertaDidik data) {
        data.setId(null);
        cekDuplikat(data, null);
        normalisasi(data);
        return repository.save(data);
    }

    @Transactional
    public PesertaDidik ubah(Long id, PesertaDidik data) {
        byId(id); // pastikan ada
        data.setId(id);
        cekDuplikat(data, id);
        normalisasi(data);
        return repository.save(data);
    }

    @Transactional
    public void hapus(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Data peserta didik tidak ditemukan");
        }
        repository.deleteById(id);
    }

    private void cekDuplikat(PesertaDidik data, Long id) {
        if (data.getNisn() != null && !data.getNisn().isBlank()) {
            repository.findByNisn(data.getNisn().trim())
                    .filter(lain -> id == null || !lain.getId().equals(id))
                    .ifPresent(lain -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "NISN sudah terdaftar");
                    });
        }
        if (data.getNis() != null && !data.getNis().isBlank()) {
            repository.findByNis(data.getNis().trim())
                    .filter(lain -> id == null || !lain.getId().equals(id))
                    .ifPresent(lain -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "NIS sudah terdaftar");
                    });
        }
    }

    private void normalisasi(PesertaDidik data) {
        data.setNama(data.getNama().trim());
        data.setNisn(bersih(data.getNisn()));
        data.setNis(bersih(data.getNis()));
        if (data.getStatus() == null || data.getStatus().isBlank()) {
            data.setStatus("Aktif");
        }
    }

    private String bersih(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
