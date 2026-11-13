package id.smpunggulanbanjarbaru.kepegawaian.repository;

import id.smpunggulanbanjarbaru.kepegawaian.model.SaranaPrasarana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaranaPrasaranaRepository extends JpaRepository<SaranaPrasarana, Long> {
    List<SaranaPrasarana> findByNamaContainingIgnoreCaseOrderByIdAsc(String nama);
    List<SaranaPrasarana> findByKategoriOrderByIdAsc(String kategori);
    List<SaranaPrasarana> findAllByOrderByIdAsc();
}
