package id.smpunggulanbanjarbaru.kepegawaian.model;

/**
 * Peran pengguna aplikasi.
 * ADMIN  : kelola pegawai, kelola pengguna, semua akses.
 * STAFF  : hanya melihat data pegawai (read-only).
 */
public enum Role {
    ADMIN,
    STAFF
}
