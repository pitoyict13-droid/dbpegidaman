package id.smpunggulanbanjarbaru.kepegawaian.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Lembar disposisi Kepala Sekolah atas surat masuk:
 * surat diteruskan kepada pegawai tertentu beserta instruksi.
 */
@Entity
@Table(name = "disposisi")
public class Disposisi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID arsip surat yang didisposisi */
    @NotNull
    @Column(nullable = false)
    private Long suratId;

    @NotNull
    @Column(nullable = false)
    private LocalDate tanggalDisposisi;

    /** Nama pegawai tujuan disposisi */
    @NotBlank
    @Column(nullable = false, length = 100)
    private String diteruskanKepada;

    /** Instruksi Kepala Sekolah */
    @Column(length = 500)
    private String instruksi;

    /** Batas waktu penyelesaian (opsional) */
    private LocalDate batasTanggal;

    /** Diproses / Selesai */
    @Column(length = 20)
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSuratId() { return suratId; }
    public void setSuratId(Long suratId) { this.suratId = suratId; }

    public LocalDate getTanggalDisposisi() { return tanggalDisposisi; }
    public void setTanggalDisposisi(LocalDate tanggalDisposisi) { this.tanggalDisposisi = tanggalDisposisi; }

    public String getDiteruskanKepada() { return diteruskanKepada; }
    public void setDiteruskanKepada(String diteruskanKepada) { this.diteruskanKepada = diteruskanKepada; }

    public String getInstruksi() { return instruksi; }
    public void setInstruksi(String instruksi) { this.instruksi = instruksi; }

    public LocalDate getBatasTanggal() { return batasTanggal; }
    public void setBatasTanggal(LocalDate batasTanggal) { this.batasTanggal = batasTanggal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
