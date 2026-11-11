package id.smpunggulanbanjarbaru.kepegawaian.service;

import id.smpunggulanbanjarbaru.kepegawaian.dto.AuthResponse;
import id.smpunggulanbanjarbaru.kepegawaian.dto.LoginRequest;
import id.smpunggulanbanjarbaru.kepegawaian.dto.RegisterRequest;
import id.smpunggulanbanjarbaru.kepegawaian.model.Role;
import id.smpunggulanbanjarbaru.kepegawaian.model.User;
import id.smpunggulanbanjarbaru.kepegawaian.repository.UserRepository;
import id.smpunggulanbanjarbaru.kepegawaian.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service autentikasi: login (mengeluarkan JWT), daftar pengguna baru (khusus admin),
 * dan penggantian password.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pengguna tidak ditemukan"));

        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, jwtService.getExpirationMs(),
                user.getUsername(), user.getNamaLengkap(), user.getRole().name());
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username sudah digunakan");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNamaLengkap(request.getNamaLengkap());
        user.setRole(request.getRole());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String passwordLama, String passwordBaru) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pengguna tidak ditemukan"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, passwordLama));

        user.setPassword(passwordEncoder.encode(passwordBaru));
        userRepository.save(user);
    }

    /** Info pengguna yang sedang login (untuk GET /api/auth/me). */
    public Map<String, Object> currentUserInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pengguna tidak ditemukan"));

        Map<String, Object> info = new HashMap<>();
        info.put("username", user.getUsername());
        info.put("namaLengkap", user.getNamaLengkap());
        info.put("role", user.getRole().name());
        return info;
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id, String usernameYangLogin) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pengguna tidak ditemukan"));
        if (user.getUsername().equals(usernameYangLogin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tidak dapat menghapus akun sendiri");
        }
        long jumlahAdmin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.isAktif()).count();
        if (user.getRole() == Role.ADMIN && jumlahAdmin <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimal harus ada satu ADMIN");
        }
        userRepository.delete(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pengguna tidak ditemukan"));
    }
}
