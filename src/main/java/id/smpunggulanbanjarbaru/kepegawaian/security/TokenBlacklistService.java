package id.smpunggulanbanjarbaru.kepegawaian.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mencatat token yang sudah "logout" (blacklist) sampai masa berlakunya berakhir,
 * sehingga token yang dicabut tidak bisa dipakai lagi.
 */
@Service
public class TokenBlacklistService {

    /** token -> waktu kedaluwarsa (epoch millis) */
    private final Map<String, Long> blacklisted = new ConcurrentHashMap<>();

    public void blacklist(String token, long expirationEpochMillis) {
        blacklisted.put(token, expirationEpochMillis);
        cleanup();
    }

    public boolean isBlacklisted(String token) {
        cleanup();
        return blacklisted.containsKey(token);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        blacklisted.entrySet().removeIf(e -> e.getValue() < now);
    }
}
