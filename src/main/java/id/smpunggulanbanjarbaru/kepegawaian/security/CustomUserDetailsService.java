package id.smpunggulanbanjarbaru.kepegawaian.security;

import id.smpunggulanbanjarbaru.kepegawaian.model.User;
import id.smpunggulanbanjarbaru.kepegawaian.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mengambil data pengguna dari database untuk proses autentikasi Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Pengguna tidak ditemukan: " + username));

        if (!user.isAktif()) {
            throw new UsernameNotFoundException("Akun tidak aktif: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(() -> "ROLE_" + user.getRole().name())
        );
    }
}
