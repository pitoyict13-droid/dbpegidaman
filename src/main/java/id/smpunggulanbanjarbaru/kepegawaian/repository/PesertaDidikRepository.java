package id.smpunggulanbanjarbaru.kepegawaian.repository;

import id.smpunggulanbanjarbaru.kepegawaian.model.PesertaDidik;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PesertaDidikRepository extends JpaRepository<PesertaDidik, Long> {
    Optional<PesertaDidik> findByNisn(String nisn);
    Optional<PesertaDidik> findByNis(String nis);
    List<PesertaDidik> findByNamaContainingIgnoreCaseOrderByIdAsc(String nama);
    List<PesertaDidik> findAllByOrderByIdAsc();
    boolean existsByNisn(String nisn);
    boolean existsByNis(String nis);

    /** Hanya NISN yang terisi — dipakai untuk cek duplikat massal saat import. */
    @Query("select p.nisn from PesertaDidik p where p.nisn is not null")
    List<String> daftarNisn();

    /** Hanya NIS yang terisi — dipakai untuk cek duplikat massal saat import. */
    @Query("select p.nis from PesertaDidik p where p.nis is not null")
    List<String> daftarNis();
}
