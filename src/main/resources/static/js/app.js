/* =========================================================
 * Sistem Informasi Data Sekolah (Dapodik) — SMP Unggulan Idaman
 * Frontend Bootstrap 5 — memanggil REST API backend + JWT.
 * ========================================================= */
"use strict";

const API = {
    login: "/api/auth/login",
    me: "/api/auth/me",
    logout: "/api/auth/logout",
    pegawai: "/api/pegawai",
    template: "/api/pegawai/template",
    importFile: "/api/pegawai/import",
    importTemplate: (jenis) => `/api/import/template/${jenis}`,
    importMulai: (jenis) => `/api/import/${jenis}`,
    importStatus: (jobId) => `/api/import/status/${jobId}`,
    users: "/api/users",
    changePassword: "/api/users/change-password",
    surat: "/api/surat",
    disposisiSaya: "/api/disposisi-saya",
    profilSekolah: "/api/profil-sekolah",
    pesertaDidik: "/api/peserta-didik",
    saranaPrasarana: "/api/sarana-prasarana",
    kesiswaan: "/api/kesiswaan",
};

const state = {
    token: localStorage.getItem("jwt_token") || null,
    user: null,
    pegawaiList: [],
    editId: null,
    surat: { MASUK: [], KELUAR: [] },
    editSuratId: null,
    notifikasi: [],
    profilSekolah: null,
    pesertaDidikList: [],
    saranaList: [],
    kesiswaanList: [],
    editSiswaId: null,
    editSarprasId: null,
    editKesiswaanId: null,
    importJenis: null,
    importTimer: null,
    importJalan: false,
};

const $ = (sel) => document.querySelector(sel);

/* Tandai ke skrip fallback bahwa app.js dimuat dengan sukses */
window.__APP_READY = true;

/* ---------- komponen Bootstrap ---------- */
const modalPegawai = () => new bootstrap.Modal("#modal-pegawai");
const modalUser = () => new bootstrap.Modal("#modal-user");
const modalImport = () => new bootstrap.Modal("#modal-import");
const modalImportData = () => bootstrap.Modal.getOrCreateInstance("#modal-import-data");
const modalSurat = () => new bootstrap.Modal("#modal-surat");
const modalSiswa = () => new bootstrap.Modal("#modal-siswa");
const modalKesiswaan = () => new bootstrap.Modal("#modal-kesiswaan");
const modalSarpras = () => new bootstrap.Modal("#modal-sarpras");

/* Klasifikasi surat gaya Srikandi (kode -> nama) untuk tampilan tabel */
const NAMA_KLASIFIKASI = {
    UM: "Umum", PT: "Permintaan/Penugasan", SN: "Sikap/Tanggapan", KP: "Keputusan/Kondisi",
    UN: "Perjanjian/Undangan", SI: "Surat Istimewa", PG: "Pengumuman", PU: "Peraturan",
    KR: "Konsep/Risalah", RH: "Laporan", RT: "Nota Dinas/Berita Acara",
    ST: "Sambutan/Surat Tugas", PM: "Penunjukan/Pelantikan", PR: "Pemberitahuan",
    SS: "Statistik/Soal", DU: "Nota Dinas U", LD: "Buku/Pedoman",
};
const STATUS_MASUK = ["Belum Diproses", "Diproses", "Selesai"];
const STATUS_KELUAR = ["Terkirim", "Arsip"];

function showToast(msg, type = "primary") {
    const el = document.createElement("div");
    el.className = `toast align-items-center text-bg-${type} border-0`;
    el.setAttribute("role", "alert");
    el.innerHTML = `<div class="d-flex"><div class="toast-body">${esc(msg)}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
    $("#toast-area").appendChild(el);
    const t = new bootstrap.Toast(el, {delay: 2800});
    t.show();
    el.addEventListener("hidden.bs.toast", () => el.remove());
}

/* ---------- wrapper API + JWT ---------- */
async function api(path, options = {}) {
    const headers = Object.assign({"Content-Type": "application/json"}, options.headers || {});
    if (state.token) headers["Authorization"] = "Bearer " + state.token;
    const res = await fetch(path, {...options, headers});
    if (res.status === 401) {
        forceLogout();
        throw new Error("Sesi berakhir, silakan login kembali");
    }
    if (!res.ok) {
        let msg = "Terjadi kesalahan (" + res.status + ")";
        try {
            const body = await res.json();
            if (body && body.message) msg = body.message;
        } catch (_) { /* abaikan */ }
        throw new Error(msg);
    }
    if (res.status === 204) return null;
    return res.json();
}

/* ---------- login / logout ---------- */
$("#login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const err = $("#login-error");
    err.hidden = true;
    try {
        const res = await fetch(API.login, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                username: $("#username").value.trim(),
                password: $("#password").value,
            }),
        });
        const body = await res.json();
        if (!res.ok) throw new Error(body.message || "Login gagal");
        state.token = body.token;
        localStorage.setItem("jwt_token", state.token);
        await masukAplikasi();
    } catch (ex) {
        err.textContent = ex.message;
        err.hidden = false;
    }
});

async function masukAplikasi() {
    try {
        state.user = await api(API.me);
    } catch (_) {
        return;
    }
    $("#login-view").hidden = true;
    $("#app-view").hidden = false;
    $("#user-chip").textContent = state.user.namaLengkap;

    const admin = state.user.role === "ADMIN";
    document.querySelectorAll(".admin-only").forEach((el) => (el.style.display = admin ? "" : "none"));

    await muatProfilSekolah();
    await muatPesertaDidik();
    await muatKesiswaan();
    await muatSaranaPrasarana();
    await muatPegawai();
    await muatSemuaSurat();
    if (admin) await muatUsers();
    await muatNotif();
    $("#disposisi-saya-nama").textContent = state.user.namaLengkap;
    // perbarui notifikasi tiap 60 detik
    clearInterval(state._notifTimer);
    state._notifTimer = setInterval(muatNotif, 60000);
}

function forceLogout() {
    state.token = null;
    state.user = null;
    localStorage.removeItem("jwt_token");
    $("#app-view").hidden = true;
    $("#login-view").hidden = false;
    $("#password").value = "";
}

$("#btn-logout").addEventListener("click", async () => {
    try {
        if (state.token) await api(API.logout, {method: "POST"});
    } catch (_) { /* abaikan */ }
    forceLogout();
    showToast("Anda telah keluar", "secondary");
});

/* ---------- navigasi halaman (sidebar) ---------- */
function tutupSidebarMobile() {
    $("#sidebar").classList.remove("show");
    $("#sidebar-backdrop").classList.remove("show");
}

$("#btn-toggle-sidebar").addEventListener("click", () => {
    $("#sidebar").classList.toggle("show");
    $("#sidebar-backdrop").classList.toggle("show", $("#sidebar").classList.contains("show"));
});
$("#sidebar-backdrop").addEventListener("click", tutupSidebarMobile);

document.querySelectorAll(".page-link-nav").forEach((link) => {
    link.addEventListener("click", (e) => {
        e.preventDefault();
        document.querySelectorAll(".page-link-nav").forEach((l) => l.classList.remove("active"));
        link.classList.add("active");
        document.querySelectorAll(".page").forEach((p) => (p.hidden = true));
        $("#page-" + link.dataset.page).hidden = false;
        $("#topbar-title").textContent = link.textContent.trim();
        tutupSidebarMobile();
        if (link.dataset.page === "pengguna" && state.user?.role === "ADMIN") muatUsers();
        if (link.dataset.page === "profil-sekolah") muatProfilSekolah();
        if (link.dataset.page === "peserta-didik") renderPesertaDidik();
        if (link.dataset.page === "kesiswaan") renderKesiswaan();
        if (link.dataset.page === "sarana-prasarana") renderSarpras();
        if (link.dataset.page === "surat-masuk") muatSurat("MASUK");
        if (link.dataset.page === "surat-keluar") muatSurat("KELUAR");
        if (link.dataset.page === "rekap-surat") renderRekap();
        if (link.dataset.page === "disposisi-saya") { muatNotif(); renderDisposisiSaya(); }
    });
});

/* ---------- data pegawai ---------- */
async function muatPegawai() {
    state.pegawaiList = await api(API.pegawai);
    renderPegawai();
    renderDashboard();
}

function renderPegawai() {
    const filter = $("#cari").value.trim().toLowerCase();
    const data = state.pegawaiList.filter(
        (p) => !filter || (p.nama || "").toLowerCase().includes(filter)
    );
    const tbody = $("#tbody-pegawai");
    tbody.innerHTML = "";
    $("#pegawai-kosong").hidden = data.length > 0;
    $("#jumlah-pegawai").textContent = data.length + " pegawai";

    const admin = state.user?.role === "ADMIN";
    data.forEach((p, i) => {
        const tr = document.createElement("tr");
        const warnaBadge = {"Pegawai Tetap": "primary", "Pegawai Tidak Tetap": "warning text-dark"};
        tr.innerHTML = `
            <td>${i + 1}</td>
            <td><span class="fw-semibold">${esc(p.nama)}</span></td>
            <td>${esc(p.nip || "-")}</td>
            <td>${esc(p.jabatan)}</td>
            <td><span class="badge text-bg-${warnaBadge[p.statusKepegawaian] || "secondary"}">${esc(p.statusKepegawaian)}</span></td>
            <td>${esc(p.mataPelajaran || "-")}</td>
            <td>${esc(p.pendidikanTerakhir || "-")}</td>
            <td>${esc(p.telepon || "-")}</td>
            <td class="text-end">
                ${admin ? `
                <button class="btn btn-sm btn-outline-primary" data-aksi="edit" data-id="${p.id}" title="Ubah"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-outline-danger" data-aksi="hapus" data-id="${p.id}" title="Hapus"><i class="bi bi-trash"></i></button>`
                : `<span class="text-muted">—</span>`}
            </td>`;
        tbody.appendChild(tr);
    });

    tbody.querySelectorAll("button[data-aksi]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.id);
            if (btn.dataset.aksi === "edit") bukaModalEdit(id);
            else hapusPegawai(id);
        });
    });
}

$("#cari").addEventListener("input", renderPegawai);

async function hapusPegawai(id) {
    const p = state.pegawaiList.find((x) => x.id === id);
    if (!confirm("Hapus data pegawai \"" + (p?.nama || id) + "\"?")) return;
    try {
        await api(API.pegawai + "/" + id, {method: "DELETE"});
        showToast("Data pegawai dihapus", "success");
        await muatPegawai();
    } catch (e) {
        showToast(e.message, "danger");
    }
}

/* ---------- modal pegawai ---------- */
function bukaModalTambah() {
    state.editId = null;
    $("#modal-pegawai-title").textContent = "Tambah Pegawai";
    $("#form-pegawai").reset();
    modalPegawai().show();
}

function bukaModalEdit(id) {
    const p = state.pegawaiList.find((x) => x.id === id);
    if (!p) return;
    state.editId = id;
    $("#modal-pegawai-title").textContent = "Ubah Pegawai — " + p.nama;
    $("#f-nip").value = p.nip || "";
    $("#f-nama").value = p.nama || "";
    $("#f-jk").value = p.jenisKelamin || "";
    $("#f-status").value = p.statusKepegawaian || "";
    $("#f-jabatan").value = p.jabatan || "";
    $("#f-mapel").value = p.mataPelajaran || "";
    $("#f-pendidikan").value = p.pendidikanTerakhir || "";
    $("#f-tgl-lahir").value = p.tanggalLahir || "";
    $("#f-tgl-masuk").value = p.tanggalMasuk || "";
    $("#f-telepon").value = p.telepon || "";
    $("#f-email").value = p.email || "";
    $("#f-gaji").value = p.gajiPokok ?? "";
    $("#f-alamat").value = p.alamat || "";
    modalPegawai().show();
}

$("#form-pegawai").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        nip: $("#f-nip").value.trim() || null,
        nama: $("#f-nama").value.trim(),
        jenisKelamin: $("#f-jk").value || null,
        jabatan: $("#f-jabatan").value,
        mataPelajaran: $("#f-mapel").value.trim() || null,
        statusKepegawaian: $("#f-status").value,
        pendidikanTerakhir: $("#f-pendidikan").value || null,
        tanggalLahir: $("#f-tgl-lahir").value || null,
        tanggalMasuk: $("#f-tgl-masuk").value || null,
        telepon: $("#f-telepon").value.trim() || null,
        email: $("#f-email").value.trim() || null,
        gajiPokok: $("#f-gaji").value === "" ? null : Number($("#f-gaji").value),
        alamat: $("#f-alamat").value.trim() || null,
    };
    try {
        if (state.editId) {
            await api(API.pegawai + "/" + state.editId, {method: "PUT", body: JSON.stringify(payload)});
            showToast("Data pegawai diperbarui", "success");
        } else {
            await api(API.pegawai, {method: "POST", body: JSON.stringify(payload)});
            showToast("Pegawai baru ditambahkan", "success");
        }
        bootstrap.Modal.getInstance($("#modal-pegawai")).hide();
        await muatPegawai();
    } catch (ex) {
        showToast(ex.message, "danger");
    }
});

$("#btn-tambah").addEventListener("click", bukaModalTambah);

/* ---------- profil sekolah ---------- */
const numOrNull = (v) => (v === "" || v == null ? null : Number(v));
const strOrNull = (v) => {
    const s = String(v ?? "").trim();
    return s === "" ? null : s;
};

async function muatProfilSekolah() {
    try {
        const p = await api(API.profilSekolah);
        state.profilSekolah = p;
        isiFormProfil(p);
    } catch (e) {
        showToast("Gagal memuat profil sekolah: " + e.message, "danger");
    }
}

function isiFormProfil(p) {
    const set = (id, nilai) => { const el = $(id); if (el) el.value = nilai ?? ""; };
    set("#pf-nama-sekolah", p.namaSekolah);
    set("#pf-npsn", p.npsn);
    set("#pf-nss", p.nss);
    set("#pf-status-sekolah", p.statusSekolah || "Negeri");
    set("#pf-bentuk", p.bentukPendidikan || "SMP");
    set("#pf-akreditasi", p.statusAkreditasi || "");
    set("#pf-tahun-berdiri", p.tahunBerdiri);
    set("#pf-kurikulum", p.kurikulum || "");
    set("#pf-waktu-belajar", p.waktuBelajar || "");
    set("#pf-alamat", p.alamat);
    set("#pf-kelurahan", p.kelurahan);
    set("#pf-kecamatan", p.kecamatan);
    set("#pf-kota", p.kotaKabupaten);
    set("#pf-provinsi", p.provinsi);
    set("#pf-kode-pos", p.kodePos);
    set("#pf-telepon", p.telepon);
    set("#pf-email", p.email);
    set("#pf-website", p.website);
    set("#pf-kepsek", p.namaKepalaSekolah);
    set("#pf-nip-kepsek", p.nipKepalaSekolah);
    set("#pf-luas-tanah", p.luasTanah);
    set("#pf-luas-bangunan", p.luasBangunan);
    set("#pf-daya-listrik", p.dayaListrik);
    set("#pf-sumber-listrik", p.sumberListrik || "");
    set("#pf-internet", p.aksesInternet || "");
    set("#pf-kecepatan-internet", p.kecepatanInternet);
    set("#pf-visi", p.visi);
    set("#pf-misi", p.misi);

    // STAFF hanya melihat: kunci semua input & tombol simpan mengikuti admin-only
    const isAdmin = state.user?.role === "ADMIN";
    document.querySelectorAll("#form-profil input, #form-profil select, #form-profil textarea")
        .forEach((el) => (el.disabled = !isAdmin));
}

$("#form-profil").addEventListener("submit", async (e) => {
    e.preventDefault();
    if (state.user?.role !== "ADMIN") return;
    const payload = {
        namaSekolah: $("#pf-nama-sekolah").value.trim(),
        npsn: strOrNull($("#pf-npsn").value),
        nss: strOrNull($("#pf-nss").value),
        statusSekolah: $("#pf-status-sekolah").value,
        bentukPendidikan: $("#pf-bentuk").value,
        statusAkreditasi: strOrNull($("#pf-akreditasi").value),
        tahunBerdiri: numOrNull($("#pf-tahun-berdiri").value),
        kurikulum: strOrNull($("#pf-kurikulum").value),
        waktuBelajar: strOrNull($("#pf-waktu-belajar").value),
        alamat: strOrNull($("#pf-alamat").value),
        kelurahan: strOrNull($("#pf-kelurahan").value),
        kecamatan: strOrNull($("#pf-kecamatan").value),
        kotaKabupaten: strOrNull($("#pf-kota").value),
        provinsi: strOrNull($("#pf-provinsi").value),
        kodePos: strOrNull($("#pf-kode-pos").value),
        telepon: strOrNull($("#pf-telepon").value),
        email: strOrNull($("#pf-email").value),
        website: strOrNull($("#pf-website").value),
        namaKepalaSekolah: strOrNull($("#pf-kepsek").value),
        nipKepalaSekolah: strOrNull($("#pf-nip-kepsek").value),
        luasTanah: numOrNull($("#pf-luas-tanah").value),
        luasBangunan: numOrNull($("#pf-luas-bangunan").value),
        dayaListrik: numOrNull($("#pf-daya-listrik").value),
        sumberListrik: strOrNull($("#pf-sumber-listrik").value),
        aksesInternet: strOrNull($("#pf-internet").value),
        kecepatanInternet: strOrNull($("#pf-kecepatan-internet").value),
        visi: strOrNull($("#pf-visi").value),
        misi: strOrNull($("#pf-misi").value),
    };
    try {
        state.profilSekolah = await api(API.profilSekolah, {method: "PUT", body: JSON.stringify(payload)});
        isiFormProfil(state.profilSekolah);
        showToast("Profil sekolah tersimpan", "success");
    } catch (ex) {
        showToast(ex.message, "danger");
    }
});

/* ---------- peserta didik ---------- */
async function muatPesertaDidik() {
    try {
        state.pesertaDidikList = await api(API.pesertaDidik);
    } catch (e) {
        showToast("Gagal memuat peserta didik: " + e.message, "danger");
        state.pesertaDidikList = [];
    }
    renderPesertaDidik();
    renderDashboard();
}

function renderPesertaDidik() {
    const filter = $("#cari-siswa").value.trim().toLowerCase();
    const data = state.pesertaDidikList.filter(
        (p) => !filter || [p.nama, p.nisn, p.nis, p.kelas].some((v) => (v || "").toLowerCase().includes(filter))
    );
    const tbody = $("#tbody-siswa");
    tbody.innerHTML = "";
    $("#siswa-kosong").hidden = data.length > 0;
    $("#jumlah-siswa").textContent = data.length + " siswa";

    const admin = state.user?.role === "ADMIN";
    const warnaStatus = {"Aktif": "success", "Lulus": "primary", "Pindah": "warning text-dark", "Keluar": "danger", "Meninggal": "dark"};
    data.forEach((p, i) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${i + 1}</td>
            <td>${esc(p.nisn || "-")}<br><small class="text-muted">NIS: ${esc(p.nis || "-")}</small></td>
            <td><span class="fw-semibold">${esc(p.nama)}</span></td>
            <td>${p.jenisKelamin === "L" ? "L" : p.jenisKelamin === "P" ? "P" : "-"}</td>
            <td>${esc(p.kelas || "-")}</td>
            <td>${p.angkatan ?? "-"}</td>
            <td><span class="badge text-bg-${warnaStatus[p.status] || "secondary"}">${esc(p.status || "Aktif")}</span></td>
            <td class="text-end">
                ${admin ? `
                <button class="btn btn-sm btn-outline-primary" data-aksi="edit" data-id="${p.id}" title="Ubah"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-outline-danger" data-aksi="hapus" data-id="${p.id}" title="Hapus"><i class="bi bi-trash"></i></button>`
                : `<span class="text-muted">—</span>`}
            </td>`;
        tbody.appendChild(tr);
    });

    tbody.querySelectorAll("button[data-aksi]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.id);
            if (btn.dataset.aksi === "edit") bukaModalSiswa(id);
            else hapusSiswa(id);
        });
    });
}

$("#cari-siswa").addEventListener("input", renderPesertaDidik);

async function hapusSiswa(id) {
    const p = state.pesertaDidikList.find((x) => x.id === id);
    if (!confirm("Hapus peserta didik \"" + (p?.nama || id) + "\"?")) return;
    try {
        await api(API.pesertaDidik + "/" + id, {method: "DELETE"});
        showToast("Data peserta didik dihapus", "success");
        await muatPesertaDidik();
    } catch (e) {
        showToast(e.message, "danger");
    }
}

function isiDatalistSiswa() {
    $("#daftar-siswa-list").innerHTML = state.pesertaDidikList
        .map((p) => `<option value="${esc(p.nama)}"></option>`).join("");
}

function bukaModalSiswa(id) {
    state.editSiswaId = id || null;
    const form = $("#form-siswa");
    form.reset();
    const p = id ? state.pesertaDidikList.find((x) => x.id === id) : null;
    $("#modal-siswa-title").textContent = p ? "Ubah Peserta Didik — " + p.nama : "Tambah Peserta Didik";
    if (p) {
        $("#s-nisn").value = p.nisn || "";
        $("#s-nis").value = p.nis || "";
        $("#s-nama").value = p.nama || "";
        $("#s-jk").value = p.jenisKelamin || "";
        $("#s-agama").value = p.agama || "";
        $("#s-tempat-lahir").value = p.tempatLahir || "";
        $("#s-tanggal-lahir").value = p.tanggalLahir || "";
        $("#s-alamat").value = p.alamat || "";
        $("#s-kelurahan").value = p.kelurahan || "";
        $("#s-kecamatan").value = p.kecamatan || "";
        $("#s-kota").value = p.kotaKabupaten || "";
        $("#s-hp").value = p.nomorHp || "";
        $("#s-nama-ayah").value = p.namaAyah || "";
        $("#s-nama-ibu").value = p.namaIbu || "";
        $("#s-pekerjaan-ayah").value = p.pekerjaanAyah || "";
        $("#s-pekerjaan-ibu").value = p.pekerjaanIbu || "";
        $("#s-penghasilan").value = p.penghasilanOrangTua || "";
        $("#s-hp-ortu").value = p.nomorHpOrangTua || "";
        $("#s-kelas").value = p.kelas || "";
        $("#s-angkatan").value = p.angkatan ?? "";
        $("#s-status").value = p.status || "Aktif";
        $("#s-tanggal-masuk").value = p.tanggalMasuk || "";
        $("#s-catatan").value = p.catatan || "";
    } else {
        $("#s-status").value = "Aktif";
        $("#s-tanggal-masuk").value = new Date().toISOString().slice(0, 10);
    }
    modalSiswa().show();
}

$("#btn-tambah-siswa").addEventListener("click", () => bukaModalSiswa(null));

$("#form-siswa").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        nisn: strOrNull($("#s-nisn").value),
        nis: strOrNull($("#s-nis").value),
        nama: $("#s-nama").value.trim(),
        jenisKelamin: strOrNull($("#s-jk").value),
        agama: strOrNull($("#s-agama").value),
        tempatLahir: strOrNull($("#s-tempat-lahir").value),
        tanggalLahir: strOrNull($("#s-tanggal-lahir").value),
        alamat: strOrNull($("#s-alamat").value),
        kelurahan: strOrNull($("#s-kelurahan").value),
        kecamatan: strOrNull($("#s-kecamatan").value),
        kotaKabupaten: strOrNull($("#s-kota").value),
        nomorHp: strOrNull($("#s-hp").value),
        namaAyah: strOrNull($("#s-nama-ayah").value),
        namaIbu: strOrNull($("#s-nama-ibu").value),
        pekerjaanAyah: strOrNull($("#s-pekerjaan-ayah").value),
        pekerjaanIbu: strOrNull($("#s-pekerjaan-ibu").value),
        penghasilanOrangTua: strOrNull($("#s-penghasilan").value),
        nomorHpOrangTua: strOrNull($("#s-hp-ortu").value),
        kelas: strOrNull($("#s-kelas").value),
        angkatan: numOrNull($("#s-angkatan").value),
        status: $("#s-status").value,
        tanggalMasuk: strOrNull($("#s-tanggal-masuk").value),
        catatan: strOrNull($("#s-catatan").value),
    };
    try {
        if (state.editSiswaId) {
            await api(API.pesertaDidik + "/" + state.editSiswaId, {method: "PUT", body: JSON.stringify(payload)});
            showToast("Data peserta didik diperbarui", "success");
        } else {
            await api(API.pesertaDidik, {method: "POST", body: JSON.stringify(payload)});
            showToast("Peserta didik baru ditambahkan", "success");
        }
        bootstrap.Modal.getInstance($("#modal-siswa")).hide();
        await muatPesertaDidik();
    } catch (ex) {
        showToast(ex.message, "danger");
    }
});

/* ---------- kesiswaan ---------- */
async function muatKesiswaan() {
    try {
        state.kesiswaanList = await api(API.kesiswaan);
    } catch (e) {
        showToast("Gagal memuat data kesiswaan: " + e.message, "danger");
        state.kesiswaanList = [];
    }
    renderKesiswaan();
    renderDashboard();
}

function renderKesiswaan() {
    const filter = $("#cari-kesiswaan").value.trim().toLowerCase();
    const data = state.kesiswaanList.filter(
        (k) => !filter || [k.namaSiswa, k.judul, k.kelas, k.jenis].some((v) => (v || "").toLowerCase().includes(filter))
    );
    const tbody = $("#tbody-kesiswaan");
    tbody.innerHTML = "";
    $("#kesiswaan-kosong").hidden = data.length > 0;
    $("#jumlah-kesiswaan").textContent = data.length + " catatan";

    const admin = state.user?.role === "ADMIN";
    const warnaJenis = {
        "Prestasi": "success", "Pelanggaran": "danger", "Ekstrakurikuler": "info text-dark",
        "Beasiswa": "primary", "Lainnya": "secondary",
    };
    data.forEach((k, i) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${i + 1}</td>
            <td>${tglIndo(k.tanggal)}</td>
            <td><span class="badge text-bg-${warnaJenis[k.jenis] || "secondary"}">${esc(k.jenis)}</span></td>
            <td class="fw-semibold">${esc(k.namaSiswa)}</td>
            <td>${esc(k.kelas || "-")}</td>
            <td>${esc(k.judul)}${k.poin != null ? `<br><small class="text-muted">Poin: ${k.poin}</small>` : ""}</td>
            <td>${esc(k.tingkat || "-")}</td>
            <td class="text-end">
                ${admin ? `
                <button class="btn btn-sm btn-outline-primary" data-aksi="edit" data-id="${k.id}" title="Ubah"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-outline-danger" data-aksi="hapus" data-id="${k.id}" title="Hapus"><i class="bi bi-trash"></i></button>`
                : `<span class="text-muted">—</span>`}
            </td>`;
        tbody.appendChild(tr);
    });

    tbody.querySelectorAll("button[data-aksi]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.id);
            if (btn.dataset.aksi === "edit") bukaModalKesiswaan(id);
            else hapusKesiswaan(id);
        });
    });
}

$("#cari-kesiswaan").addEventListener("input", renderKesiswaan);

async function hapusKesiswaan(id) {
    const k = state.kesiswaanList.find((x) => x.id === id);
    if (!confirm("Hapus catatan kesiswaan \"" + (k?.judul || id) + "\"?")) return;
    try {
        await api(API.kesiswaan + "/" + id, {method: "DELETE"});
        showToast("Catatan kesiswaan dihapus", "success");
        await muatKesiswaan();
    } catch (e) {
        showToast(e.message, "danger");
    }
}

function bukaModalKesiswaan(id) {
    state.editKesiswaanId = id || null;
    const form = $("#form-kesiswaan");
    form.reset();
    const k = id ? state.kesiswaanList.find((x) => x.id === id) : null;
    $("#modal-kesiswaan-title").textContent = k ? "Ubah Catatan — " + k.judul : "Tambah Catatan Kesiswaan";
    isiDatalistSiswa();
    if (k) {
        $("#k-jenis").value = k.jenis || "";
        $("#k-nama-siswa").value = k.namaSiswa || "";
        $("#k-kelas").value = k.kelas || "";
        $("#k-judul").value = k.judul || "";
        $("#k-tingkat").value = k.tingkat || "";
        $("#k-tanggal").value = k.tanggal || "";
        $("#k-poin").value = k.poin ?? "";
        $("#k-keterangan").value = k.keterangan || "";
    } else {
        $("#k-tanggal").value = new Date().toISOString().slice(0, 10);
    }
    modalKesiswaan().show();
}

$("#btn-tambah-kesiswaan").addEventListener("click", () => bukaModalKesiswaan(null));

$("#form-kesiswaan").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        jenis: $("#k-jenis").value,
        namaSiswa: $("#k-nama-siswa").value.trim(),
        kelas: strOrNull($("#k-kelas").value),
        judul: $("#k-judul").value.trim(),
        tingkat: strOrNull($("#k-tingkat").value),
        tanggal: strOrNull($("#k-tanggal").value),
        poin: numOrNull($("#k-poin").value),
        keterangan: strOrNull($("#k-keterangan").value),
    };
    try {
        if (state.editKesiswaanId) {
            await api(API.kesiswaan + "/" + state.editKesiswaanId, {method: "PUT", body: JSON.stringify(payload)});
            showToast("Catatan kesiswaan diperbarui", "success");
        } else {
            await api(API.kesiswaan, {method: "POST", body: JSON.stringify(payload)});
            showToast("Catatan kesiswaan ditambahkan", "success");
        }
        bootstrap.Modal.getInstance($("#modal-kesiswaan")).hide();
        await muatKesiswaan();
    } catch (ex) {
        showToast(ex.message, "danger");
    }
});

/* ---------- sarana prasarana ---------- */
async function muatSaranaPrasarana() {
    try {
        state.saranaList = await api(API.saranaPrasarana);
    } catch (e) {
        showToast("Gagal memuat sarana prasarana: " + e.message, "danger");
        state.saranaList = [];
    }
    renderSarpras();
    renderDashboard();
}

function renderSarpras() {
    const filter = $("#cari-sarpras").value.trim().toLowerCase();
    const kategori = $("#filter-kategori-sarpras").value;
    const data = state.saranaList.filter((s) =>
        (!kategori || s.kategori === kategori) &&
        (!filter || [s.nama, s.kode, s.kategori].some((v) => (v || "").toLowerCase().includes(filter)))
    );
    const tbody = $("#tbody-sarpras");
    tbody.innerHTML = "";
    $("#sarpras-kosong").hidden = data.length > 0;
    $("#jumlah-sarpras").textContent = data.length + " item";

    const admin = state.user?.role === "ADMIN";
    const warnaKondisi = {"Baik": "success", "Rusak Ringan": "warning text-dark", "Rusak Sedang": "warning text-dark", "Rusak Berat": "danger"};
    data.forEach((s, i) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${i + 1}</td>
            <td><span class="badge text-bg-info text-dark">${esc(s.kategori)}</span></td>
            <td><span class="fw-semibold">${esc(s.nama)}</span>${s.keterangan ? `<br><small class="text-muted">${esc(s.keterangan)}</small>` : ""}</td>
            <td>${esc(s.kode || "-")}</td>
            <td>${s.jumlah ?? "-"}</td>
            <td><span class="badge text-bg-${warnaKondisi[s.kondisi] || "secondary"}">${esc(s.kondisi)}</span></td>
            <td>${s.tahunPengadaan ?? "-"}</td>
            <td class="text-end">
                ${admin ? `
                <button class="btn btn-sm btn-outline-primary" data-aksi="edit" data-id="${s.id}" title="Ubah"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-outline-danger" data-aksi="hapus" data-id="${s.id}" title="Hapus"><i class="bi bi-trash"></i></button>`
                : `<span class="text-muted">—</span>`}
            </td>`;
        tbody.appendChild(tr);
    });

    tbody.querySelectorAll("button[data-aksi]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.id);
            if (btn.dataset.aksi === "edit") bukaModalSarpras(id);
            else hapusSarpras(id);
        });
    });
}

$("#cari-sarpras").addEventListener("input", renderSarpras);
$("#filter-kategori-sarpras").addEventListener("change", renderSarpras);

async function hapusSarpras(id) {
    const s = state.saranaList.find((x) => x.id === id);
    if (!confirm("Hapus sarana prasarana \"" + (s?.nama || id) + "\"?")) return;
    try {
        await api(API.saranaPrasarana + "/" + id, {method: "DELETE"});
        showToast("Sarana prasarana dihapus", "success");
        await muatSaranaPrasarana();
    } catch (e) {
        showToast(e.message, "danger");
    }
}

function bukaModalSarpras(id) {
    state.editSarprasId = id || null;
    const form = $("#form-sarpras");
    form.reset();
    const s = id ? state.saranaList.find((x) => x.id === id) : null;
    $("#modal-sarpras-title").textContent = s ? "Ubah Sarpras — " + s.nama : "Tambah Sarana Prasarana";
    if (s) {
        $("#sp-kategori").value = s.kategori || "";
        $("#sp-nama").value = s.nama || "";
        $("#sp-kode").value = s.kode || "";
        $("#sp-jumlah").value = s.jumlah ?? "";
        $("#sp-kondisi").value = s.kondisi || "Baik";
        $("#sp-luas").value = s.luas ?? "";
        $("#sp-tahun").value = s.tahunPengadaan ?? "";
        $("#sp-sumber-dana").value = s.sumberDana || "";
        $("#sp-keterangan").value = s.keterangan || "";
    } else {
        $("#sp-kondisi").value = "Baik";
    }
    modalSarpras().show();
}

$("#btn-tambah-sarpras").addEventListener("click", () => bukaModalSarpras(null));

$("#form-sarpras").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        kategori: $("#sp-kategori").value,
        nama: $("#sp-nama").value.trim(),
        kode: strOrNull($("#sp-kode").value),
        jumlah: numOrNull($("#sp-jumlah").value),
        kondisi: $("#sp-kondisi").value,
        luas: numOrNull($("#sp-luas").value),
        tahunPengadaan: numOrNull($("#sp-tahun").value),
        sumberDana: strOrNull($("#sp-sumber-dana").value),
        keterangan: strOrNull($("#sp-keterangan").value),
    };
    try {
        if (state.editSarprasId) {
            await api(API.saranaPrasarana + "/" + state.editSarprasId, {method: "PUT", body: JSON.stringify(payload)});
            showToast("Sarana prasarana diperbarui", "success");
        } else {
            await api(API.saranaPrasarana, {method: "POST", body: JSON.stringify(payload)});
            showToast("Sarana prasarana ditambahkan", "success");
        }
        bootstrap.Modal.getInstance($("#modal-sarpras")).hide();
        await muatSaranaPrasarana();
    } catch (ex) {
        showToast(ex.message, "danger");
    }
});

/* ---------- persuratan (surat masuk & keluar) ---------- */
async function muatSemuaSurat() {
    try {
        const [sm, sk] = await Promise.all([
            api(API.surat + "?tipe=MASUK"),
            api(API.surat + "?tipe=KELUAR"),
        ]);
        state.surat.MASUK = sm;
        state.surat.KELUAR = sk;
        renderDashboard(); // refresh kartu statistik surat
    } catch (e) {
        showToast("Gagal memuat arsip surat: " + e.message, "danger");
    }
}

async function muatSurat(tipe) {
    try {
        state.surat[tipe] = await api(API.surat + "?tipe=" + tipe);
        renderSurat(tipe);
    } catch (e) {
        showToast(e.message, "danger");
    }
}

function renderSurat(tipe) {
    const short = tipe === "MASUK" ? "sm" : "sk";
    const admin = state.user?.role === "ADMIN";
    const filter = $("#cari-" + short).value.trim().toLowerCase();
    const data = state.surat[tipe].filter((s) => !filter || [
        s.nomorAgenda, s.nomorSurat, s.pengirim, s.tujuan, s.perihal, s.klasifikasi,
    ].some((v) => (v || "").toLowerCase().includes(filter)));

    const tbody = $("#tbody-" + short);
    tbody.innerHTML = "";
    $("#" + short + "-kosong").hidden = data.length > 0;
    $("#jumlah-" + short).textContent = data.length + " surat";

    data.forEach((s, i) => {
        const tr = document.createElement("tr");
        const warnaStatus = {"Belum Diproses": "danger", "Diproses": "warning text-dark", "Selesai": "success", "Terkirim": "success", "Arsip": "secondary"};
        const warnaSifat = {"Penting": "warning text-dark", "Segera": "danger", "Rahasia": "dark"};
        tr.innerHTML = `
            <td><span class="badge text-bg-primary">${esc(s.nomorAgenda || "-")}</span></td>
            <td>${esc(s.nomorSurat)}</td>
            <td>${tglIndo(s.tanggalSurat)}</td>
            <td>${esc(tipe === "MASUK" ? s.pengirim : s.tujuan) || "-"}</td>
            <td><span class="badge text-bg-info text-dark" title="${esc(NAMA_KLASIFIKASI[s.klasifikasi] || "")}">${esc(s.klasifikasi)}</span></td>
            <td class="surat-perihal">${esc(s.perihal)}</td>
            <td><span class="badge text-bg-${warnaSifat[s.sifatSurat] || "secondary"}">${esc(s.sifatSurat || "Biasa")}</span></td>
            <td><span class="badge text-bg-${warnaStatus[s.statusSurat] || "secondary"}">${esc(s.statusSurat || "-")}</span></td>
            <td class="text-end">
                ${s.lampiranNama ? `<button class="btn btn-sm btn-outline-success me-1" data-aksi="lampiran" data-id="${s.id}" title="Unduh lampiran: ${esc(s.lampiranNama)}"><i class="bi bi-paperclip"></i></button>` : ""}
                ${tipe === "MASUK" ? `<button class="btn btn-sm btn-outline-info me-1" data-aksi="disposisi" data-id="${s.id}" title="Disposisi"><i class="bi bi-send-arrow-down"></i></button>` : ""}
                ${admin ? `
                <button class="btn btn-sm btn-outline-primary me-1" data-aksi="edit" data-id="${s.id}" title="Ubah"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-outline-danger" data-aksi="hapus" data-id="${s.id}" title="Hapus"><i class="bi bi-trash"></i></button>`
                : `<span class="text-muted">—</span>`}
            </td>`;
        tbody.appendChild(tr);
    });

    tbody.querySelectorAll("button[data-aksi]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = Number(btn.dataset.id);
            if (btn.dataset.aksi === "edit") bukaModalSurat(tipe, id);
            else if (btn.dataset.aksi === "hapus") hapusSurat(tipe, id);
            else if (btn.dataset.aksi === "disposisi") bukaModalDisposisi(id);
            else unduhLampiran(id);
        });
    });
}

$("#cari-sm").addEventListener("input", () => renderSurat("MASUK"));
$("#cari-sk").addEventListener("input", () => renderSurat("KELUAR"));

function tglIndo(t) {
    if (!t) return "-";
    const d = new Date(t + "T00:00:00");
    return d.toLocaleDateString("id-ID", {day: "2-digit", month: "short", year: "numeric"});
}

async function hapusSurat(tipe, id) {
    const s = state.surat[tipe].find((x) => x.id === id);
    if (!confirm("Hapus arsip surat \"" + (s?.nomorAgenda || id) + "\"?")) return;
    try {
        await api(API.surat + "/" + id, {method: "DELETE"});
        showToast("Arsip surat dihapus", "success");
        await muatSurat(tipe);
        renderDashboard();
    } catch (e) {
        showToast(e.message, "danger");
    }
}

/* ---------- modal surat ---------- */
function isiPilihanStatus(tipe, terpilih) {
    const sel = $("#m-status");
    const opsi = tipe === "MASUK" ? STATUS_MASUK : STATUS_KELUAR;
    sel.innerHTML = opsi.map((o) => `<option${o === terpilih ? " selected" : ""}>${o}</option>`).join("");
}

function bukaModalSurat(tipe, id) {
    state.editSuratId = id || null;
    const form = $("#form-surat");
    form.reset();
    $("#m-lampiran-info").innerHTML = "";

    if (id) {
        const s = state.surat[tipe].find((x) => x.id === id);
        if (!s) return;
        $("#modal-surat-title").textContent = "Ubah Surat — " + (s.nomorAgenda || s.nomorSurat);
        $("#m-nomor").value = s.nomorSurat || "";
        $("#m-klasifikasi").value = s.klasifikasi || "";
        $("#m-tanggal").value = s.tanggalSurat || "";
        $("#m-pihak").value = (tipe === "MASUK" ? s.pengirim : s.tujuan) || "";
        $("#m-perihal").value = s.perihal || "";
        $("#m-sifat").value = s.sifatSurat || "Biasa";
        isiPilihanStatus(tipe, s.statusSurat);
        $("#m-catatan").value = s.catatan || "";
        if (s.lampiranNama) {
            $("#m-lampiran-info").innerHTML =
                `<i class="bi bi-paperclip me-1"></i>Lampiran saat ini: <b>${esc(s.lampiranNama)}</b> — unggah file baru untuk mengganti.`;
        }
    } else {
        $("#modal-surat-title").textContent = tipe === "MASUK" ? "Catat Surat Masuk" : "Catat Surat Keluar";
        $("#m-tanggal").value = new Date().toISOString().slice(0, 10);
        isiPilihanStatus(tipe, null);
    }

    // label kolom pihak sesuai tipe surat
    $("#label-pihak").innerHTML = (tipe === "MASUK" ? "Pengirim" : "Tujuan") + ' <span class="text-danger">*</span>';
    $("#m-pihak").placeholder = tipe === "MASUK"
        ? "mis. Dinas Pendidikan Kota Banjarbaru"
        : "mis. Orang Tua/Wali, Dinas, dsb.";
    $("#modal-surat").dataset.tipe = tipe;
    modalSurat().show();
}

$("#btn-tambah-sm").addEventListener("click", () => bukaModalSurat("MASUK"));
$("#btn-tambah-sk").addEventListener("click", () => bukaModalSurat("KELUAR"));

$("#form-surat").addEventListener("submit", async (e) => {
    e.preventDefault();
    const tipe = $("#modal-surat").dataset.tipe;
    const file = $("#m-lampiran").files[0];

    const fd = new FormData();
    if (!state.editSuratId) fd.append("tipe", tipe);
    fd.append("nomorSurat", $("#m-nomor").value.trim());
    fd.append("klasifikasi", $("#m-klasifikasi").value);
    fd.append("tanggalSurat", $("#m-tanggal").value);
    if (tipe === "MASUK") fd.append("pengirim", $("#m-pihak").value.trim());
    else fd.append("tujuan", $("#m-pihak").value.trim());
    fd.append("perihal", $("#m-perihal").value.trim());
    fd.append("sifatSurat", $("#m-sifat").value);
    fd.append("statusSurat", $("#m-status").value);
    fd.append("catatan", $("#m-catatan").value.trim());
    if (file) fd.append("lampiran", file);

    const btn = e.target.querySelector('button[type="submit"]');
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Menyimpan...`;
    try {
        const url = state.editSuratId ? API.surat + "/" + state.editSuratId : API.surat;
        await fetch(url, {
            method: state.editSuratId ? "PUT" : "POST",
            headers: {"Authorization": "Bearer " + state.token},
            body: fd,
        }).then(async (res) => {
            if (!res.ok) {
                let msg = "Gagal menyimpan surat";
                try { msg = (await res.json()).message || msg; } catch (_) {}
                throw new Error(msg);
            }
        });
        showToast(state.editSuratId ? "Arsip surat diperbarui" : "Surat berhasil dicatat", "success");
        bootstrap.Modal.getInstance($("#modal-surat")).hide();
        await muatSurat(tipe);
        renderDashboard();
    } catch (ex) {
        showToast(ex.message, "danger");
    } finally {
        btn.disabled = false;
        btn.innerHTML = `<i class="bi bi-check-lg me-1"></i>Simpan`;
    }
});

async function unduhLampiran(id) {
    try {
        const res = await fetch(API.surat + "/" + id + "/lampiran", {headers: {"Authorization": "Bearer " + state.token}});
        if (!res.ok) throw new Error("Lampiran tidak ditemukan");
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = res.headers.get("Content-Disposition")?.match(/filename="(.+)"/)?.[1] || "lampiran-surat";
        a.click();
        URL.revokeObjectURL(url);
    } catch (e) {
        showToast(e.message, "danger");
    }
}

/* ---------- disposisi surat masuk ---------- */
const modalDisposisi = () => new bootstrap.Modal("#modal-disposisi");
let disposisiSuratId = null;

function bukaModalDisposisi(id) {
    disposisiSuratId = id;
    const s = state.surat.MASUK.find((x) => x.id === id);
    if (!s) return;
    $("#disposisi-surat-info").innerHTML =
        `<b>No. Agenda:</b> ${esc(s.nomorAgenda || "-")} &nbsp;|&nbsp; <b>Dari:</b> ${esc(s.pengirim || "-")}<br>` +
        `<b>Perihal:</b> ${esc(s.perihal)}`;
    $("#d-tanggal").value = new Date().toISOString().slice(0, 10);
    $("#d-batas").value = "";
    $("#d-instruksi").value = "";
    $("#d-kepada").value = "";
    $("#d-status").value = "Diproses";

    // datalist nama pegawai untuk mempermudah pengisian
    $("#daftar-pegawai-list").innerHTML = state.pegawaiList
        .map((p) => `<option value="${esc(p.nama)}"></option>`).join("");

    muatDisposisi(id);
    modalDisposisi().show();
}

async function muatDisposisi(id) {
    const box = $("#daftar-disposisi");
    box.innerHTML = `<div class="text-muted small"><i class="bi bi-hourglass-split me-1"></i>Memuat...</div>`;
    try {
        const list = await api(API.surat + "/" + id + "/disposisi");
        if (!list.length) {
            box.innerHTML = `<div class="text-muted small py-2"><i class="bi bi-inbox me-1"></i>Belum ada disposisi untuk surat ini.</div>`;
            return;
        }
        const admin = state.user?.role === "ADMIN";
        const warnaStatus = {"Diproses": "warning text-dark", "Selesai": "success"};
        box.innerHTML = list.map((d) => `
            <div class="border rounded p-2 mb-2 bg-light">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <span class="fw-semibold"><i class="bi bi-person-check me-1 text-primary"></i>${esc(d.diteruskanKepada)}</span>
                        <span class="badge text-bg-${warnaStatus[d.status] || "secondary"} ms-2">${esc(d.status || "Diproses")}</span>
                        <div class="small text-muted">Tanggal: ${tglIndo(d.tanggalDisposisi)}${d.batasTanggal ? " &nbsp;|&nbsp; Batas: " + tglIndo(d.batasTanggal) : ""}</div>
                        ${d.instruksi ? `<div class="small mt-1"><i class="bi bi-chat-left-text me-1"></i>${esc(d.instruksi)}</div>` : ""}
                    </div>
                    ${admin ? `<button class="btn btn-sm btn-outline-danger" data-disid="${d.id}" title="Hapus"><i class="bi bi-trash"></i></button>` : ""}
                </div>
            </div>`).join("");

        box.querySelectorAll("button[data-disid]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                if (!confirm("Hapus disposisi ini?")) return;
                try {
                    await api(API.surat + "/" + id + "/disposisi/" + btn.dataset.disid, {method: "DELETE"});
                    showToast("Disposisi dihapus", "success");
                    muatDisposisi(id);
                } catch (e) {
                    showToast(e.message, "danger");
                }
            });
        });
    } catch (e) {
        box.innerHTML = `<div class="text-danger small">${esc(e.message)}</div>`;
    }
}

$("#form-disposisi").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData();
    fd.append("tanggalDisposisi", $("#d-tanggal").value);
    fd.append("diteruskanKepada", $("#d-kepada").value.trim());
    fd.append("instruksi", $("#d-instruksi").value.trim());
    fd.append("batasTanggal", $("#d-batas").value);
    fd.append("status", $("#d-status").value);
    try {
        const res = await fetch(API.surat + "/" + disposisiSuratId + "/disposisi", {
            method: "POST",
            headers: {"Authorization": "Bearer " + state.token},
            body: fd,
        });
        if (!res.ok) {
            let msg = "Gagal menyimpan disposisi";
            try { msg = (await res.json()).message || msg; } catch (_) {}
            throw new Error(msg);
        }
        showToast("Disposisi tersimpan", "success");
        $("#d-kepada").value = "";
        $("#d-instruksi").value = "";
        $("#d-batas").value = "";
        $("#d-status").value = "Diproses";
        muatDisposisi(disposisiSuratId);
    } catch (ex) {
        showToast(ex.message, "danger");
    }
});

/* ---------- export excel & cetak daftar surat ---------- */
async function unduhLaporan(tipe) {
    try {
        const short = tipe === "MASUK" ? "sm" : "sk";
        const url = API.surat + "/export?tipe=" + tipe + "&cari=" + encodeURIComponent($("#cari-" + short).value.trim());
        const res = await fetch(url, {headers: {"Authorization": "Bearer " + state.token}});
        if (!res.ok) throw new Error("Gagal membuat laporan Excel");
        const blob = await res.blob();
        const a = document.createElement("a");
        a.href = URL.createObjectURL(blob);
        a.download = "buku-agenda-surat-" + tipe.toLowerCase() + ".xlsx";
        a.click();
        URL.revokeObjectURL(a.href);
        showToast("Laporan Excel diunduh", "success");
    } catch (e) {
        showToast(e.message, "danger");
    }
}

$("#btn-export-sm").addEventListener("click", () => unduhLaporan("MASUK"));
$("#btn-export-sk").addEventListener("click", () => unduhLaporan("KELUAR"));

function cetakDaftarSurat(tipe) {
    const short = tipe === "MASUK" ? "sm" : "sk";
    const judul = tipe === "MASUK" ? "BUKU AGENDA SURAT MASUK" : "BUKU AGENDA SURAT KELUAR";
    const filter = $("#cari-" + short).value.trim().toLowerCase();
    const data = state.surat[tipe].filter((s) => !filter || [
        s.nomorAgenda, s.nomorSurat, s.pengirim, s.tujuan, s.perihal, s.klasifikasi,
    ].some((v) => (v || "").toLowerCase().includes(filter)));

    const w = window.open("", "_blank", "width=1000,height=700");
    if (!w) { showToast("Popup diblokir browser — izinkan popup untuk mencetak", "warning"); return; }
    const baris = data.length
        ? data.map((s, i) => `<tr>
            <td class="c">${i + 1}</td>
            <td>${esc(s.nomorAgenda || "-")}</td>
            <td>${esc(s.nomorSurat)}</td>
            <td>${tglIndo(s.tanggalSurat)}</td>
            <td>${esc(tipe === "MASUK" ? s.pengirim : s.tujuan) || "-"}</td>
            <td>${esc(s.klasifikasi)}</td>
            <td>${esc(s.perihal)}</td>
            <td>${esc(s.sifatSurat || "Biasa")}</td>
            <td>${esc(s.statusSurat || "-")}</td></tr>`).join("")
        : `<tr><td colspan="9" class="c text-muted">Belum ada data surat.</td></tr>`;

    w.document.write(`<!DOCTYPE html><html lang="id"><head><meta charset="utf-8"><title>${judul}</title>
    <style>
        body { font-family: "Times New Roman", serif; margin: 24px; }
        .kop { display: flex; align-items: center; gap: 14px; border-bottom: 3px double #000; padding-bottom: 10px; margin-bottom: 16px; }
        .kop img { width: 78px; height: 78px; object-fit: contain; }
        .kop .t { flex: 1; text-align: center; line-height: 1.3; }
        .kop h1 { font-size: 20px; margin: 0; }
        .kop p { margin: 2px 0 0; font-size: 12px; }
        h2 { text-align: center; font-size: 15px; text-decoration: underline; margin: 4px 0 14px; }
        table { width: 100%; border-collapse: collapse; font-size: 12px; }
        th, td { border: 1px solid #333; padding: 5px 7px; vertical-align: top; }
        th { background: #e8eefc; text-align: center; }
        .c { text-align: center; }
        .ttd { margin-top: 28px; float: right; width: 300px; text-align: center; font-size: 13px; }
        .ttd .sp { height: 64px; }
        @media print { @page { size: A4 landscape; margin: 12mm; } }
    </style></head><body>
    <div class="kop">
        <img src="${location.origin}/image/logoPendidikan.png" alt="logo">
        <div class="t"><h1>SMP UNGGULAN IDAMAN BANJARBARU</h1>
        <p>Alamat: Kota Banjarbaru, Kalimantan Selatan</p></div>
    </div>
    <h2>${judul}</h2>
    <table>
        <thead><tr><th>No</th><th>Nomor Agenda</th><th>Nomor Surat</th><th>Tanggal</th>
        <th>${tipe === "MASUK" ? "Pengirim" : "Tujuan"}</th><th>Klasifikasi</th><th>Perihal</th>
        <th>Sifat</th><th>Status</th></tr></thead>
        <tbody>${baris}</tbody>
    </table>
    <div class="ttd">
        <p>Banjarbaru, ${new Date().toLocaleDateString("id-ID", {day: "2-digit", month: "long", year: "numeric"})}</p>
        <p>Kepala SMP Unggulan Idaman Banjarbaru</p>
        <div class="sp"></div>
        <p>( ____________________________ )</p>
    </div>
    </body></html>`);
    w.document.close();
    w.focus();
    setTimeout(() => w.print(), 400); // tunggu logo termuat
}

$("#btn-cetak-sm").addEventListener("click", () => cetakDaftarSurat("MASUK"));
$("#btn-cetak-sk").addEventListener("click", () => cetakDaftarSurat("KELUAR"));

/* ---------- rekap persuratan ---------- */
const BULAN_ID = ["Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"];

function renderRekap() {
    // pilihan tahun dari data yang ada
    const tahunSet = new Set();
    ["MASUK", "KELUAR"].forEach((t) => state.surat[t].forEach((s) => tahunSet.add(s.tanggalSurat?.slice(0, 4))));
    tahunSet.add(String(new Date().getFullYear()));
    const tahunTersedia = [...tahunSet].filter(Boolean).sort().reverse();
    const sel = $("#rekap-tahun");
    const terpilih = sel.value && tahunTersedia.includes(sel.value) ? sel.value : tahunTersedia[0];
    sel.innerHTML = tahunTersedia.map((y) => `<option${y === terpilih ? " selected" : ""}>${y}</option>`).join("");
    const tahun = sel.value;

    const sm = state.surat.MASUK.filter((s) => s.tanggalSurat?.startsWith(tahun));
    const sk = state.surat.KELUAR.filter((s) => s.tanggalSurat?.startsWith(tahun));

    $("#rekap-stats").innerHTML = `
        <div class="col-4"><div class="stat-card stat-primary p-3 text-center">
            <div class="num">${sm.length + sk.length}</div><div class="lbl">Total Surat ${tahun}</div></div></div>
        <div class="col-4"><div class="stat-card stat-secondary p-3 text-center">
            <div class="num">${sm.length}</div><div class="lbl">Surat Masuk</div></div></div>
        <div class="col-4"><div class="stat-card stat-danger p-3 text-center">
            <div class="num">${sk.length}</div><div class="lbl">Surat Keluar</div></div></div>`;

    const perKlas = (list) => {
        const map = {};
        list.forEach((s) => { const k = s.klasifikasi || "(tanpa kode)"; map[k] = (map[k] || 0) + 1; });
        return Object.entries(map).sort((a, b) => b[1] - a[1]);
    };
    $("#rekap-klas-sm").innerHTML = barChart(perKlas(sm).map(([k, n]) =>
        [`${k} — ${NAMA_KLASIFIKASI[k] || ""}`, n]));
    $("#rekap-klas-sk").innerHTML = barChart(perKlas(sk).map(([k, n]) =>
        [`${k} — ${NAMA_KLASIFIKASI[k] || ""}`, n]));

    // rekap per bulan
    const masukPerBulan = Array(12).fill(0), keluarPerBulan = Array(12).fill(0);
    sm.forEach((s) => masukPerBulan[Number(s.tanggalSurat.slice(5, 7)) - 1]++);
    sk.forEach((s) => keluarPerBulan[Number(s.tanggalSurat.slice(5, 7)) - 1]++);

    const tbody = $("#tbody-rekap");
    tbody.innerHTML = BULAN_ID.map((b, i) => `
        <tr>
            <td>${b}</td>
            <td>${masukPerBulan[i]}</td>
            <td>${keluarPerBulan[i]}</td>
            <td class="fw-semibold">${masukPerBulan[i] + keluarPerBulan[i]}</td>
        </tr>`).join("") + `
        <tr class="table-light fw-bold">
            <td>Jumlah Tahun ${tahun}</td>
            <td>${sm.length}</td><td>${sk.length}</td>
            <td>${sm.length + sk.length}</td>
        </tr>`;

    $("#rekap-bulan").innerHTML = barChart(
        BULAN_ID.map((b, i) => [b, masukPerBulan[i] + keluarPerBulan[i]]).filter(([, n]) => n > 0));
}

$("#rekap-tahun").addEventListener("change", renderRekap);

/* ---------- notifikasi disposisi (web) ---------- */
async function muatNotif() {
    try {
        state.notifikasi = await api(API.disposisiSaya);
        const belum = state.notifikasi.filter((d) => d.status !== "Selesai").length;
        const badge = $("#notif-count");
        badge.textContent = belum;
        badge.hidden = belum === 0;
        if (!$("#page-disposisi-saya").hidden) renderDisposisiSaya();
    } catch (_) { /* abaikan - pengguna mungkin belum punya disposisi */ }
}

$("#btn-notif").addEventListener("click", (e) => {
    e.preventDefault();
    const link = document.querySelector('[data-page="disposisi-saya"]');
    link.click(); // pakai handler navigasi yang sudah ada
});

function renderDisposisiSaya() {
    const box = $("#daftar-disposisi-saya");
    const data = state.notifikasi;
    $("#ds-kosong").hidden = data.length > 0;
    box.innerHTML = "";
    data.forEach((d) => {
        const belum = d.status !== "Selesai";
        const lewat = belum && d.batasTanggal && new Date(d.batasTanggal) < new Date(new Date().toDateString());
        const col = document.createElement("div");
        col.className = "col-12 mb-3";
        col.innerHTML = `
        <div class="card border-0 shadow-sm ${belum ? "border-start border-4 border-primary" : "opacity-75"}">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                    <div>
                        <span class="badge text-bg-primary">${esc(d.nomorAgenda || "-")}</span>
                        <span class="badge text-bg-${belum ? (lewat ? "danger" : "warning text-dark") : "success"}">
                            ${belum ? (lewat ? "Terlambat — batas " + tglIndo(d.batasTanggal) : "Perlu Dikerjakan") : "Selesai ✓"}
                        </span>
                    </div>
                    <small class="text-muted">Didisposisikan: ${tglIndo(d.tanggalDisposisi)}</small>
                </div>
                <div class="mt-2"><b>Perihal:</b> ${esc(d.perihal || "-")}</div>
                <div class="small text-muted">Dari: ${esc(d.pengirim || "-")} | No. Surat: ${esc(d.nomorSurat || "-")}</div>
                ${d.instruksi ? `<div class="mt-2 p-2 bg-light border rounded small"><i class="bi bi-chat-left-text me-1 text-primary"></i><b>Instruksi:</b> ${esc(d.instruksi)}</div>` : ""}
                ${d.batasTanggal ? `<div class="small mt-1"><i class="bi bi-alarm me-1"></i>Batas waktu: ${tglIndo(d.batasTanggal)}</div>` : ""}
                ${belum ? `<button class="btn btn-sm btn-success mt-2" data-selesai="${d.id}"><i class="bi bi-check2-circle me-1"></i>Tandai Sudah Dikerjakan</button>` : ""}
            </div>
        </div>`;
        box.appendChild(col);
    });

    box.querySelectorAll("button[data-selesai]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            try {
                await api(API.disposisiSaya + "/" + btn.dataset.selesai + "/selesai", {method: "POST"});
                showToast("Disposisi ditandai selesai — terima kasih! 🎉", "success");
                await muatNotif();
            } catch (e) {
                showToast(e.message, "danger");
            }
        });
    });
}

/* ---------- import excel ---------- */
$("#btn-import").addEventListener("click", () => {
    $("#file-import").value = "";
    modalImport().show();
});

async function unduhTemplate() {
    try {
        const res = await fetch(API.template, {headers: {"Authorization": "Bearer " + state.token}});
        if (!res.ok) throw new Error("Gagal mengunduh template");
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "template-pegawai.xlsx";
        a.click();
        URL.revokeObjectURL(url);
        showToast("Template berhasil diunduh", "success");
    } catch (e) {
        showToast(e.message, "danger");
    }
}

$("#btn-template").addEventListener("click", unduhTemplate);
$("#btn-download-template").addEventListener("click", unduhTemplate);

$("#btn-upload").addEventListener("click", async () => {
    const fileInput = $("#file-import");
    if (!fileInput.files.length) {
        showToast("Silakan pilih file Excel terlebih dahulu", "warning");
        return;
    }
    const btn = $("#btn-upload");
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Mengunggah...`;
    try {
        const fd = new FormData();
        fd.append("file", fileInput.files[0]);
        const res = await fetch(API.importFile, {
            method: "POST",
            headers: {"Authorization": "Bearer " + state.token},
            body: fd,
        });
        const hasil = await res.json();
        if (!res.ok) throw new Error(hasil.message || "Import gagal");

        let pesan = `Import selesai: ${hasil.berhasil} berhasil, ${hasil.gagal} gagal (total ${hasil.totalBaris} baris).`;
        if (hasil.gagal > 0 && hasil.pesanGagal?.length) {
            pesan += " Rincian gagal: " + hasil.pesanGagal.join(" | ");
            showToast(pesan, "warning");
        } else {
            showToast(pesan, "success");
        }
        bootstrap.Modal.getInstance($("#modal-import")).hide();
        await muatPegawai();
    } catch (e) {
        showToast(e.message, "danger");
    } finally {
        btn.disabled = false;
        btn.innerHTML = `<i class="bi bi-upload me-1"></i>Unggah`;
    }
});

/* ---------- import excel: siswa / sarpras / surat ---------- */
const IMPORT_DATA = {
    siswa: {
        judul: "Peserta Didik",
        namaFile: "template-siswa.xlsx",
        muat: () => muatPesertaDidik(),
    },
    sarpras: {
        judul: "Sarana Prasarana",
        namaFile: "template-sarpras.xlsx",
        muat: () => muatSaranaPrasarana(),
    },
    surat: {
        judul: "Persuratan (Masuk & Keluar)",
        namaFile: "template-surat.xlsx",
        muat: async () => { await muatSemuaSurat(); await muatSurat("MASUK"); await muatSurat("KELUAR"); },
    },
};

async function unduhTemplateJenis(jenis) {
    const info = IMPORT_DATA[jenis];
    if (!info) return;
    try {
        const res = await fetch(API.importTemplate(jenis), {
            headers: {"Authorization": "Bearer " + state.token},
        });
        if (!res.ok) throw new Error("Gagal mengunduh template");
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = info.namaFile;
        a.click();
        URL.revokeObjectURL(url);
        showToast("Template berhasil diunduh", "success");
    } catch (e) {
        showToast(e.message, "danger");
    }
}

function bukaImportData(jenis) {
    if (!IMPORT_DATA[jenis]) return;
    if (state.importJalan) {
        showToast("Masih ada proses import berjalan. Tunggu sampai selesai.", "warning");
        return;
    }
    state.importJenis = jenis;
    $("#import-judul").textContent = IMPORT_DATA[jenis].judul;
    $("#file-import-data").value = "";
    $("#import-progress").hidden = true;
    const bar = $("#import-bar");
    bar.className = "progress-bar progress-bar-striped progress-bar-animated";
    bar.style.width = "0%";
    bar.textContent = "0%";
    $("#import-info").textContent = "";
    $("#import-errors").classList.add("d-none");
    $("#import-errors").innerHTML = "";
    $("#import-hasil").classList.add("d-none");
    $("#import-hasil").classList.remove("alert-warning");
    $("#import-hasil").classList.add("alert-success");
    $("#btn-upload-data").disabled = false;
    $("#btn-upload-data").innerHTML = `<i class="bi bi-upload me-1"></i>Unggah`;
    $("#btn-tutup-import-data").disabled = false;
    $("#btn-close-import-data").disabled = false;
    modalImportData().show();
}

async function unggahImportData() {
    if (state.importJalan) return;
    const fileInput = $("#file-import-data");
    if (!fileInput.files.length) {
        showToast("Silakan pilih file Excel terlebih dahulu", "warning");
        return;
    }
    const btn = $("#btn-upload-data");
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Mengunggah...`;
    try {
        const fd = new FormData();
        fd.append("file", fileInput.files[0]);
        const res = await fetch(API.importMulai(state.importJenis), {
            method: "POST",
            headers: {"Authorization": "Bearer " + state.token},
            body: fd,
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Gagal memulai import");

        state.importJalan = true;
        $("#import-progress").hidden = false;
        $("#btn-tutup-import-data").disabled = true;
        $("#btn-close-import-data").disabled = true;
        $("#btn-upload-data").innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Memproses...`;
        $("#import-info").textContent = "Menyiapkan data...";
        pantauImport(data.jobId);
    } catch (e) {
        showToast(e.message, "danger");
        btn.disabled = false;
        btn.innerHTML = `<i class="bi bi-upload me-1"></i>Unggah`;
    }
}

function pantauImport(jobId) {
    clearInterval(state.importTimer);
    const cek = async () => {
        try {
            const res = await fetch(API.importStatus(jobId), {
                headers: {"Authorization": "Bearer " + state.token},
            });
            const d = await res.json();
            if (!res.ok) throw new Error(d.message || "Gagal memantau proses import");
            renderProgresImport(d);
            if (d.status === "SELESAI" || d.status === "GAGAL") {
                clearInterval(state.importTimer);
                state.importTimer = null;
                state.importJalan = false;
                selesaikanImport(d);
            }
        } catch (e) {
            clearInterval(state.importTimer);
            state.importTimer = null;
            state.importJalan = false;
            showToast(e.message, "danger");
            $("#btn-tutup-import-data").disabled = false;
            $("#btn-close-import-data").disabled = false;
            $("#btn-upload-data").disabled = false;
            $("#btn-upload-data").innerHTML = `<i class="bi bi-upload me-1"></i>Unggah`;
        }
    };
    cek();
    state.importTimer = setInterval(cek, 1500);
}

function renderProgresImport(d) {
    const total = d.total || 0;
    const persen = total > 0 ? Math.min(100, Math.round((d.diproses / total) * 100)) : 0;
    const bar = $("#import-bar");
    bar.style.width = persen + "%";
    bar.textContent = persen + "%";
    if (d.status === "MENUNGGU") {
        $("#import-info").textContent = "Menunggu antrean...";
        return;
    }
    $("#import-info").textContent =
        `${d.diproses}/${total} baris diproses — ${d.berhasil} berhasil, ${d.gagal} gagal`;

    const box = $("#import-errors");
    box.innerHTML = "";
    const daftar = d.pesanGagal || [];
    if (daftar.length) {
        const ul = document.createElement("ul");
        ul.className = "mb-0 ps-3";
        daftar.forEach((pesan) => {
            const li = document.createElement("li");
            li.textContent = pesan;
            ul.appendChild(li);
        });
        if (d.pesanTerpotong) {
            const li = document.createElement("li");
            li.textContent = "… dan pesan lainnya (lihat ringkasan di atas).";
            ul.appendChild(li);
        }
        box.appendChild(ul);
        box.classList.remove("d-none");
    } else {
        box.classList.add("d-none");
    }
}

function selesaikanImport(d) {
    const bar = $("#import-bar");
    const info = $("#import-info");
    const hasil = $("#import-hasil");
    bar.classList.remove("progress-bar-animated");

    if (d.status === "GAGAL") {
        bar.classList.remove("bg-success");
        bar.classList.add("bg-danger");
        bar.style.width = "100%";
        bar.textContent = "Gagal";
        info.textContent = d.pesan || "Import gagal.";
        showToast(d.pesan || "Import gagal", "danger");
    } else {
        bar.classList.remove("bg-danger");
        bar.classList.add("bg-success");
        bar.style.width = "100%";
        bar.textContent = "100%";
        info.textContent = `${d.diproses} baris — ${d.berhasil} berhasil, ${d.gagal} gagal`;
        hasil.classList.remove("d-none", "alert-success", "alert-warning");
        hasil.classList.add(d.gagal > 0 ? "alert-warning" : "alert-success");
        hasil.textContent = d.gagal > 0
            ? `Import selesai. ${d.berhasil} baris masuk, ${d.gagal} baris dilewati (rincian di atas).`
            : `Import selesai. ${d.berhasil} baris berhasil dimasukkan.`;
        showToast(`Import selesai: ${d.berhasil} berhasil, ${d.gagal} gagal`,
            d.gagal > 0 ? "warning" : "success");
        const infoJenis = IMPORT_DATA[d.jenis];
        if (infoJenis) infoJenis.muat().catch(() => {});
    }

    $("#btn-tutup-import-data").disabled = false;
    $("#btn-close-import-data").disabled = false;
    $("#btn-upload-data").disabled = false;
    $("#btn-upload-data").innerHTML = `<i class="bi bi-upload me-1"></i>Unggah`;
}

$("#btn-import-siswa").addEventListener("click", () => bukaImportData("siswa"));
$("#btn-template-siswa").addEventListener("click", () => unduhTemplateJenis("siswa"));
$("#btn-import-sarpras").addEventListener("click", () => bukaImportData("sarpras"));
$("#btn-template-sarpras").addEventListener("click", () => unduhTemplateJenis("sarpras"));
$("#btn-import-sm").addEventListener("click", () => bukaImportData("surat"));
$("#btn-template-sm").addEventListener("click", () => unduhTemplateJenis("surat"));
$("#btn-import-sk").addEventListener("click", () => bukaImportData("surat"));
$("#btn-template-sk").addEventListener("click", () => unduhTemplateJenis("surat"));
$("#btn-download-template-data").addEventListener("click", () => unduhTemplateJenis(state.importJenis));
$("#btn-upload-data").addEventListener("click", unggahImportData);

/* ---------- dashboard ---------- */
function renderDashboard() {
    const list = state.pegawaiList;
    const siswaAktif = state.pesertaDidikList.filter((p) => (p.status || "Aktif") === "Aktif").length;
    const prestasi = state.kesiswaanList.filter((k) => k.jenis === "Prestasi").length;
    const total = list.length;
    const guru = list.filter((p) => ["Guru", "Wali Kelas", "Kepala Sekolah", "Wakil Kepala Sekolah"].includes(p.jabatan)).length;
    const tendik = total - guru;
    const pt = list.filter((p) => p.statusKepegawaian === "Pegawai Tetap").length;

    $("#stats").innerHTML = `
        <div class="col-6 col-lg-3"><div class="stat-card stat-primary p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${total}</div><div class="lbl">Total Pegawai</div></div><i class="bi bi-people-fill ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-success p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${guru}</div><div class="lbl">Guru & Pimpinan</div></div><i class="bi bi-person-video3 ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-warning p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${tendik}</div><div class="lbl">Tenaga Kependidikan</div></div><i class="bi bi-person-hearts ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-info p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${pt}</div><div class="lbl">Pegawai Tetap</div></div><i class="bi bi-patch-check ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-secondary p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${state.surat.MASUK.length}</div><div class="lbl">Arsip Surat Masuk</div></div><i class="bi bi-envelope-arrow-down ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-danger p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${state.surat.KELUAR.length}</div><div class="lbl">Arsip Surat Keluar</div></div><i class="bi bi-envelope-arrow-up ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-success p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${siswaAktif}</div><div class="lbl">Peserta Didik Aktif</div></div><i class="bi bi-backpack2 ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-info p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${state.saranaList.length}</div><div class="lbl">Item Sarana Prasarana</div></div><i class="bi bi-hammer ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-warning p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${prestasi}</div><div class="lbl">Catatan Prestasi Siswa</div></div><i class="bi bi-trophy ico"></i></div></div>`;

    $("#chart-status").innerHTML = barChart(grupkan(list, "statusKepegawaian"));
    $("#chart-jabatan").innerHTML = barChart(grupkan(list, "jabatan"));
    $("#chart-kelas").innerHTML = barChart(grupkan(state.pesertaDidikList.filter((p) => (p.status || "Aktif") === "Aktif"), "kelas"));
}

function grupkan(list, field) {
    const map = {};
    list.forEach((p) => {
        const k = p[field] || "(kosong)";
        map[k] = (map[k] || 0) + 1;
    });
    return Object.entries(map).sort((a, b) => b[1] - a[1]);
}

function barChart(entries) {
    if (!entries.length) return `<div class="text-muted small py-3 text-center"><i class="bi bi-inbox me-1"></i>Belum ada data</div>`;
    const max = Math.max(...entries.map(([, n]) => n));
    return entries
        .map(([label, n]) => `
        <div class="bar-row">
            <span class="text-truncate" title="${esc(label)}">${esc(label)}</span>
            <div class="bar-track"><div class="bar-fill" style="width:${(n / max) * 100}%"></div></div>
            <span class="bar-count">${n}</span>
        </div>`)
        .join("");
}

/* ---------- pengguna (ADMIN) ---------- */
async function muatUsers() {
    try {
        const users = await api(API.users);
        const tbody = $("#tbody-users");
        tbody.innerHTML = "";
        users.forEach((u, i) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${i + 1}</td>
                <td class="fw-semibold">${esc(u.username)}</td>
                <td>${esc(u.namaLengkap)}</td>
                <td><span class="badge text-bg-${u.role === "ADMIN" ? "primary" : "secondary"}">${esc(u.role)}</span></td>
                <td class="text-end"><button class="btn btn-sm btn-outline-danger" data-id="${u.id}"><i class="bi bi-trash"></i></button></td>`;
            tbody.appendChild(tr);
        });
        tbody.querySelectorAll("button[data-id]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                if (!confirm("Hapus pengguna ini?")) return;
                try {
                    await api(API.users + "/" + btn.dataset.id, {method: "DELETE"});
                    showToast("Pengguna dihapus", "success");
                    await muatUsers();
                } catch (e) {
                    showToast(e.message, "danger");
                }
            });
        });
    } catch (e) {
        showToast(e.message, "danger");
    }
}

$("#btn-tambah-user").addEventListener("click", () => {
    $("#form-user").reset();
    modalUser().show();
});

$("#form-user").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        await api(API.users, {
            method: "POST",
            body: JSON.stringify({
                username: $("#u-username").value.trim(),
                password: $("#u-password").value,
                namaLengkap: $("#u-nama").value.trim(),
                role: $("#u-role").value,
            }),
        });
        bootstrap.Modal.getInstance($("#modal-user")).hide();
        showToast("Pengguna baru ditambahkan", "success");
        await muatUsers();
    } catch (ex) {
        showToast(ex.message, "danger");
    }
});

/* ---------- ganti password ---------- */
$("#form-password").addEventListener("submit", async (e) => {
    e.preventDefault();
    if ($("#pw-baru").value !== $("#pw-baru2").value) {
        showToast("Konfirmasi password tidak sama", "danger");
        return;
    }
    try {
        await api(API.changePassword, {
            method: "POST",
            body: JSON.stringify({
                passwordLama: $("#pw-lama").value,
                passwordBaru: $("#pw-baru").value,
            }),
        });
        showToast("Password berhasil diubah", "success");
        $("#form-password").reset();
    } catch (ex) {
        showToast(ex.message, "danger");
    }
});

/* ---------- util ---------- */
function esc(s) {
    return String(s ?? "").replace(/[&<>"']/g, (c) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[c]);
}

if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("/sw.js").catch(() => {});
}

/* ---------- mulai ---------- */
(async function init() {
    if (state.token) {
        $("#login-view").hidden = true;
        $("#app-view").hidden = false;
        await masukAplikasi();
    }
})();
