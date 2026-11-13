package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.model.Kesiswaan;
import id.smpunggulanbanjarbaru.kepegawaian.repository.KesiswaanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD catatan kesiswaan (prestasi, pelanggaran, ekstrakurikuler, beasiswa).
 */
@Service
public class KesiswaanService {

    private final KesiswaanRepository repository;

    public KesiswaanService(KesiswaanRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Kesiswaan> semua(String cari) {
        if (cari != null && !cari.isBlank()) {
            return repository.findByNamaSiswaContainingIgnoreCaseOrderByTanggalDescIdDesc(cari.trim());
        }
        return repository.findAllByOrderByTanggalDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Kesiswaan byId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data kesiswaan tidak ditemukan"));
    }

    @Transactional
    public Kesiswaan tambah(Kesiswaan data) {
        data.setId(null);
        normalisasi(data);
        return repository.save(data);
    }

    @Transactional
    public Kesiswaan ubah(Long id, Kesiswaan data) {
        byId(id);
        data.setId(id);
        normalisasi(data);
        return repository.save(data);
    }

    @Transactional
    public void hapus(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Data kesiswaan tidak ditemukan");
        }
        repository.deleteById(id);
    }

    private void normalisasi(Kesiswaan data) {
        data.setNamaSiswa(data.getNamaSiswa().trim());
        data.setJudul(data.getJudul().trim());
        data.setJenis(data.getJenis().trim());
    }
}
