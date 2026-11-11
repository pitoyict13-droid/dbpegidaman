package id.smpunggulanbanjarbaru.kepegawaian.repository;

import id.smpunggulanbanjarbaru.kepegawaian.model.Pegawai;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PegawaiRepository extends JpaRepository<Pegawai, Long> {
    Optional<Pegawai> findByNip(String nip);
    List<Pegawai> findByNamaContainingIgnoreCaseOrderByIdAsc(String nama);
    List<Pegawai> findAllByOrderByIdAsc();
    boolean existsByNip(String nip);
}
