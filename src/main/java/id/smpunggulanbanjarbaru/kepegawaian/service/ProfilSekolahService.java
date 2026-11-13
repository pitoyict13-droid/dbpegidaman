package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.model.ProfilSekolah;
import id.smpunggulanbanjarbaru.kepegawaian.repository.ProfilSekolahRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profil sekolah bersifat tunggal (id = 1) — identitas, sarana umum, visi & misi.
 */
@Service
public class ProfilSekolahService {

    public static final Long ID_TUNGGAL = 1L;

    private final ProfilSekolahRepository repository;

    public ProfilSekolahService(ProfilSekolahRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ProfilSekolah get() {
        return repository.findById(ID_TUNGGAL).orElseGet(this::profilKosong);
    }

    @Transactional
    public ProfilSekolah simpan(ProfilSekolah data) {
        data.setId(ID_TUNGGAL); // selalu satu baris
        return repository.save(data);
    }

    private ProfilSekolah profilKosong() {
        ProfilSekolah p = new ProfilSekolah();
        p.setId(ID_TUNGGAL);
        p.setNamaSekolah("SMP Unggulan Idaman Banjarbaru");
        p.setStatusSekolah("Negeri");
        p.setBentukPendidikan("SMP");
        return p;
    }
}
