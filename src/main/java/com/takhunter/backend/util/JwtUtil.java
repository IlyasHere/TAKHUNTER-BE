package com.takhunter.backend.util;

import com.takhunter.backend.model.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(User user) {
        long now = Instant.now().toEpochMilli();
        long expiredAt = now + expiration;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{"
                + "\"sub\":\"" + user.getEmail() + "\","
                + "\"id\":" + user.getId() + ","
                + "\"role\":\"" + user.getRole() + "\","
                + "\"iat\":" + now + ","
                + "\"exp\":" + expiredAt
                + "}";

        String encodedHeader = base64UrlEncode(header);
        String encodedPayload = base64UrlEncode(payload);
        String unsignedToken = encodedHeader + "." + encodedPayload;
        String signature = createSignature(unsignedToken);

        return unsignedToken + "." + signature;
    }

    public boolean isTokenValid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = createSignature(unsignedToken);
            if (!expectedSignature.equals(parts[2])) {
                return false;
            }

            Long expiredAt = getLongClaim(token, "exp");
            return expiredAt != null && expiredAt > Instant.now().toEpochMilli();
        } catch (Exception exception) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return getStringClaim(token, "sub");
    }

    private String createSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (Exception exception) {
            throw new RuntimeException("Gagal membuat JWT signature", exception);
        }
    }

    private String getStringClaim(String token, String claimName) {
        String payload = decodePayload(token);
        String pattern = "\"" + claimName + "\":\"";
        int start = payload.indexOf(pattern);
        if (start == -1) {
            return null;
        }

        start += pattern.length();
        int end = payload.indexOf("\"", start);
        if (end == -1) {
            return null;
        }

        return payload.substring(start, end);
    }

    private Long getLongClaim(String token, String claimName) {
        String payload = decodePayload(token);
        String pattern = "\"" + claimName + "\":";
        int start = payload.indexOf(pattern);
        if (start == -1) {
            return null;
        }

        start += pattern.length();
        int end = payload.indexOf(",", start);
        if (end == -1) {
            end = payload.indexOf("}", start);
        }
        if (end == -1) {
            return null;
        }

        return Long.parseLong(payload.substring(start, end));
    }

    private String decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return "";
        }

        byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
