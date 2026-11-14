package id.smpunggulanbanjarbaru.kepegawaian.repository;

import id.smpunggulanbanjarbaru.kepegawaian.model.Surat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SuratRepository extends JpaRepository<Surat, Long> {

    List<Surat> findByTipeOrderByIdAsc(String tipe);

    /** Nomor agenda sudah terpakai? (mencegah duplikat saat pembuatan otomatis) */
    boolean existsByNomorAgenda(String nomorAgenda);

    /** Semua nomor surat — dipakai untuk cek duplikat massal saat import. */
    @Query("select s.nomorSurat from Surat s where s.nomorSurat is not null")
    List<String> daftarNomorSurat();
}
