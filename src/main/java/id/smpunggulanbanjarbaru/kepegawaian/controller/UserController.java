package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.dto.RegisterRequest;
import id.smpunggulanbanjarbaru.kepegawaian.model.User;
import id.smpunggulanbanjarbaru.kepegawaian.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Endpoint kelola pengguna — khusus ADMIN (lihat SecurityConfig). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public List<User> daftar() {
        // Password jangan pernah dikirim ke klien
        return authService.listUsers().stream()
                .peek(u -> u.setPassword(null))
                .toList();
    }

    @PostMapping
    public User tambah(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        user.setPassword(null);
        return user;
    }

    @DeleteMapping("/{id}")
    public void hapus(@PathVariable Long id,
                      @AuthenticationPrincipal UserDetails principal) {
        authService.deleteUser(id, principal.getUsername());
    }

    /** Ganti password akun sendiri: POST /api/users/change-password */
    @PostMapping("/change-password")
    public Map<String, String> gantiPassword(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, String> body) {

        String lama = body.get("passwordLama");
        String baru = body.get("passwordBaru");
        if (lama == null || baru == null || baru.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password baru minimal 6 karakter");
        }
        authService.changePassword(principal.getUsername(), lama, baru);
        return Map.of("message", "Password berhasil diubah");
    }
}
