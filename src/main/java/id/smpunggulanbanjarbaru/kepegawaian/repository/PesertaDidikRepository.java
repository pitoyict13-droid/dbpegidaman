package id.smpunggulanbanjarbaru.kepegawaian.repository;

import id.smpunggulanbanjarbaru.kepegawaian.model.PesertaDidik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PesertaDidikRepository extends JpaRepository<PesertaDidik, Long> {
    Optional<PesertaDidik> findByNisn(String nisn);
    Optional<PesertaDidik> findByNis(String nis);
    List<PesertaDidik> findByNamaContainingIgnoreCaseOrderByIdAsc(String nama);
    List<PesertaDidik> findAllByOrderByIdAsc();
    boolean existsByNisn(String nisn);
    boolean existsByNis(String nis);
}
