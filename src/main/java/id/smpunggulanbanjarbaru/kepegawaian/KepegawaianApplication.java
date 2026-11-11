package id.smpunggulanbanjarbaru.kepegawaian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KepegawaianApplication {

    public static void main(String[] args) {
        SpringApplication.run(KepegawaianApplication.class, args);
        System.out.println("""
                ==========================================================
                 Aplikasi Kepegawaian SMP Unggulan Idaman Banjarbaru
                 Berjalan di http://localhost:8080
                 Login awal -> admin / admin123 (segera ganti password)
                ==========================================================  """);
    }
}
