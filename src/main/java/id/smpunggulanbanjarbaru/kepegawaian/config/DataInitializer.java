package id.smpunggulanbanjarbaru.kepegawaian.config;

import id.smpunggulanbanjarbaru.kepegawaian.model.Role;
import id.smpunggulanbanjarbaru.kepegawaian.model.User;
import id.smpunggulanbanjarbaru.kepegawaian.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Membuat akun admin pertama secara otomatis saat aplikasi baru dijalankan,
 * hanya bila belum ada pengguna di database.
 *
 * Username: admin | Password: admin123  --> WAJIB diganti setelah login pertama.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNamaLengkap("Administrator Sekolah");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }
    }
}
