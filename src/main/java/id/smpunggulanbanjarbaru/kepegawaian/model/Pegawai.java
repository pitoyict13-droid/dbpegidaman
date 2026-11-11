package id.smpunggulanbanjarbaru.kepegawaian.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pegawai", uniqueConstraints = @UniqueConstraint(columnNames = "nip"))
public class Pegawai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nomor Induk Pegawai (boleh kosong untuk pegawai non-ASN, mis. GTT/PTT) */
    @Column(unique = true, length = 30)
    private String nip;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nama;

    /** L = Laki-laki, P = Perempuan */
    @Column(length = 1)
    private String jenisKelamin;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String jabatan;

    /** Mata pelajaran yang diampu (khusus guru) */
    @Column(length = 100)
    private String mataPelajaran;

    /** PNS / PPPK / GTT / PTT / Honorer */
    @NotBlank
    @Column(nullable = false, length = 20)
    private String statusKepegawaian;

    /** SMA/SMK, D3, S1, S2, S3 */
    @Column(length = 20)
    private String pendidikanTerakhir;

    private LocalDate tanggalLahir;

    private LocalDate tanggalMasuk;

    @Column(length = 20)
    private String telepon;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String alamat;

    /** Gaji pokok bulanan (Rp) */
    @Column(precision = 12, scale = 2)
    private BigDecimal gajiPokok;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNip() { return nip; }
    public void setNip(String nip) { this.nip = nip; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getJenisKelamin() { return jenisKelamin; }
    public void setJenisKelamin(String jenisKelamin) { this.jenisKelamin = jenisKelamin; }

    public String getJabatan() { return jabatan; }
    public void setJabatan(String jabatan) { this.jabatan = jabatan; }

    public String getMataPelajaran() { return mataPelajaran; }
    public void setMataPelajaran(String mataPelajaran) { this.mataPelajaran = mataPelajaran; }

    public String getStatusKepegawaian() { return statusKepegawaian; }
    public void setStatusKepegawaian(String statusKepegawaian) { this.statusKepegawaian = statusKepegawaian; }

    public String getPendidikanTerakhir() { return pendidikanTerakhir; }
    public void setPendidikanTerakhir(String pendidikanTerakhir) { this.pendidikanTerakhir = pendidikanTerakhir; }

    public LocalDate getTanggalLahir() { return tanggalLahir; }
    public void setTanggalLahir(LocalDate tanggalLahir) { this.tanggalLahir = tanggalLahir; }

    public LocalDate getTanggalMasuk() { return tanggalMasuk; }
    public void setTanggalMasuk(LocalDate tanggalMasuk) { this.tanggalMasuk = tanggalMasuk; }

    public String getTelepon() { return telepon; }
    public void setTelepon(String telepon) { this.telepon = telepon; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }

    public BigDecimal getGajiPokok() { return gajiPokok; }
    public void setGajiPokok(BigDecimal gajiPokok) { this.gajiPokok = gajiPokok; }
}
