package com.fxexposure.service;

import com.fxexposure.dto.LoginRequestDto;
import com.fxexposure.dto.LoginResponseDto;
import com.fxexposure.dto.UserRequestDto;
import com.fxexposure.dto.UserResponseDto;
import com.fxexposure.entity.User;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.repository.UserRepository;
import com.fxexposure.security.CustomUserDetailsService;
import com.fxexposure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user registration and JWT authentication.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Registers a new user with BCrypt hashed password and specified role.
     *
     * @param requestDto user registration payload
     * @return UserResponseDto excluding password
     */
    public UserResponseDto register(UserRequestDto requestDto) {
        // TODO: In a production application, public registration should not allow users to choose ADMIN privileges.
        if (userRepository.existsByUsername(requestDto.getUsername())) {
            throw new DuplicateResourceException("Username is already taken: " + requestDto.getUsername());
        }

        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + requestDto.getEmail());
        }

        User user = User.builder()
                .username(requestDto.getUsername())
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(requestDto.getRole())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        return UserResponseDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .enabled(savedUser.isEnabled())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();
    }

    /**
     * Authenticates user credentials and generates a signed JWT token.
     *
     * @param requestDto login credentials (username and password)
     * @return LoginResponseDto containing JWT token, username, role, and Bearer token type
     */
    public LoginResponseDto login(LoginRequestDto requestDto) {
        // Authenticate credentials against configured AuthenticationProvider
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestDto.getUsername(), requestDto.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(requestDto.getUsername());
        User user = userRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));

        String jwtToken = jwtService.generateToken(userDetails);

        return LoginResponseDto.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .role(user.getRole())
                .tokenType("Bearer")
                .build();
    }
}
