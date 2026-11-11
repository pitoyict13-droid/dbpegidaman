package id.smpunggulanbanjarbaru.kepegawaian.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PegawaiRequest {

    @Size(max = 30)
    @Pattern(regexp = "^[0-9]*$", message = "NIP hanya boleh berisi angka")
    private String nip;

    @NotBlank
    @Size(max = 100)
    private String nama;

    /** L = Laki-laki, P = Perempuan */
    @Pattern(regexp = "^[LP]?$", message = "Jenis kelamin harus L atau P")
    private String jenisKelamin;

    @NotBlank
    @Size(max = 50)
    private String jabatan;

    @Size(max = 100)
    private String mataPelajaran;

    @NotBlank
    @Size(max = 20)
    private String statusKepegawaian;

    @Size(max = 20)
    private String pendidikanTerakhir;

    private LocalDate tanggalLahir;

    private LocalDate tanggalMasuk;

    @Size(max = 20)
    @Pattern(regexp = "^[0-9+\\-]*$", message = "Nomor telepon tidak valid")
    private String telepon;

    @Size(max = 100)
    @Email(message = "Format email tidak valid")
    private String email;

    @Size(max = 255)
    private String alamat;

    @Positive(message = "Gaji pokok harus positif")
    private BigDecimal gajiPokok;

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
