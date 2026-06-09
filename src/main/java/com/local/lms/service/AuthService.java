package com.local.lms.service;

import com.local.lms.dto.request.LoginRequest;
import com.local.lms.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
