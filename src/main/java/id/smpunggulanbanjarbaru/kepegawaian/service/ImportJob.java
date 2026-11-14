package id.smpunggulanbanjarbaru.kepegawaian.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Status satu proses import Excel yang berjalan di latar belakang.
 * Disimpan di memori selama aplikasi hidup; cukup untuk satu instance
 * (Back4App free tier hanya menjalankan satu container).
 */
public class ImportJob {

    public enum Status {
        MENUNGGU, BERJALAN, SELESAI, GAGAL
    }

    /** Batas pesan error yang disimpan agar memori tetap kecil. */
    static final int MAKS_PESAN = 100;

    private final String id;
    private final String jenis;
    private final Instant dibuat = Instant.now();
    private final List<String> pesanGagal = Collections.synchronizedList(new ArrayList<>());
    private volatile Status status = Status.MENUNGGU;
    private volatile int total;
    private volatile int diproses;
    private volatile int berhasil;
    private volatile int gagal;
    private volatile String pesan;
    private volatile Instant selesai;

    public ImportJob(String id, String jenis) {
        this.id = id;
        this.jenis = jenis;
    }

    public String getId() { return id; }

    public String getJenis() { return jenis; }

    public Status getStatus() { return status; }

    public int getTotal() { return total; }

    public int getDiproses() { return diproses; }

    public int getBerhasil() { return berhasil; }

    public int getGagal() { return gagal; }

    public String getPesan() { return pesan; }

    public Instant getDibuat() { return dibuat; }

    public Instant getSelesai() { return selesai; }

    void setTotal(int total) { this.total = total; }

    void mulai() {
        this.status = Status.BERJALAN;
    }

    /** Satu baris berhasil disimpan. */
    void sukses() {
        this.berhasil++;
        this.diproses++;
    }

    /** Satu baris gagal; pesan disimpan sampai batas maksimum. */
    void gagal(String alasan) {
        this.gagal++;
        this.diproses++;
        if (pesanGagal.size() < MAKS_PESAN) {
            pesanGagal.add(alasan);
        }
    }

    void selesai() {
        this.status = Status.SELESAI;
        this.selesai = Instant.now();
    }

    void gagalTotal(String alasan) {
        this.pesan = alasan;
        this.status = Status.GAGAL;
        this.selesai = Instant.now();
    }

    public Map<String, Object> keMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jobId", id);
        m.put("jenis", jenis);
        m.put("status", status.name());
        m.put("total", total);
        m.put("diproses", diproses);
        m.put("berhasil", berhasil);
        m.put("gagal", gagal);
        m.put("pesan", pesan);
        m.put("pesanGagal", List.copyOf(pesanGagal));
        m.put("pesanTerpotong", gagal > pesanGagal.size());
        m.put("mulai", dibuat.toString());
        m.put("selesai", selesai == null ? null : selesai.toString());
        return m;
    }
}
