package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.model.SaranaPrasarana;
import id.smpunggulanbanjarbaru.kepegawaian.repository.SaranaPrasaranaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD sarana prasarana sekolah (gedung, ruang, aset/peralatan).
 */
@Service
public class SaranaPrasaranaService {

    private final SaranaPrasaranaRepository repository;

    public SaranaPrasaranaService(SaranaPrasaranaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SaranaPrasarana> semua(String cari, String kategori) {
        if (cari != null && !cari.isBlank()) {
            return repository.findByNamaContainingIgnoreCaseOrderByIdAsc(cari.trim());
        }
        if (kategori != null && !kategori.isBlank()) {
            return repository.findByKategoriOrderByIdAsc(kategori.trim());
        }
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public SaranaPrasarana byId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data sarana prasarana tidak ditemukan"));
    }

    @Transactional
    public SaranaPrasarana tambah(SaranaPrasarana data) {
        data.setId(null);
        normalisasi(data);
        return repository.save(data);
    }

    @Transactional
    public SaranaPrasarana ubah(Long id, SaranaPrasarana data) {
        byId(id);
        data.setId(id);
        normalisasi(data);
        return repository.save(data);
    }

    @Transactional
    public void hapus(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Data sarana prasarana tidak ditemukan");
        }
        repository.deleteById(id);
    }

    private void normalisasi(SaranaPrasarana data) {
        data.setNama(data.getNama().trim());
        data.setKategori(data.getKategori().trim());
        if (data.getKondisi() == null || data.getKondisi().isBlank()) {
            data.setKondisi("Baik");
        }
    }
}
