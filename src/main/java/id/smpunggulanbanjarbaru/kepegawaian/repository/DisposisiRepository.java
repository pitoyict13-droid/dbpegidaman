package id.smpunggulanbanjarbaru.kepegawaian.repository;

import id.smpunggulanbanjarbaru.kepegawaian.model.Disposisi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface DisposisiRepository extends JpaRepository<Disposisi, Long> {

    List<Disposisi> findBySuratIdOrderByTanggalDisposisiAscIdAsc(Long suratId);

    @Modifying
    void deleteBySuratId(Long suratId);
}
