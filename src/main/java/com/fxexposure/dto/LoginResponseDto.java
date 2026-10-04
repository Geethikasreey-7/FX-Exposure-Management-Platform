package com.fxexposure.dto;

import com.fxexposure.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object returned upon successful authentication.
 * Never includes user password.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String token;
    private String username;
    private Role role;

    @Builder.Default
    private String tokenType = "Bearer";
}
