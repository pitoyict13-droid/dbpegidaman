/* =========================================================
 * Aplikasi Kepegawaian SMP Unggulan Idaman Banjarbaru
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
    users: "/api/users",
    changePassword: "/api/users/change-password",
    surat: "/api/surat",
};

const state = {
    token: localStorage.getItem("jwt_token") || null,
    user: null,
    pegawaiList: [],
    editId: null,
    surat: { MASUK: [], KELUAR: [] },
    editSuratId: null,
};

const $ = (sel) => document.querySelector(sel);

/* Tandai ke skrip fallback bahwa app.js dimuat dengan sukses */
window.__APP_READY = true;

/* ---------- komponen Bootstrap ---------- */
const modalPegawai = () => new bootstrap.Modal("#modal-pegawai");
const modalUser = () => new bootstrap.Modal("#modal-user");
const modalImport = () => new bootstrap.Modal("#modal-import");
const modalSurat = () => new bootstrap.Modal("#modal-surat");

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

    await muatPegawai();
    await muatSemuaSurat();
    if (admin) await muatUsers();
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

/* ---------- navigasi halaman ---------- */
document.querySelectorAll(".page-link-nav").forEach((link) => {
    link.addEventListener("click", (e) => {
        e.preventDefault();
        document.querySelectorAll(".page-link-nav").forEach((l) => l.classList.remove("active"));
        link.classList.add("active");
        document.querySelectorAll(".page").forEach((p) => (p.hidden = true));
        $("#page-" + link.dataset.page).hidden = false;
        const nav = document.getElementById("mainNav");
        if (nav.classList.contains("show")) bootstrap.Collapse.getOrCreateInstance(nav).hide();
        if (link.dataset.page === "pengguna" && state.user?.role === "ADMIN") muatUsers();
        if (link.dataset.page === "surat-masuk") muatSurat("MASUK");
        if (link.dataset.page === "surat-keluar") muatSurat("KELUAR");
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

/* ---------- dashboard ---------- */
function renderDashboard() {
    const list = state.pegawaiList;
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
            <div><div class="num">${state.surat.KELUAR.length}</div><div class="lbl">Arsip Surat Keluar</div></div><i class="bi bi-envelope-arrow-up ico"></i></div></div>`;

    $("#chart-status").innerHTML = barChart(grupkan(list, "statusKepegawaian"));
    $("#chart-jabatan").innerHTML = barChart(grupkan(list, "jabatan"));
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
