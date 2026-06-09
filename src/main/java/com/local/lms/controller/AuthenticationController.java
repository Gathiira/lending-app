package com.local.lms.controller;

import com.local.lms.dto.request.LoginRequest;
import com.local.lms.dto.response.AuthResponse;
import com.local.lms.dto.response.ResponseResult;
import com.local.lms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth management APIs")
public class AuthenticationController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "login")
    public ResponseResult<AuthResponse> login(@Validated @RequestBody LoginRequest request) {
        return ResponseResult.success("Login successfully", authService.login(request));
    }
}
