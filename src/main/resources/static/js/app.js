/* =========================================================
 * Aplikasi Kepegawaian SMP Unggulan Idaman Banjarbaru
 * Frontend memanggil REST API backend + token JWT.
 * ========================================================= */
"use strict";

const API = {
    login: "/api/auth/login",
    me: "/api/auth/me",
    logout: "/api/auth/logout",
    pegawai: "/api/pegawai",
    users: "/api/users",
    changePassword: "/api/users/change-password",
};

const state = {
    token: localStorage.getItem("jwt_token") || null,
    user: null, // {username, namaLengkap, role}
    pegawaiList: [],
    editId: null,
    editUserId: null,
};

/* ---------- util ---------- */
const $ = (sel) => document.querySelector(sel);

function showToast(msg, type = "ok") {
    const t = $("#toast");
    t.textContent = msg;
    t.className = type;
    t.hidden = false;
    setTimeout(() => (t.hidden = true), 2600);
}

async function api(path, options = {}) {
    const headers = Object.assign(
        {"Content-Type": "application/json"},
        options.headers || {}
    );
    if (state.token) headers["Authorization"] = "Bearer " + state.token;

    const res = await fetch(path, {...options, headers});

    if (res.status === 401) {
        // Token kedaluwarsa / tidak valid -> paksa keluar
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
        return; // sudah dipaksa logout oleh api()
    }
    $("#login-view").hidden = true;
    $("#app-view").hidden = false;
    $("#user-chip").textContent = state.user.namaLengkap;
    $("#role-label").textContent = "Login sebagai: " + state.user.role;

    const admin = state.user.role === "ADMIN";
    document.querySelectorAll(".admin-only").forEach((el) => {
        el.style.display = admin ? "" : "none";
    });

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
    showToast("Anda telah keluar");
});

/* ---------- navigasi ---------- */
document.querySelectorAll(".nav-btn[data-page]").forEach((btn) => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".nav-btn").forEach((b) => b.classList.remove("active"));
        btn.classList.add("active");
        document.querySelectorAll(".page").forEach((p) => (p.hidden = true));
        $("#page-" + btn.dataset.page).hidden = false;
        if (window.innerWidth <= 860) $("#sidebar").classList.remove("open");
        if (btn.dataset.page === "pengguna" && state.user?.role === "ADMIN") muatUsers();
    });
});
$("#btn-menu").addEventListener("click", () => $("#sidebar").classList.toggle("open"));

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

    const admin = state.user?.role === "ADMIN";
    data.forEach((p, i) => {
        const tr = document.createElement("tr");
        const badgeClass = "badge badge-" + (p.statusKepegawaian || "");
        tr.innerHTML = `
            <td>${i + 1}</td>
            <td><strong>${esc(p.nama)}</strong></td>
            <td>${esc(p.nip || "-")}</td>
            <td>${esc(p.jabatan)}</td>
            <td><span class="${badgeClass}">${esc(p.statusKepegawaian)}</span></td>
            <td>${esc(p.mataPelajaran || "-")}</td>
            <td>${esc(p.pendidikanTerakhir || "-")}</td>
            <td>${esc(p.telepon || "-")}</td>
            <td class="aksi-col">
                ${admin ? `
                <button class="btn btn-small" data-aksi="edit" data-id="${p.id}">✏️ Ubah</button>
                <button class="btn btn-small btn-danger" data-aksi="hapus" data-id="${p.id}">🗑 Hapus</button>`
                : "<span style='color:var(--muted)'>—</span>"}
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
        showToast("Data pegawai dihapus");
        await muatPegawai();
    } catch (e) {
        showToast(e.message, "err");
    }
}

/* ---------- modal pegawai ---------- */
const JABATAN_LAINNYA = "Lainnya";

function bukaModalTambah() {
    state.editId = null;
    $("#modal-pegawai-title").textContent = "Tambah Pegawai";
    $("#form-pegawai").reset();
    $("#modal-pegawai").hidden = false;
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
    $("#modal-pegawai").hidden = false;
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
            await api(API.pegawai + "/" + state.editId, {
                method: "PUT",
                body: JSON.stringify(payload),
            });
            showToast("Data pegawai diperbarui");
        } else {
            await api(API.pegawai, {method: "POST", body: JSON.stringify(payload)});
            showToast("Pegawai baru ditambahkan");
        }
        $("#modal-pegawai").hidden = true;
        await muatPegawai();
    } catch (ex) {
        showToast(ex.message, "err");
    }
});

document.querySelectorAll("[data-close]").forEach((btn) => {
    btn.addEventListener("click", () => ($(btn.dataset.close ? "#" + btn.dataset.close : "")).hidden = true);
});

$("#btn-tambah").addEventListener("click", bukaModalTambah);

/* ---------- dashboard ---------- */
function renderDashboard() {
    const list = state.pegawaiList;
    const total = list.length;
    const guru = list.filter((p) => ["Guru", "Wali Kelas", "Kepala Sekolah", "Wakil Kepala Sekolah"].includes(p.jabatan)).length;
    const tendik = total - guru;
    const pns = list.filter((p) => p.statusKepegawaian === "PNS").length;

    $("#stats").innerHTML = `
        <div class="stat"><div class="num">${total}</div><div class="lbl">Total Pegawai</div></div>
        <div class="stat"><div class="num">${guru}</div><div class="lbl">Guru &amp; Pimpinan</div></div>
        <div class="stat"><div class="num">${tendik}</div><div class="lbl">Tenaga Kependidikan</div></div>
        <div class="stat"><div class="num">${pns}</div><div class="lbl">PNS</div></div>`;

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
    if (!entries.length) return "<em style='color:var(--muted)'>Belum ada data</em>";
    const max = Math.max(...entries.map(([, n]) => n));
    return entries
        .map(
            ([label, n]) => `
        <div class="bar-row">
            <span>${esc(label)}</span>
            <div class="bar-track"><div class="bar-fill" style="width:${(n / max) * 100}%"></div></div>
            <span class="bar-count">${n}</span>
        </div>`
        )
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
                <td>${esc(u.username)}</td>
                <td>${esc(u.namaLengkap)}</td>
                <td><span class="badge ${u.role === "ADMIN" ? "badge-PNS" : "badge-PPPK"}">${esc(u.role)}</span></td>
                <td><button class="btn btn-small btn-danger" data-aksi="hapus-user" data-id="${u.id}">🗑 Hapus</button></td>`;
            tbody.appendChild(tr);
        });
        tbody.querySelectorAll("button[data-aksi='hapus-user']").forEach((btn) => {
            btn.addEventListener("click", async () => {
                if (!confirm("Hapus pengguna ini?")) return;
                try {
                    await api(API.users + "/" + btn.dataset.id, {method: "DELETE"});
                    showToast("Pengguna dihapus");
                    await muatUsers();
                } catch (e) {
                    showToast(e.message, "err");
                }
            });
        });
    } catch (e) {
        showToast(e.message, "err");
    }
}

$("#btn-tambah-user").addEventListener("click", () => {
    $("#form-user").reset();
    $("#modal-user").hidden = false;
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
        $("#modal-user").hidden = true;
        showToast("Pengguna baru ditambahkan");
        await muatUsers();
    } catch (ex) {
        showToast(ex.message, "err");
    }
});

/* ---------- ganti password ---------- */
$("#form-password").addEventListener("submit", async (e) => {
    e.preventDefault();
    if ($("#pw-baru").value !== $("#pw-baru2").value) {
        showToast("Konfirmasi password tidak sama", "err");
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
        showToast("Password berhasil diubah");
        $("#form-password").reset();
    } catch (ex) {
        showToast(ex.message, "err");
    }
});

/* ---------- util lain ---------- */
function esc(s) {
    return String(s ?? "").replace(/[&<>"']/g, (c) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[c]);
}

/* service worker (agar bisa di-install di Android sebagai PWA) */
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
