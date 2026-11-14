package id.smpunggulanbanjarbaru.kepegawaian.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Konfigurasi keamanan aplikasi:
 * - Login via JWT (stateless, tanpa session server).
 * - Endpoint publik: halaman web statis + /api/auth/login.
 * - Endpoint /api/users dan operasi tulis pegawai hanya untuk ADMIN.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Halaman web (frontend statis) boleh diakses semua
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/image/**", "/manifest.json", "/sw.js").permitAll()
                        // Login terbuka
                        .requestMatchers("/api/auth/login").permitAll()
                        // Kelola pengguna: khusus ADMIN
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        // Tambah/ubah/hapus pegawai: khusus ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/pegawai/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pegawai/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/pegawai/**").hasRole("ADMIN")
                        // Ubah profil sekolah: khusus ADMIN (lihat: semua yang login)
                        .requestMatchers(HttpMethod.PUT, "/api/profil-sekolah/**").hasRole("ADMIN")
                        // Tambah/ubah/hapus peserta didik: khusus ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/peserta-didik/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/peserta-didik/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/peserta-didik/**").hasRole("ADMIN")
                        // Tambah/ubah/hapus sarana prasarana: khusus ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/sarana-prasarana/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/sarana-prasarana/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/sarana-prasarana/**").hasRole("ADMIN")
                        // Tambah/ubah/hapus data kesiswaan: khusus ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/kesiswaan/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/kesiswaan/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/kesiswaan/**").hasRole("ADMIN")
                        // Tambah/ubah/hapus surat (persuratan): khusus ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/surat/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/surat/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/surat/**").hasRole("ADMIN")
                        // Template & import massal (siswa, sarpras, surat): khusus ADMIN
                        .requestMatchers("/api/import/**").hasRole("ADMIN")
                        // Sisanya (termasuk lihat data pegawai): wajib login
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // dibatasi di produksi
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
