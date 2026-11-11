package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.dto.PegawaiRequest;
import id.smpunggulanbanjarbaru.kepegawaian.model.Pegawai;
import id.smpunggulanbanjarbaru.kepegawaian.repository.PegawaiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD data pegawai (guru dan tenaga kependidikan).
 */
@Service
public class PegawaiService {

    private final PegawaiRepository repository;

    public PegawaiService(PegawaiRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Pegawai> semua(String cari) {
        if (cari != null && !cari.isBlank()) {
            return repository.findByNamaContainingIgnoreCaseOrderByIdAsc(cari.trim());
        }
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Pegawai byId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data pegawai tidak ditemukan"));
    }

    @Transactional
    public Pegawai tambah(PegawaiRequest req) {
        if (req.getNip() != null && !req.getNip().isBlank() && repository.existsByNip(req.getNip())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "NIP sudah terdaftar");
        }
        Pegawai p = new Pegawai();
        salin(req, p);
        return repository.save(p);
    }

    @Transactional
    public Pegawai ubah(Long id, PegawaiRequest req) {
        Pegawai p = byId(id);
        if (req.getNip() != null && !req.getNip().isBlank()) {
            repository.findByNip(req.getNip())
                    .filter(lain -> !lain.getId().equals(id))
                    .ifPresent(lain -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "NIP sudah terdaftar");
                    });
        }
        salin(req, p);
        return repository.save(p);
    }

    @Transactional
    public void hapus(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Data pegawai tidak ditemukan");
        }
        repository.deleteById(id);
    }

    private void salin(PegawaiRequest req, Pegawai p) {
        p.setNip((req.getNip() == null || req.getNip().isBlank()) ? null : req.getNip());
        p.setNama(req.getNama().trim());
        p.setJenisKelamin(req.getJenisKelamin());
        p.setJabatan(req.getJabatan());
        p.setMataPelajaran(req.getMataPelajaran());
        p.setStatusKepegawaian(req.getStatusKepegawaian());
        p.setPendidikanTerakhir(req.getPendidikanTerakhir());
        p.setTanggalLahir(req.getTanggalLahir());
        p.setTanggalMasuk(req.getTanggalMasuk());
        p.setTelepon(req.getTelepon());
        p.setEmail(req.getEmail());
        p.setAlamat(req.getAlamat());
        p.setGajiPokok(req.getGajiPokok());
    }
}
