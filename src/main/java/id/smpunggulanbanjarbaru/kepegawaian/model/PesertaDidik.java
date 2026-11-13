package id.smpunggulanbanjarbaru.kepegawaian.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * Peserta didik (gaya Dapodik) — biodata siswa, orang tua/wali, dan kelas.
 */
@Entity
@Table(name = "peserta_didik", uniqueConstraints = {
        @UniqueConstraint(columnNames = "nisn"),
        @UniqueConstraint(columnNames = "nis")
})
public class PesertaDidik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nomor Induk Siswa Nasional (boleh kosong sementara) */
    @Column(unique = true, length = 20)
    private String nisn;

    /** Nomor Induk Siswa (NIS lokal sekolah) */
    @Column(unique = true, length = 20)
    private String nis;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String nama;

    /** L = Laki-laki, P = Perempuan */
    @Column(length = 1)
    private String jenisKelamin;

    @Column(length = 100)
    private String tempatLahir;

    private LocalDate tanggalLahir;

    /** Islam / Kristen / Katolik / Hindu / Buddha / Konghucu */
    @Column(length = 20)
    private String agama;

    @Column(length = 255)
    private String alamat;

    @Column(length = 100)
    private String kelurahan;

    @Column(length = 100)
    private String kecamatan;

    @Column(length = 100)
    private String kotaKabupaten;

    @Column(length = 25)
    private String nomorHp;

    @Column(length = 150)
    private String namaAyah;

    @Column(length = 150)
    private String namaIbu;

    @Column(length = 50)
    private String pekerjaanAyah;

    @Column(length = 50)
    private String pekerjaanIbu;

    /** mis. "< 500 Ribu", "500 Ribu - 1 Juta", dsb. */
    @Column(length = 50)
    private String penghasilanOrangTua;

    @Column(length = 25)
    private String nomorHpOrangTua;

    /** Kelas, mis. "7A", "8B", "9C" */
    @Column(length = 10)
    private String kelas;

    /** Tahun ajaran mulai, mis. 2024 (angkatan) */
    private Integer angkatan;

    /** Aktif / Lulus / Pindah / Keluar / Meninggal */
    @Column(nullable = false, length = 20)
    private String status = "Aktif";

    private LocalDate tanggalMasuk;

    @Column(length = 255)
    private String catatan;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNisn() { return nisn; }
    public void setNisn(String nisn) { this.nisn = nisn; }

    public String getNis() { return nis; }
    public void setNis(String nis) { this.nis = nis; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getJenisKelamin() { return jenisKelamin; }
    public void setJenisKelamin(String jenisKelamin) { this.jenisKelamin = jenisKelamin; }

    public String getTempatLahir() { return tempatLahir; }
    public void setTempatLahir(String tempatLahir) { this.tempatLahir = tempatLahir; }

    public LocalDate getTanggalLahir() { return tanggalLahir; }
    public void setTanggalLahir(LocalDate tanggalLahir) { this.tanggalLahir = tanggalLahir; }

    public String getAgama() { return agama; }
    public void setAgama(String agama) { this.agama = agama; }

    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }

    public String getKelurahan() { return kelurahan; }
    public void setKelurahan(String kelurahan) { this.kelurahan = kelurahan; }

    public String getKecamatan() { return kecamatan; }
    public void setKecamatan(String kecamatan) { this.kecamatan = kecamatan; }

    public String getKotaKabupaten() { return kotaKabupaten; }
    public void setKotaKabupaten(String kotaKabupaten) { this.kotaKabupaten = kotaKabupaten; }

    public String getNomorHp() { return nomorHp; }
    public void setNomorHp(String nomorHp) { this.nomorHp = nomorHp; }

    public String getNamaAyah() { return namaAyah; }
    public void setNamaAyah(String namaAyah) { this.namaAyah = namaAyah; }

    public String getNamaIbu() { return namaIbu; }
    public void setNamaIbu(String namaIbu) { this.namaIbu = namaIbu; }

    public String getPekerjaanAyah() { return pekerjaanAyah; }
    public void setPekerjaanAyah(String pekerjaanAyah) { this.pekerjaanAyah = pekerjaanAyah; }

    public String getPekerjaanIbu() { return pekerjaanIbu; }
    public void setPekerjaanIbu(String pekerjaanIbu) { this.pekerjaanIbu = pekerjaanIbu; }

    public String getPenghasilanOrangTua() { return penghasilanOrangTua; }
    public void setPenghasilanOrangTua(String penghasilanOrangTua) { this.penghasilanOrangTua = penghasilanOrangTua; }

    public String getNomorHpOrangTua() { return nomorHpOrangTua; }
    public void setNomorHpOrangTua(String nomorHpOrangTua) { this.nomorHpOrangTua = nomorHpOrangTua; }

    public String getKelas() { return kelas; }
    public void setKelas(String kelas) { this.kelas = kelas; }

    public Integer getAngkatan() { return angkatan; }
    public void setAngkatan(Integer angkatan) { this.angkatan = angkatan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getTanggalMasuk() { return tanggalMasuk; }
    public void setTanggalMasuk(LocalDate tanggalMasuk) { this.tanggalMasuk = tanggalMasuk; }

    public String getCatatan() { return catatan; }
    public void setCatatan(String catatan) { this.catatan = catatan; }
}
