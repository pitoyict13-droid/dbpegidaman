package id.smpunggulanbanjarbaru.kepegawaian.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Profil sekolah (gaya Dapodik — entitas tunggal, selalu id = 1).
 * Menyimpan identitas sekolah, data sarana umum, visi & misi.
 */
@Entity
@Table(name = "profil_sekolah")
public class ProfilSekolah {

    @Id
    private Long id = 1L;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String namaSekolah;

    /** Nomor Pokok Sekolah Nasional */
    @Column(length = 20)
    private String npsn;

    /** Nomor Statistik Sekolah */
    @Column(length = 30)
    private String nss;

    /** Negeri / Swasta */
    @Column(length = 20)
    private String statusSekolah;

    /** SMP / MTs / SMA ... */
    @Column(length = 20)
    private String bentukPendidikan;

    /** A / B / C / Belum Terakreditasi */
    @Column(length = 30)
    private String statusAkreditasi;

    private Integer tahunBerdiri;

    @Column(length = 255)
    private String alamat;

    @Column(length = 100)
    private String kelurahan;

    @Column(length = 100)
    private String kecamatan;

    @Column(length = 100)
    private String kotaKabupaten;

    @Column(length = 100)
    private String provinsi;

    @Column(length = 10)
    private String kodePos;

    @Column(length = 30)
    private String telepon;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String website;

    @Column(length = 150)
    private String namaKepalaSekolah;

    @Column(length = 30)
    private String nipKepalaSekolah;

    /** Kurikulum Merdeka / K13 / KTSP */
    @Column(length = 50)
    private String kurikulum;

    /** Pagi / Siang / Full Day */
    @Column(length = 30)
    private String waktuBelajar;

    /** Luas tanah (m²) */
    @Column(precision = 12, scale = 2)
    private BigDecimal luasTanah;

    /** Luas bangunan (m²) */
    @Column(precision = 12, scale = 2)
    private BigDecimal luasBangunan;

    /** Daya listrik (VA) */
    private Integer dayaListrik;

    /** PLN / Genset / Tenaga Surya */
    @Column(length = 30)
    private String sumberListrik;

    /** Ya / Tidak */
    @Column(length = 10)
    private String aksesInternet;

    @Column(length = 50)
    private String kecepatanInternet;

    @Column(length = 1000)
    private String visi;

    @Column(length = 1500)
    private String misi;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNamaSekolah() { return namaSekolah; }
    public void setNamaSekolah(String namaSekolah) { this.namaSekolah = namaSekolah; }

    public String getNpsn() { return npsn; }
    public void setNpsn(String npsn) { this.npsn = npsn; }

    public String getNss() { return nss; }
    public void setNss(String nss) { this.nss = nss; }

    public String getStatusSekolah() { return statusSekolah; }
    public void setStatusSekolah(String statusSekolah) { this.statusSekolah = statusSekolah; }

    public String getBentukPendidikan() { return bentukPendidikan; }
    public void setBentukPendidikan(String bentukPendidikan) { this.bentukPendidikan = bentukPendidikan; }

    public String getStatusAkreditasi() { return statusAkreditasi; }
    public void setStatusAkreditasi(String statusAkreditasi) { this.statusAkreditasi = statusAkreditasi; }

    public Integer getTahunBerdiri() { return tahunBerdiri; }
    public void setTahunBerdiri(Integer tahunBerdiri) { this.tahunBerdiri = tahunBerdiri; }

    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }

    public String getKelurahan() { return kelurahan; }
    public void setKelurahan(String kelurahan) { this.kelurahan = kelurahan; }

    public String getKecamatan() { return kecamatan; }
    public void setKecamatan(String kecamatan) { this.kecamatan = kecamatan; }

    public String getKotaKabupaten() { return kotaKabupaten; }
    public void setKotaKabupaten(String kotaKabupaten) { this.kotaKabupaten = kotaKabupaten; }

    public String getProvinsi() { return provinsi; }
    public void setProvinsi(String provinsi) { this.provinsi = provinsi; }

    public String getKodePos() { return kodePos; }
    public void setKodePos(String kodePos) { this.kodePos = kodePos; }

    public String getTelepon() { return telepon; }
    public void setTelepon(String telepon) { this.telepon = telepon; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getNamaKepalaSekolah() { return namaKepalaSekolah; }
    public void setNamaKepalaSekolah(String namaKepalaSekolah) { this.namaKepalaSekolah = namaKepalaSekolah; }

    public String getNipKepalaSekolah() { return nipKepalaSekolah; }
    public void setNipKepalaSekolah(String nipKepalaSekolah) { this.nipKepalaSekolah = nipKepalaSekolah; }

    public String getKurikulum() { return kurikulum; }
    public void setKurikulum(String kurikulum) { this.kurikulum = kurikulum; }

    public String getWaktuBelajar() { return waktuBelajar; }
    public void setWaktuBelajar(String waktuBelajar) { this.waktuBelajar = waktuBelajar; }

    public BigDecimal getLuasTanah() { return luasTanah; }
    public void setLuasTanah(BigDecimal luasTanah) { this.luasTanah = luasTanah; }

    public BigDecimal getLuasBangunan() { return luasBangunan; }
    public void setLuasBangunan(BigDecimal luasBangunan) { this.luasBangunan = luasBangunan; }

    public Integer getDayaListrik() { return dayaListrik; }
    public void setDayaListrik(Integer dayaListrik) { this.dayaListrik = dayaListrik; }

    public String getSumberListrik() { return sumberListrik; }
    public void setSumberListrik(String sumberListrik) { this.sumberListrik = sumberListrik; }

    public String getAksesInternet() { return aksesInternet; }
    public void setAksesInternet(String aksesInternet) { this.aksesInternet = aksesInternet; }

    public String getKecepatanInternet() { return kecepatanInternet; }
    public void setKecepatanInternet(String kecepatanInternet) { this.kecepatanInternet = kecepatanInternet; }

    public String getVisi() { return visi; }
    public void setVisi(String visi) { this.visi = visi; }

    public String getMisi() { return misi; }
    public void setMisi(String misi) { this.misi = misi; }
}
