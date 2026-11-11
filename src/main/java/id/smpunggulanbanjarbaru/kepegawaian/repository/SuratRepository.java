package id.smpunggulanbanjarbaru.kepegawaian.repository;

import id.smpunggulanbanjarbaru.kepegawaian.model.Surat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuratRepository extends JpaRepository<Surat, Long> {

    List<Surat> findByTipeOrderByIdAsc(String tipe);

    /** Nomor agenda sudah terpakai? (mencegah duplikat saat pembuatan otomatis) */
    boolean existsByNomorAgenda(String nomorAgenda);
}
