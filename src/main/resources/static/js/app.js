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
};

const state = {
    token: localStorage.getItem("jwt_token") || null,
    user: null,
    pegawaiList: [],
    editId: null,
};

const $ = (sel) => document.querySelector(sel);

/* ---------- komponen Bootstrap ---------- */
const modalPegawai = () => new bootstrap.Modal("#modal-pegawai");
const modalUser = () => new bootstrap.Modal("#modal-user");
const modalImport = () => new bootstrap.Modal("#modal-import");

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
        const warnaBadge = {"PNS": "primary", "PPPK": "success", "GTT": "warning text-dark", "PTT": "warning text-dark", "Honorer": "secondary"};
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
    const pns = list.filter((p) => p.statusKepegawaian === "PNS").length;

    $("#stats").innerHTML = `
        <div class="col-6 col-lg-3"><div class="stat-card stat-primary p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${total}</div><div class="lbl">Total Pegawai</div></div><i class="bi bi-people-fill ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-success p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${guru}</div><div class="lbl">Guru & Pimpinan</div></div><i class="bi bi-person-video3 ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-warning p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${tendik}</div><div class="lbl">Tenaga Kependidikan</div></div><i class="bi bi-person-hearts ico"></i></div></div>
        <div class="col-6 col-lg-3"><div class="stat-card stat-info p-3 d-flex justify-content-between align-items-center">
            <div><div class="num">${pns}</div><div class="lbl">PNS</div></div><i class="bi bi-patch-check ico"></i></div></div>`;

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
