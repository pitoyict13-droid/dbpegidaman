package id.smpunggulanbanjarbaru.kepegawaian.repository;

import id.smpunggulanbanjarbaru.kepegawaian.model.Kesiswaan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KesiswaanRepository extends JpaRepository<Kesiswaan, Long> {
    List<Kesiswaan> findByNamaSiswaContainingIgnoreCaseOrderByTanggalDescIdDesc(String nama);
    List<Kesiswaan> findAllByOrderByTanggalDescIdDesc();
}
