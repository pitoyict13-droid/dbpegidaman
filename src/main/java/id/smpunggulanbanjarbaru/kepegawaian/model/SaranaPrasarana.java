package id.smpunggulanbanjarbaru.kepegawaian.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Sarana prasarana sekolah (gaya Dapodik): gedung, ruang, aset/peralatan, dsb.
 */
@Entity
@Table(name = "sarana_prasarana")
public class SaranaPrasarana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Gedung / Ruang Kelas / Laboratorium / Perpustakaan / Ruang Kantor /
     * Toilet/MCK / Lapangan/Olahraga / Alat Peraga/Peralatan / Furnitur/Mebel /
     * Buku & Media / Lainnya
     */
    @NotBlank
    @Column(nullable = false, length = 50)
    private String kategori;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String nama;

    @Column(length = 50)
    private String kode;

    private Integer jumlah;

    /** Baik / Rusak Ringan / Rusak Sedang / Rusak Berat */
    @Column(nullable = false, length = 20)
    private String kondisi = "Baik";

    /** Luas (m²), untuk gedung/ruang */
    private Double luas;

    private Integer tahunPengadaan;

    /** BOS / APBD / APBN / Dana Mandiri / Lainnya */
    @Column(length = 30)
    private String sumberDana;

    @Column(length = 500)
    private String keterangan;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }

    public Integer getJumlah() { return jumlah; }
    public void setJumlah(Integer jumlah) { this.jumlah = jumlah; }

    public String getKondisi() { return kondisi; }
    public void setKondisi(String kondisi) { this.kondisi = kondisi; }

    public Double getLuas() { return luas; }
    public void setLuas(Double luas) { this.luas = luas; }

    public Integer getTahunPengadaan() { return tahunPengadaan; }
    public void setTahunPengadaan(Integer tahunPengadaan) { this.tahunPengadaan = tahunPengadaan; }

    public String getSumberDana() { return sumberDana; }
    public void setSumberDana(String sumberDana) { this.sumberDana = sumberDana; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
}
