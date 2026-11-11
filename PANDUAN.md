# 📘 PANDUAN APLIKASI
## Sistem Informasi Kepegawaian & Persuratan
### SMP Unggulan Idaman Banjarbaru

---

# BAGIAN A — PANDUAN INSTALASI

## A.1 Persyaratan

| Kebutuhan | Keterangan |
|---|---|
| Komputer | Windows 10/11 (juga bisa Linux/Mac) |
| Java | JDK 17 ke atas (sudah terpasang bersama Android Studio, atau unduh di adoptium.net) |
| Database | PostgreSQL 16 (unduh di postgresql.org) |
| Browser | Chrome / Edge / Firefox versi terbaru |
| Jaringan | Komputer server dan HP/klien harus dalam satu jaringan (WiFi/LAN sekolah) |

## A.2 Menyiapkan Database PostgreSQL

1. Pastikan PostgreSQL sudah terpasang dan berjalan (layanan `postgresql-x64-16` berstatus *Running* di Services).
2. Buka **pgAdmin** (atau command prompt psql), lalu buat database baru:

```sql
CREATE DATABASE kepegawaian_smp;
```

3. Kredensial bawaan yang dipakai aplikasi:
   - Host: `localhost`, Port: `5432`
   - User: `postgres`, Password: `postgres`
4. Jika password PostgreSQL sekolah berbeda, ubah di file
   `src/main/resources/application.yml` pada bagian `spring.datasource.password`
   (atau set environment variable `DB_PASSWORD`).

> ⚠️ Tabel database **dibuat otomatis** saat aplikasi pertama kali dijalankan. Tidak perlu buat tabel manual.

## A.3 Menjalankan Aplikasi

1. Salin folder project `kepegawaian-smp` ke komputer server sekolah.
2. Buka **PowerShell** / Command Prompt, masuk ke folder project:

```bat
cd kepegawaian-smp
```

3. Arahkan ke Java yang terpasang, lalu jalankan:

```bat
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
.\mvnw.cmd spring-boot:run
```

   *(Jika menggunakan JDK sendiri, sesuaikan `JAVA_HOME`, mis. `C:\Program Files\Eclipse Adoptium\jdk-17`)*

4. Tunggu hingga muncul tulisan:

```
Tomcat started on port 8080
Aplikasi Kepegawaian SMP Unggulan Idaman Banjarbaru
Berjalan di http://localhost:8080
```

5. Buka browser ke **http://localhost:8080**

6. Untuk **menghentikan** aplikasi: tekan `Ctrl + C` di jendela PowerShell, atau tutup jendelanya.

### Agar otomatis jalan setiap komputer menyala (opsional)
Buat file `JALANKAN-APLIKASI.bat` di Desktop berisi:

```bat
@echo off
cd /d C:\lokasi\kepegawaian-smp
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
call .\mvnw.cmd spring-boot:run
pause
```

Klik dua kali file .bat tersebut setiap pagi, biarkan jendelanya terbuka.

## A.4 Akses dari HP / Komputer Lain

1. Di komputer server, cari alamat IP lokal:
   - Buka PowerShell, ketik `ipconfig`, catat **IPv4 Address** (contoh: `192.168.1.10`).
2. Izinkan port 8080 di firewall Windows:
   - **Windows Security → Firewall → Advanced settings → Inbound Rules → New Rule**
   - Port → TCP → `8080` → Allow.
3. Dari HP/komputer lain (satu WiFi), buka: `http://192.168.1.10:8080`
   *(ganti sesuai IP server)*

### Install sebagai aplikasi di HP Android (PWA)
1. Buka alamat aplikasi di **Chrome** Android.
2. Ketuk menu ⋮ → **Tambahkan ke layar utama**.
3. Aplikasi muncul dengan ikon sendiri seperti aplikasi biasa.

## A.5 Login Pertama Kali

| Kolom | Isi |
|---|---|
| Username | `admin` |
| Password | `admin123` |

> 🚨 **WAJIB segera ganti password bawaan** (lihat Bagian B.3). Password yang dibiarkan default membawa risiko data sekolah diubah orang lain.

---

# BAGIAN B — PANDUAN PENGGUNAAN

## B.1 Peran Pengguna

| Peran | Hak akses |
|---|---|
| **ADMIN** | Mengelola semua data: pegawai, pengguna, surat, disposisi, laporan |
| **STAFF** | Hanya **melihat** data + mengelola **disposisi miliknya sendiri** |

## B.2 Login & Keluar

- Isi username & password → klik **Masuk**.
- Klik tombol **Keluar** di kanan atas setelah selesai bekerja (token login langsung dicabut).

## B.3 Ganti Password

Menu **🔑 Ganti Password** → isi password lama, password baru (min. 6 karakter), konfirmasi → **Simpan**.

## B.4 Kelola Pengguna (ADMIN)

Menu **Pengguna** → **Tambah Pengguna**:
- **Username** untuk login
- **Password** min. 6 karakter
- **Nama Lengkap** — ⚠️ **harus sama persis dengan nama pegawai** di Data Pegawai, karena nama inilah yang dipakai sistem untuk mencocokkan **disposisi surat** ke pemiliknya.
- **Peran**: ADMIN atau STAFF

## B.5 Dashboard

Ringkasan otomatis: total pegawai, guru & pimpinan, tenaga kependidikan, pegawai tetap, jumlah arsip surat masuk & keluar, plus grafik sebaran status dan jabatan.

## B.6 Data Pegawai

**Tambah:** tombol **+ Tambah** → isi NIP (boleh kosong), nama, jenis kelamin, **Status Kepegawaian** (`Pegawai Tetap` / `Pegawai Tidak Tetap`), jabatan, mapel, pendidikan, tanggal, kontak, gaji, alamat → Simpan.

**Cari:** ketik nama di kotak pencarian.

**Ubah / Hapus:** tombol ✏️ / 🗑 pada baris pegawai.

### Import Data dari Excel (massal)
1. Klik **Template** → file `template-pegawai.xlsx` terunduh.
2. Isi data mulai baris ke-3 (baris contoh boleh dihapus). Kolom wajib: Nama, Jabatan, Status Kepegawaian. Tanggal format `yyyy-MM-dd`.
3. Klik **Import Excel** → pilih file → **Unggah**.
4. Sistem menampilkan jumlah berhasil/gagal beserta alasannya per baris. Data yang gagal bisa diperbaiki lalu diunggah ulang — **tidak ada data ganda** karena NIP unik.

## B.7 Surat Masuk 📩

Menu **Surat Masuk** → **Catat Surat Masuk**:

| Kolom | Keterangan |
|---|---|
| Nomor Surat | Nomor di kop surat pengirim, mis. `421.2/123/DISDIK/2026` |
| Klasifikasi | Kode arsip Srikandi: UM, PT, SN, KP, UN, PG, PU, dst. (17 kode) |
| Tanggal Surat | Tanggal pada surat |
| Pengirim | Instansi/orang pengirim (wajib) |
| Perihal | Ringkasan isi surat |
| Sifat | Biasa / Penting / Segera / Rahasia |
| Status | Belum Diproses / Diproses / Selesai |
| Lampiran | Scan/softcopy surat (PDF/gambar), maks. 10 MB |

**Nomor agenda dibuat OTOMATIS** oleh sistem dengan format
`urut/kode klasifikasi/tahun` — contoh: `001/UM/2026`, `002/UM/2026`.
Nomor urut naik sendiri dan direset tiap tahun. **Tidak perlu diisi manual.**

Tombol aksi pada tabel: 📤 Disposisi, ✏️ Ubah, 🗑 Hapus, 📎 Unduh lampiran.

## B.8 Surat Keluar 📤

Sama seperti surat masuk, tetapi kolom **Pengirim** diganti **Tujuan** (kepada siapa surat dikirim) dan pilihan status menjadi **Terkirim / Arsip**. Nomor agenda juga otomatis (urutan terpisah dari surat masuk).

## B.9 Disposisi Surat Masuk ✍️

1. Buka **Surat Masuk** → klik tombol **📤 (Disposisi)**.
2. Tambah disposisi: **Diteruskan Kepada** (ketik nama pegawai — nama akan muncul saran otomatis), **Instruksi Kepala Sekolah**, **Batas Waktu**, status **Diproses/Selesai** → Simpan.
3. Satu surat boleh didisposisikan ke beberapa pegawai.

> Sistem mencocokkan disposisi dengan akun pengguna melalui **Nama Lengkap** (lihat B.4).

## B.10 Notifikasi "Disposisi Saya" 🔔

- Pegawai yang didisposisikan melihat **lonceng di navbar** dengan angka merah (jumlah yang perlu dikerjakan). Diperbarui otomatis tiap **60 detik**.
- Klik lonceng / menu **Disposisi Saya** → daftar surat + instruksi + batas waktu (jika lewat batas, ditandai merah **"Terlambat"**).
- Setelah selesai, klik **✅ Tandai Sudah Dikerjakan** → status berubah "Selesai" dan angka lonceng berkurang.
- Disposisi **hanya terlihat oleh penerima** — data pegawai lain tetap aman.

## B.11 Laporan Daftar Surat 📄

Di halaman Surat Masuk / Surat Keluar tersedia:

- **Export Excel** — mengunduh *Buku Agenda Surat* berkop sekolah, kolom lengkap, plus tanda tangan Kepala Sekolah. Isi laporan mengikuti kata kunci pencarian yang aktif.
- **Cetak / PDF** — membuka tampilan A4 *landscape* berkop (logo sekolah) siap cetak. Untuk PDF: di dialog cetak pilih tujuan **"Save as PDF" / "Simpan sebagai PDF"**.

## B.12 Rekap Persuratan 📈

Menu **Rekap Surat**: pilih **tahun** → kartu total, grafik per klasifikasi (masuk & keluar), grafik per bulan, dan tabel rekap bulanan Januari–Desember yang siap dilampirkan ke laporan.

## B.13 Masalah Umum (FAQ)

| Masalah | Penyebab & solusi |
|---|---|
| Tidak bisa login | Pastikan aplikasi berjalan (jendela PowerShell masih terbuka), username/password benar, atau password sudah diganti orang lain |
| Tampilan tidak berubah setelah update | Tekan **Ctrl + F5** (muat ulang paksa) |
| Tombol tidak bereaksi | Muat ulang halaman (F5); bila tetap, **Ctrl + F5** |
| Lampiran gagal unggah | Ukuran file maks. 10 MB; kompres/scan ulang surat |
| Notifikasi disposisi tidak muncul | Pastikan **Nama Lengkap** akun sama persis dengan nama tujuan disposisi; tunggu maks. 1 menit atau muat ulang halaman |
| Nomor agenda tidak berurutan | Wajar — nomor per kode klasifikasi & tipe surat dihitung terpisah (001/UM, 001/UN, dst.) |
| HP tidak bisa membuka aplikasi | Pastikan satu WiFi dengan server, IP benar, firewall port 8080 dibuka |

## B.14 Keamanan Data

- Login memakai token **JWT** (berlaku 12 jam); tombol **Keluar** langsung mencabut token.
- Data tersimpan di database **PostgreSQL** di komputer server — disarankan rutin mem-backup:
  ```bat
  pg_dump -U postgres -h localhost kepegawaian_smp > backup-kepegawaian.sql
  ```
  (file muncul di folder tempat perintah dijalankan; simpan ke flashdisk/drive sekolah)
- Folder `uploads/surat` menyimpan berkas lampiran — jangan dihapus/hapus ikut sertakan saat backup.

---

*Panduan ini untuk Aplikasi Kepegawaian & Persuratan SMP Unggulan Idaman Banjarbaru — dibuat November 2026.*
