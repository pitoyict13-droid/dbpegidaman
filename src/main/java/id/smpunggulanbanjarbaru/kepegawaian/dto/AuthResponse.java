package id.smpunggulanbanjarbaru.kepegawaian.dto;

public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private long expiresInMillis;
    private String username;
    private String namaLengkap;
    private String role;

    public AuthResponse(String token, long expiresInMillis, String username, String namaLengkap, String role) {
        this.token = token;
        this.expiresInMillis = expiresInMillis;
        this.username = username;
        this.namaLengkap = namaLengkap;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public long getExpiresInMillis() { return expiresInMillis; }
    public String getUsername() { return username; }
    public String getNamaLengkap() { return namaLengkap; }
    public String getRole() { return role; }
}
