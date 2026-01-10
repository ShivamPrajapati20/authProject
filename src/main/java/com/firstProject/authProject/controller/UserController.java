package com.firstProject.authProject.controller;

import com.firstProject.authProject.dto.response.UserResponse;
import com.firstProject.authProject.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        var user = principal.getUser();

        var roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEnabled(),
                roles
        );
    }
}
