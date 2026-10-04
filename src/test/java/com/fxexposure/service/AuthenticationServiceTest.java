package com.fxexposure.service;

import com.fxexposure.dto.LoginRequestDto;
import com.fxexposure.dto.LoginResponseDto;
import com.fxexposure.dto.UserRequestDto;
import com.fxexposure.dto.UserResponseDto;
import com.fxexposure.entity.Role;
import com.fxexposure.entity.User;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.repository.UserRepository;
import com.fxexposure.security.CustomUserDetailsService;
import com.fxexposure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User sampleUser;
    private UserRequestDto registrationDto;
    private LoginRequestDto loginDto;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("trader_bob")
                .email("bob@fxexposure.com")
                .password("$2a$10$encodedHash")
                .role(Role.MANAGER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registrationDto = UserRequestDto.builder()
                .username("trader_bob")
                .email("bob@fxexposure.com")
                .password("Password123!")
                .role(Role.MANAGER)
                .build();

        loginDto = LoginRequestDto.builder()
                .username("trader_bob")
                .password("Password123!")
                .build();

        userDetails = new org.springframework.security.core.userdetails.User(
                "trader_bob",
                "$2a$10$encodedHash",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void register_Success() {
        when(userRepository.existsByUsername("trader_bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@fxexposure.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponseDto response = authenticationService.register(registrationDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("trader_bob");
        assertThat(response.getEmail()).isEqualTo("bob@fxexposure.com");
        assertThat(response.getRole()).isEqualTo(Role.MANAGER);
        assertThat(response.isEnabled()).isTrue();
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on registration with existing username")
    void register_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername("trader_bob")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(registrationDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on registration with existing email")
    void register_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByUsername("trader_bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@fxexposure.com")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(registrationDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login and return JWT token")
    void login_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        when(userDetailsService.loadUserByUsername("trader_bob")).thenReturn(userDetails);
        when(userRepository.findByUsername("trader_bob")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(userDetails)).thenReturn("mocked.jwt.token");

        LoginResponseDto response = authenticationService.login(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getUsername()).isEqualTo("trader_bob");
        assertThat(response.getRole()).isEqualTo(Role.MANAGER);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException on invalid login")
    void login_InvalidCredentials_ThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(loginDto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }
}
