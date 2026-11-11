package id.smpunggulanbanjarbaru.kepegawaian.controller;

import id.smpunggulanbanjarbaru.kepegawaian.dto.AuthResponse;
import id.smpunggulanbanjarbaru.kepegawaian.dto.LoginRequest;
import id.smpunggulanbanjarbaru.kepegawaian.security.JwtService;
import id.smpunggulanbanjarbaru.kepegawaian.security.TokenBlacklistService;
import id.smpunggulanbanjarbaru.kepegawaian.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;

    public AuthController(AuthService authService, JwtService jwtService,
                          TokenBlacklistService blacklistService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
    }

    /** POST /api/auth/login — mengembalikan JWT bila username & password benar. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** GET /api/auth/me — profil pengguna yang sedang login. */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal UserDetails principal) {
        return authService.currentUserInfo(principal.getUsername());
    }

    /** POST /api/auth/logout — mencabut token yang dipakai (dimasukkan ke blacklist). */
    @PostMapping("/logout")
    public Map<String, String> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header Authorization tidak ada");
        }
        String token = authHeader.substring(7);
        if (jwtService.extractUsername(token) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token tidak valid");
        }
        // Token dicabut sampai masa berlakunya paling lama berakhir
        blacklistService.blacklist(token, System.currentTimeMillis() + jwtService.getExpirationMs());
        return Map.of("message", "Logout berhasil");
    }
}
