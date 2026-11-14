# Sistem Informasi Data Sekolah (Dapodik) — SMP Unggulan Idaman Banjarbaru

Sistem informasi data pokok sekolah **gaya Dapodik** (Data Pokok Pendidikan) berbasis **Java (Spring Boot)** dengan keamanan login menggunakan **JSON Web Token (JWT)** dan database **PostgreSQL**. Frontend berupa aplikasi web responsif yang disajikan langsung oleh backend, sehingga bisa juga dipakai dari HP dan nantinya dapat dikembangkan menjadi aplikasi Android karena seluruh fitur berjalan di atas REST API.

> Fork dari sistem kepegawaian sekolah sebelumnya; modul kepegawaian, persuratan, dan disposisi tetap tersedia, ditambah modul **Profil Sekolah, Peserta Didik, Kesiswaan, dan Sarana Prasarana**.

## Fitur

- 🔐 **Login JWT** — token bertanda tangan HMAC-SHA256, masa berlaku 12 jam, logout mencabut token (blacklist)
- 🏫 **Profil Sekolah** — identitas sekolah (nama, NPSN, NSS, status sekolah, akreditasi, kurikulum), alamat & kontak, kepala sekolah, luas tanah/bangunan, daya listrik, internet, serta **visi & misi** — bisa dilihat semua pengguna dan **diedit oleh ADMIN**
- 🎒 **CRUD Peserta Didik (Dapodik)** — NISN, NIS, nama, jenis kelamin, agama, tempat/tanggal lahir, alamat, data orang tua (nama, pekerjaan, penghasilan, HP), kelas, angkatan, status (Aktif/Lulus/Pindah/Keluar/Meninggal), dan tanggal masuk
- 🏆 **CRUD Kesiswaan** — catatan prestasi, pelanggaran (dengan poin), ekstrakurikuler, beasiswa, dan lainnya per siswa (nama bisa dipilih dari data peserta didik)
- 🔨 **CRUD Sarana Prasarana** — gedung, ruang kelas, laboratorium, perpustakaan, kantor, toilet, lapangan, alat peraga, furnitur, buku & media; lengkap dengan kode inventaris, jumlah, kondisi (Baik/Rusak Ringan/Sedang/Berat), luas, tahun pengadaan, dan sumber dana
- 👥 **CRUD Data Pegawai** — NIP, nama, jabatan, status (Pegawai Tetap / Pegawai Tidak Tetap), mapel, pendidikan, tanggal lahir/masuk, telepon, email, gaji pokok, alamat
- 📊 **Dashboard** — statistik total pegawai, peserta didik aktif, item sarpras, prestasi siswa, arsip surat, sebaran status kepegawaian/jabatan/kelas
- 📨 **Manajemen Persuratan** — arsip surat masuk & surat keluar dengan **penomoran agenda otomatis bergaya Srikandi (Kalsel)** (`urut/kode klasifikasi/tahun`, contoh `001/UM/2026`), 17 kode klasifikasi arsip, sifat surat, status tindak lanjut, dan **lampiran file** (maks 10 MB)
- ✍️ **Disposisi surat masuk** — surat diteruskan kepada pegawai (nama dari data pegawai) beserta instruksi Kepala Sekolah, batas waktu, dan status tindak lanjut
- 📄 **Laporan daftar surat** — export **Excel** (buku agenda berkop) dan **Cetak / Simpan PDF** langsung dari browser
- 🔔 **Notifikasi web "Disposisi Saya"** — pegawai yang didisposisikan melihat lonceng 🔔 di navbar (angka merah = jumlah yang perlu dikerjakan), diperbarui tiap 60 detik
- 📈 **Rekap persuratan** — statistik per klasifikasi dan per bulan dengan filter tahun
- 🛡 **Peran pengguna** — `ADMIN` (kelola semua data & pengguna) dan `STAFF` (hanya melihat)
- 📥 **Import Excel siswa, sarpras & persuratan** — unduh template `.xlsx` (sudah berisi sheet **Petunjuk**), isi data, lalu unggah. Import berjalan di latar belakang dengan **progress bar**, baris duplikat **dilewati & dilaporkan** (bukan ditimpa). Maks **8 MB / 1.000 baris** per file (Apache POI). Import pegawai tetap tersedia seperti sebelumnya.
- 📱 Frontend responsif **Bootstrap 5** + PWA (bisa di-install di Android dari Chrome)

## Teknologi

| Komponen | Teknologi |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Keamanan | JJWT 0.12 (JSON Web Token), BCrypt |
| Database | PostgreSQL (default) / H2 (opsional, untuk coba lokal) |
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

Syarat: **JDK 17** dan **Maven** (atau gunakan IDE seperti IntelliJ/Eclipse; atau gunakan `mvnw` yang sudah tersedia).

```bash
cd dbpegidaman
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Buka browser: **http://localhost:8080**

### Coba lokal tanpa PostgreSQL (profil H2)

Aplikasi juga bisa dijalankan dengan database file H2 bawaan (data tersimpan di `./data/dapodik-smp.mv.db`):

```bash
./mvnw -Ph2 spring-boot:run \
  -Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:file:./data/dapodik-smp \
  -Dspring-boot.run.arguments=--spring.datasource.driver-class-name=org.h2.Driver \
  -Dspring-boot.run.arguments=--spring.datasource.username=sa \
  -Dspring-boot.run.arguments=--spring.datasource.password=
```

### Menjalankan di Linux (satu perintah)

Skrip `jalankan-linux.sh` otomatis mencari **Java 17**, mengunduh driver H2 bila belum ada, lalu menjalankan aplikasi dengan database lokal H2 — tidak perlu memasang Maven/JDK secara manual.

```bash
./jalankan-linux.sh              # http://localhost:8080
PORT=9090 ./jalankan-linux.sh    # ganti port
```

Hentikan dengan `Ctrl+C`. Untuk PostgreSQL/Neon, cukup set `SPRING_DATASOURCE_URL`, `DB_USERNAME`, dan `DB_PASSWORD` seperti biasa.

Agar aplikasi otomatis hidup saat server Linux menyala (systemd):

```bash
./pasang-service-linux.sh        # minta password sudo sekali
sudo systemctl status dapodik-smp
sudo journalctl -u dapodik-smp -f
```

> Bila Java 17 belum terpasang, skrip menampilkan perintah pemasangannya (`sudo apt install openjdk-17-jdk`, atau unduh JRE 17 tanpa sudo).

## Deploy Online Gratis (Back4App + Neon)

Agar pengurus sekolah bisa membuka aplikasi dari mana saja **tanpa kartu kredit**, kombinasi yang dipakai:

| Komponen | Layanan | Biaya |
|---|---|---|
| Web service (Docker) | [Back4App Containers](https://www.back4app.com) — Free: 0,25 CPU / 256 MB | Rp 0, tanpa kartu |
| Database PostgreSQL | [Neon](https://neon.tech) free tier | Rp 0 |

Repo ini sudah berisi `Dockerfile` yang disetel agar muat di RAM 256 MB (heap 96 MB, code cache 32 MB). Hasil uji lokal: RSS ±210 MB saat diam dan ±253 MB saat import 1.000 baris — masih di bawah 256 MB (sebagian RSS adalah cache file JAR yang bisa dilepas sistem saat tertekan).

### Langkah-langkah

1. **Push repo ini ke GitHub** (repo publik `pitoyict13-droid/dbpegidaman`, branch `main`).
2. **Database Neon** — proyek `my-first-project` (region Singapura) sudah berisi skema. Nilai koneksi ada di `.env.local` (tidak di-commit).
3. **Daftar Back4App** (gratis, tanpa kartu): https://www.back4app.com/signup
4. Buka menu **Containers** → **Create New App** → hubungkan akun GitHub → pilih repo `dbpegidaman`, branch `main`, root `/`.
5. Isi **Environment Variables**:

   | Key | Nilai |
   |---|---|
   | `DB_HOST` | host Neon (lihat `.env.local`) |
   | `DB_PORT` | `5432` |
   | `DB_NAME` | `neondb` |
   | `DB_USERNAME` | `neondb_owner` |
   | `DB_PASSWORD` | password Neon (lihat `.env.local`) |
   | `DB_PARAMS` | `?sslmode=require` |
   | `JWT_SECRET` | string acak min. 32 karakter |

   Opsional untuk menghemat RAM: `DB_POOL_SIZE=3`, `TOMCAT_MAX_THREADS=15`.
6. Klik **Create App**, tunggu build Docker selesai (5–15 menit). URL aplikasi muncul di **App Overview**.
7. Buka URL tersebut → login `admin` / `admin123` → **segera ganti password** dan isi **Profil Sekolah**.

### Catatan penting (free tier)

- **RAM hanya 256 MB / 0,25 CPU.** Aplikasi sudah dibatasi `-Xmx96m`; jangan menaikkan heap melebihi 128 MB.
- **Region Back4App = USA** sedangkan Neon di Singapura, jadi setiap query menambah ±0,2 detik. Bila terasa lambat, buat proyek Neon baru di region `aws-us-east-1` lalu ganti `DB_HOST`.
- **Lampiran surat** tersimpan di filesystem container yang *ephemeral*: hilang saat re-deploy/restart. Backup berkala `uploads/` atau jalankan di server sekolah untuk arsip permanen.
- **Neon free ≈ 0,5 GB** — cukup untuk puluhan ribu siswa/pegawai.
- Jangan set `PORT`; Back4App memakai port `8080` yang di-`EXPOSE` di Dockerfile.
- Deployment pertama bisa gagal karena health check terlalu cepat — klik **Actions → Deploy the Last Commit** untuk mencoba ulang.

### Alternatif gratis lain

- **Cloudflare Tunnel** (tanpa akun, tanpa kartu): jalankan aplikasi di komputer sekolah lalu
  `cloudflared tunnel --url http://localhost:8080`. URL `*.trycloudflare.com` langsung aktif, tetapi komputer harus menyala dan URL berubah setiap tunnel dijalankan ulang.
- **Render** (`render.yaml` sudah tersedia) — paket Free tetap ada, tetapi akun baru bisa diminta menambahkan kartu.

## Login Awal

| Username | Password | Peran |
|---|---|---|
| `admin` | `admin123` | ADMIN |

> ⚠️ **Segera ganti password admin** setelah login pertama (menu *Ganti Password*). Akun admin otomatis dibuat hanya saat database masih kosong. Profil sekolah bawaan (nama "SMP Unggulan Idaman Banjarbaru") juga dibuat otomatis saat database masih kosong.

## Import Excel (Siswa, Sarana Prasarana, Persuratan)

Halaman **Peserta Didik**, **Sarana Prasarana**, **Surat Masuk**, dan **Surat Keluar** memiliki tombol **Template** dan **Import Excel** (khusus ADMIN).

1. Klik **Template** untuk mengunduh file `.xlsx`. Sheet pertama adalah data; sheet **Petunjuk** berisi aturan pengisian dan contoh (contoh tidak ikut terimport).
2. Isi data mulai **baris ke-2**; jangan mengubah/menggeser header.
3. Simpan sebagai `.xlsx`, lalu klik **Import Excel** dan unggah.

Ketentuan:

- Maksimum **8 MB** dan **1.000 baris** per file. Untuk data lebih banyak, bagi menjadi beberapa file.
- Hanya **satu proses import** yang boleh berjalan pada satu waktu; tunggu sampai selesai.
- Baris **duplikat dilewati** dan dilaporkan: NISN/NIS untuk siswa, Nomor Surat untuk persuratan. Data lama tidak ditimpa.
- Import berjalan **di latar belakang** dengan progress bar, sehingga tidak timeout meski server hosting gratis dan database cloud.
- Nomor agenda surat (`001/UM/2026`) tetap dibuat otomatis; tanggal catat boleh dikosongkan (default hari ini).

## Struktur REST API

| Metode | Endpoint | Akses | Keterangan |
|---|---|---|---|
| POST | `/api/auth/login` | publik | Login, mengembalikan token JWT |
| GET | `/api/auth/me` | login | Profil pengguna aktif |
| POST | `/api/auth/logout` | login | Mencabut token |
| GET | `/api/profil-sekolah` | login | Lihat profil sekolah |
| PUT | `/api/profil-sekolah` | ADMIN | Simpan/perbarui profil sekolah |
| GET | `/api/peserta-didik?cari=...` | login | Daftar peserta didik (+ pencarian) |
| GET | `/api/peserta-didik/{id}` | login | Detail peserta didik |
| POST | `/api/peserta-didik` | ADMIN | Tambah peserta didik |
| PUT | `/api/peserta-didik/{id}` | ADMIN | Ubah peserta didik |
| DELETE | `/api/peserta-didik/{id}` | ADMIN | Hapus peserta didik |
| GET | `/api/kesiswaan?cari=...` | login | Daftar catatan kesiswaan (+ pencarian) |
| GET | `/api/kesiswaan/{id}` | login | Detail catatan kesiswaan |
| POST | `/api/kesiswaan` | ADMIN | Tambah catatan kesiswaan |
| PUT | `/api/kesiswaan/{id}` | ADMIN | Ubah catatan kesiswaan |
| DELETE | `/api/kesiswaan/{id}` | ADMIN | Hapus catatan kesiswaan |
| GET | `/api/sarana-prasarana?cari=...&kategori=...` | login | Daftar sarana prasarana (+ pencarian & filter kategori) |
| GET | `/api/sarana-prasarana/{id}` | login | Detail sarana prasarana |
| POST | `/api/sarana-prasarana` | ADMIN | Tambah sarana prasarana |
| PUT | `/api/sarana-prasarana/{id}` | ADMIN | Ubah sarana prasarana |
| DELETE | `/api/sarana-prasarana/{id}` | ADMIN | Hapus sarana prasarana |
| GET | `/api/pegawai?cari=...` | login | Daftar pegawai (+ pencarian) |
| GET | `/api/pegawai/{id}` | login | Detail pegawai |
| GET | `/api/pegawai/template` | login | Unduh template Excel (.xlsx) |
| POST | `/api/pegawai/import` | ADMIN | Import massal dari file Excel |
| GET | `/api/import/template/{jenis}` | ADMIN | Unduh template import (`siswa`, `sarpras`, `surat`) |
| POST | `/api/import/{jenis}` | ADMIN | Mulai import Excel (async) — mengembalikan `jobId` |
| GET | `/api/import/status/{jobId}` | ADMIN | Progres & hasil import (dipantau tiap 1,5 detik) |
| POST | `/api/pegawai` | ADMIN | Tambah pegawai |
| PUT | `/api/pegawai/{id}` | ADMIN | Ubah pegawai |
| DELETE | `/api/pegawai/{id}` | ADMIN | Hapus pegawai |
| GET | `/api/surat?tipe=MASUK&cari=...` | login | Daftar arsip surat (tipe: MASUK/KELUAR, + pencarian) |
| GET | `/api/surat/{id}` | login | Detail arsip surat |
| GET | `/api/surat/{id}/lampiran` | login | Unduh lampiran surat |
| POST | `/api/surat` | ADMIN | Catat surat baru (form-data + lampiran opsional) — nomor agenda otomatis |
| PUT | `/api/surat/{id}` | ADMIN | Ubah arsip surat |
| DELETE | `/api/surat/{id}` | ADMIN | Hapus arsip surat |
| GET | `/api/surat/{id}/disposisi` | login | Daftar disposisi suatu surat |
| POST | `/api/surat/{id}/disposisi` | ADMIN | Buat disposisi (diteruskan kepada + instruksi) |
| DELETE | `/api/surat/{id}/disposisi/{disposisiId}` | ADMIN | Hapus disposisi |
| GET | `/api/surat/export?tipe=MASUK` | login | Laporan Excel buku agenda surat |
| GET | `/api/disposisi-saya` | login | Daftar disposisi untuk pengguna yang sedang login |
| POST | `/api/disposisi-saya/{id}/selesai` | login | Tandai disposisi saya sudah dikerjakan |
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
dbpegidaman/
├── pom.xml
└── src/main/
    ├── java/id/smpunggulanbanjarbaru/kepegawaian/
    │   ├── KepegawaianApplication.java
    │   ├── config/DataInitializer.java          # akun admin + profil sekolah bawaan
    │   ├── controller/ (Auth, Pegawai, User, Surat, DisposisiSaya,
    │   │                ProfilSekolah, PesertaDidik, Kesiswaan, SaranaPrasarana)
    │   ├── dto/         (Login, Register, AuthResponse, PegawaiRequest)
    │   ├── exception/   (GlobalExceptionHandler)
    │   ├── model/       (User, Pegawai, Surat, Disposisi, Role,
    │   │                ProfilSekolah, PesertaDidik, Kesiswaan, SaranaPrasarana)
    │   ├── repository/  (UserRepository, PegawaiRepository, SuratRepository, ...)
    │   ├── security/    (JwtService, JwtAuthenticationFilter, SecurityConfig, TokenBlacklistService, CustomUserDetailsService)
    │   └── service/     (AuthService, PegawaiService, SuratService, ExcelService, ...)
    └── resources/
        ├── application.yml
        └── static/      (frontend: index.html, css/, js/, manifest.json, sw.js)
```
