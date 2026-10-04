package com.fxexposure.service;

import com.fxexposure.dto.UserRequestDto;
import com.fxexposure.dto.UserResponseDto;
import com.fxexposure.entity.User;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class handling user management operations and business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user after verifying unique constraints.
     *
     * @param requestDto the user creation payload
     * @return UserResponseDto representing the created user
     */
    public UserResponseDto createUser(UserRequestDto requestDto) {
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
        return mapToResponseDto(savedUser);
    }

    /**
     * Retrieve a user by their unique identifier.
     *
     * @param id user ID
     * @return UserResponseDto of the found user
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponseDto(user);
    }

    /**
     * Retrieve all registered users.
     *
     * @return list of UserResponseDto
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Update an existing user.
     *
     * @param id user ID
     * @param requestDto updated user data
     * @return UserResponseDto with updated details
     */
    public UserResponseDto updateUser(Long id, UserRequestDto requestDto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check for duplicate username on other users
        userRepository.findByUsername(requestDto.getUsername())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new DuplicateResourceException("Username is already in use by another user: " + requestDto.getUsername());
                });

        // Check for duplicate email on other users
        userRepository.findByEmail(requestDto.getEmail())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new DuplicateResourceException("Email is already in use by another user: " + requestDto.getEmail());
                });

        existingUser.setUsername(requestDto.getUsername());
        existingUser.setEmail(requestDto.getEmail());
        existingUser.setRole(requestDto.getRole());

        if (requestDto.getPassword() != null && !requestDto.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        }

        User updatedUser = userRepository.save(existingUser);
        return mapToResponseDto(updatedUser);
    }

    /**
     * Delete a user by their ID.
     *
     * @param id user ID
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Helper method to convert User entity to UserResponseDto.
     * Explicitly omits password field.
     */
    private UserResponseDto mapToResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
