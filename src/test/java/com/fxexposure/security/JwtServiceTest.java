package com.fxexposure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails sampleUserDetails;

    // 256-bit Hex key for testing
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_EXPIRATION);

        sampleUserDetails = new User(
                "analyst_user",
                "password123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANALYST"))
        );
    }

    @Test
    @DisplayName("Should successfully generate a JWT token")
    void generateToken_Success() {
        String token = jwtService.generateToken(sampleUserDetails);

        assertThat(token).isNotNull().isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // Header.Payload.Signature
    }

    @Test
    @DisplayName("Should extract username from valid JWT token")
    void extractUsername_Success() {
        String token = jwtService.generateToken(sampleUserDetails);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("analyst_user");
    }

    @Test
    @DisplayName("Should validate token successfully for matching user")
    void isTokenValid_ValidToken_ReturnsTrue() {
        String token = jwtService.generateToken(sampleUserDetails);
        boolean isValid = jwtService.isTokenValid(token, sampleUserDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false when validating token against different user")
    void isTokenValid_DifferentUser_ReturnsFalse() {
        String token = jwtService.generateToken(sampleUserDetails);
        UserDetails differentUser = new User(
                "different_user",
                "password123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANALYST"))
        );

        boolean isValid = jwtService.isTokenValid(token, differentUser);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should detect expired token and report expiration")
    void isTokenExpired_ExpiredToken() {
        // Create JwtService with negative expiration (already expired)
        JwtService expiredJwtService = new JwtService();
        ReflectionTestUtils.setField(expiredJwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(expiredJwtService, "jwtExpiration", -1000L);

        String expiredToken = expiredJwtService.generateToken(sampleUserDetails);

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, sampleUserDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should fail when parsing invalid/tampered token")
    void extractUsername_InvalidToken_ThrowsException() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalidPayload.invalidSignature";

        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
