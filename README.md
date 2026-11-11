# Aplikasi Kepegawaian SMP Unggulan Idaman Banjarbaru

Sistem informasi kepegawaian berbasis **Java (Spring Boot)** dengan keamanan login menggunakan **JSON Web Token (JWT)** dan database **PostgreSQL**. Frontend berupa aplikasi web responsif yang disajikan langsung oleh backend, sehingga bisa juga dipakai dari HP dan nantinya dapat dikembangkan menjadi aplikasi Android karena seluruh fitur berjalan di atas REST API.

## Fitur

- 🔐 **Login JWT** — token bertanda tangan HMAC-SHA256, masa berlaku 12 jam, logout mencabut token (blacklist)
- 👥 **CRUD Data Pegawai** — NIP, nama, jabatan, status (Pegawai Tetap / Pegawai Tidak Tetap), mapel, pendidikan, tanggal lahir/masuk, telepon, email, gaji pokok, alamat
- 📊 **Dashboard** — statistik total pegawai, sebaran status kepegawaian, sebaran jabatan, jumlah arsip surat masuk & keluar
- 📨 **Manajemen Persuratan** — arsip surat masuk & surat keluar dengan **penomoran agenda otomatis bergaya Srikandi (Kalsel)** (`urut/kode klasifikasi/tahun`, contoh `001/UM/2026`), 17 kode klasifikasi arsip (UM, PT, SN, KP, UN, dst.), sifat surat (Biasa/Penting/Segera/Rahasia), status tindak lanjut, dan **lampiran file** (scan/softcopy surat, maks 10 MB) yang bisa diunduh
- 🛡 **Peran pengguna** — `ADMIN` (kelola semua data & pengguna) dan `STAFF` (hanya melihat)
- 📥 **Import Excel** — unduh template `.xlsx`, isi data pegawai, lalu unggah untuk masuk massal ke database (Apache POI, dengan validasi baris per baris dan laporan baris yang gagal)
- 🔎 Pencarian pegawai, ubah password, kelola pengguna (khusus admin)
- 📱 Frontend responsif **Bootstrap 5** + PWA (bisa di-install di Android dari Chrome)

## Teknologi

| Komponen | Teknologi |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Keamanan | JJWT 0.12 (JSON Web Token), BCrypt |
| Database | PostgreSQL |
| Frontend | HTML/CSS/JavaScript (disajikan Spring Boot, tanpa build) |

## Persiapan Database PostgreSQL

Buat database di PostgreSQL (mis. lewat `psql` atau pgAdmin):

```sql
CREATE DATABASE kepegawaian_smp;
```

Tabel akan dibuat otomatis oleh Hibernate saat aplikasi pertama kali dijalankan.

Konfigurasi dapat diubah lewat file `src/main/resources/application.yml`
atau environment variable: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, dan `JWT_SECRET`.

## Menjalankan Aplikasi

Syarat: **JDK 17** dan **Maven** (atau gunakan IDE seperti IntelliJ/Eclipse).

```bash
cd kepegawaian-smp
mvn spring-boot:run
```

Buka browser: **http://localhost:8080**

## Login Awal

| Username | Password | Peran |
|---|---|---|
| `admin` | `admin123` | ADMIN |

> ⚠️ **Segera ganti password admin** setelah login pertama (menu *Ganti Password*). Akun admin otomatis dibuat hanya saat database masih kosong.

## Struktur REST API

| Metode | Endpoint | Akses | Keterangan |
|---|---|---|---|
| POST | `/api/auth/login` | publik | Login, mengembalikan token JWT |
| GET | `/api/auth/me` | login | Profil pengguna aktif |
| POST | `/api/auth/logout` | login | Mencabut token |
| GET | `/api/pegawai?cari=...` | login | Daftar pegawai (+ pencarian) |
| GET | `/api/pegawai/{id}` | login | Detail pegawai |
| GET | `/api/pegawai/template` | login | Unduh template Excel (.xlsx) |
| POST | `/api/pegawai/import` | ADMIN | Import massal dari file Excel |
| POST | `/api/pegawai` | ADMIN | Tambah pegawai |
| PUT | `/api/pegawai/{id}` | ADMIN | Ubah pegawai |
| DELETE | `/api/pegawai/{id}` | ADMIN | Hapus pegawai |
| GET | `/api/surat?tipe=MASUK&cari=...` | login | Daftar arsip surat (tipe: MASUK/KELUAR, + pencarian) |
| GET | `/api/surat/{id}` | login | Detail arsip surat |
| GET | `/api/surat/{id}/lampiran` | login | Unduh lampiran surat |
| POST | `/api/surat` | ADMIN | Catat surat baru (form-data + lampiran opsional) — nomor agenda otomatis |
| PUT | `/api/surat/{id}` | ADMIN | Ubah arsip surat |
| DELETE | `/api/surat/{id}` | ADMIN | Hapus arsip surat |
| GET/POST/DELETE | `/api/users` | ADMIN | Kelola pengguna |
| POST | `/api/users/change-password` | login | Ganti password sendiri |

Semua endpoint selain `/api/auth/login` memerlukan header:

```
Authorization: Bearer <token>
```

## Cara Kerja Keamanan JWT

1. Klien mengirim `POST /api/auth/login` dengan username & password.
2. Server memeriksa kredensial (password disimpan sebagai **hash BCrypt**).
3. Bila benar, server membuat **JWT** berisi username + peran, ditandatangani dengan secret key, berlaku 12 jam.
4. Setiap request berikutnya mengirim header `Authorization: Bearer <token>`.
5. `JwtAuthenticationFilter` memverifikasi tanda tangan & kedaluwarsa token, lalu memasukkan user ke SecurityContext.
6. `Logout` memasukkan token ke **blacklist** sehingga tidak bisa dipakai lagi.

## Rute Pengembangan ke Android

Karena semua logika sudah berupa REST API + JWT, aplikasi Android di masa depan cukup memanggil API yang sama (mis. dengan Retrofit) — backend tidak perlu diubah. Alternatif lain: frontend web ini sudah berbentuk **PWA** sehingga bisa langsung di-install di HP Android melalui Chrome (Menu → Tambahkan ke layar utama).

## Struktur Project

```
kepegawaian-smp/
├── pom.xml
└── src/main/
    ├── java/id/smpunggulanbanjarbaru/kepegawaian/
    │   ├── KepegawaianApplication.java
    │   ├── config/DataInitializer.java          # akun admin awal
    │   ├── controller/ (Auth, Pegawai, User)
    │   ├── dto/         (Login, Register, AuthResponse, PegawaiRequest)
    │   ├── exception/   (GlobalExceptionHandler)
    │   ├── model/       (User, Pegawai, Role)
    │   ├── repository/  (UserRepository, PegawaiRepository)
    │   ├── security/    (JwtService, JwtAuthenticationFilter, SecurityConfig, TokenBlacklistService, CustomUserDetailsService)
    │   └── service/     (AuthService, PegawaiService, ExcelService)
    └── resources/
        ├── application.yml
        └── static/      (frontend: index.html, css/, js/, manifest.json, sw.js)
```
