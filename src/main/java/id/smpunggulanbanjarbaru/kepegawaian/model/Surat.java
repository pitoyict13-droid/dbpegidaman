package id.smpunggulanbanjarbaru.kepegawaian.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Arsip surat masuk dan surat keluar sekolah,
 * dengan penomoran agenda bergaya Srikandi (Kalsel):
 * nomor agenda dibuat otomatis -> 001/KODE/TAHUN.
 */
@Entity
@Table(name = "surat")
public class Surat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** MASUK atau KELUAR */
    @NotNull
    @Column(nullable = false, length = 10)
    private String tipe;

    /** Nomor agenda otomatis: 001/UM/2026 */
    @Column(unique = true, length = 50)
    private String nomorAgenda;

    /** Nomor surat resmi di kop surat, mis. 421.2/123/SMUI/2026 */
    @NotBlank
    @Column(nullable = false, length = 100)
    private String nomorSurat;

    /** Kode klasifikasi arsip, mis. UM, PT, SN, KP, UN, PG, PU */
    @NotBlank
    @Column(nullable = false, length = 5)
    private String klasifikasi;

    @NotNull
    @Column(nullable = false)
    private LocalDate tanggalSurat;

    /** Tanggal surat dicatat dalam sistem */
    @Column(nullable = false)
    private LocalDate tanggalCatat;

    /** Untuk surat MASUK: instansi/orang pengirim */
    @Column(length = 150)
    private String pengirim;

    /** Untuk surat KELUAR: instansi/orang tujuan */
    @Column(length = 150)
    private String tujuan;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String perihal;

    /** Biasa / Penting / Segera / Rahasia */
    @Column(length = 20)
    private String sifatSurat;

    /** Surat masuk: Belum Diproses / Diproses / Selesai. Surat keluar: Terkirim / Arsip */
    @Column(length = 30)
    private String statusSurat;

    @Column(length = 255)
    private String catatan;

    /* ---------- lampiran (file scan/softcopy surat, opsional) ---------- */
    @Column(length = 255)
    private String lampiranNama;   // nama asli file saat diunggah

    @Column(length = 255)
    private String lampiranPath;   // nama file di server (acak, unik)

    @Column(length = 100)
    private String lampiranTipe;   // tipe MIME

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipe() { return tipe; }
    public void setTipe(String tipe) { this.tipe = tipe; }

    public String getNomorAgenda() { return nomorAgenda; }
    public void setNomorAgenda(String nomorAgenda) { this.nomorAgenda = nomorAgenda; }

    public String getNomorSurat() { return nomorSurat; }
    public void setNomorSurat(String nomorSurat) { this.nomorSurat = nomorSurat; }

    public String getKlasifikasi() { return klasifikasi; }
    public void setKlasifikasi(String klasifikasi) { this.klasifikasi = klasifikasi; }

    public LocalDate getTanggalSurat() { return tanggalSurat; }
    public void setTanggalSurat(LocalDate tanggalSurat) { this.tanggalSurat = tanggalSurat; }

    public LocalDate getTanggalCatat() { return tanggalCatat; }
    public void setTanggalCatat(LocalDate tanggalCatat) { this.tanggalCatat = tanggalCatat; }

    public String getPengirim() { return pengirim; }
    public void setPengirim(String pengirim) { this.pengirim = pengirim; }

    public String getTujuan() { return tujuan; }
    public void setTujuan(String tujuan) { this.tujuan = tujuan; }

    public String getPerihal() { return perihal; }
    public void setPerihal(String perihal) { this.perihal = perihal; }

    public String getSifatSurat() { return sifatSurat; }
    public void setSifatSurat(String sifatSurat) { this.sifatSurat = sifatSurat; }

    public String getStatusSurat() { return statusSurat; }
    public void setStatusSurat(String statusSurat) { this.statusSurat = statusSurat; }

    public String getCatatan() { return catatan; }
    public void setCatatan(String catatan) { this.catatan = catatan; }

    public String getLampiranNama() { return lampiranNama; }
    public void setLampiranNama(String lampiranNama) { this.lampiranNama = lampiranNama; }

    public String getLampiranPath() { return lampiranPath; }
    public void setLampiranPath(String lampiranPath) { this.lampiranPath = lampiranPath; }

    public String getLampiranTipe() { return lampiranTipe; }
    public void setLampiranTipe(String lampiranTipe) { this.lampiranTipe = lampiranTipe; }
}
