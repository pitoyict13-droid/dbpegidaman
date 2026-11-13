package id.smpunggulanbanjarbaru.kepegawaian.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * Catatan kesiswaan (gaya Dapodik): prestasi, pelanggaran,
 * kegiatan ekstrakurikuler, dan beasiswa peserta didik.
 */
@Entity
@Table(name = "kesiswaan")
public class Kesiswaan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Prestasi / Pelanggaran / Ekstrakurikuler / Beasiswa / Lainnya */
    @NotBlank
    @Column(nullable = false, length = 30)
    private String jenis;

    /** Nama peserta didik terkait */
    @NotBlank
    @Column(nullable = false, length = 150)
    private String namaSiswa;

    /** Kelas, mis. "7A" */
    @Column(length = 10)
    private String kelas;

    /** Judul/ringkasan, mis. "Juara 1 Olimpiade Matematika Tingkat Provinsi" */
    @NotBlank
    @Column(nullable = false, length = 255)
    private String judul;

    /** Sekolah / Kecamatan / Kabupaten/Kota / Provinsi / Nasional / Internasional */
    @Column(length = 30)
    private String tingkat;

    private LocalDate tanggal;

    /** Poin pelanggaran (khusus jenis Pelanggaran) */
    private Integer poin;

    @Column(length = 500)
    private String keterangan;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJenis() { return jenis; }
    public void setJenis(String jenis) { this.jenis = jenis; }

    public String getNamaSiswa() { return namaSiswa; }
    public void setNamaSiswa(String namaSiswa) { this.namaSiswa = namaSiswa; }

    public String getKelas() { return kelas; }
    public void setKelas(String kelas) { this.kelas = kelas; }

    public String getJudul() { return judul; }
    public void setJudul(String judul) { this.judul = judul; }

    public String getTingkat() { return tingkat; }
    public void setTingkat(String tingkat) { this.tingkat = tingkat; }

    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }

    public Integer getPoin() { return poin; }
    public void setPoin(Integer poin) { this.poin = poin; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
}
