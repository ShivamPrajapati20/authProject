package com.firstProject.authProject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private boolean enabled;
    private Set<String> roles;
}
