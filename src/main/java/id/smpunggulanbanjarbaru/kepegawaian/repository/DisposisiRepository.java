package id.smpunggulanbanjarbaru.kepegawaian.repository;

import id.smpunggulanbanjarbaru.kepegawaian.model.Disposisi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface DisposisiRepository extends JpaRepository<Disposisi, Long> {

    List<Disposisi> findBySuratIdOrderByTanggalDisposisiAscIdAsc(Long suratId);

    /** Disposisi yang diteruskan kepada seseorang (untuk notifikasi "Disposisi Saya") */
    List<Disposisi> findByDiteruskanKepadaIgnoreCaseOrderByTanggalDisposisiDescIdDesc(String nama);

    Optional<Disposisi> findByIdAndDiteruskanKepadaIgnoreCase(Long id, String nama);

    @Modifying
    void deleteBySuratId(Long suratId);
}
