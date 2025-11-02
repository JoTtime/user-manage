package com.app.Harvest.Service;

import com.app.Harvest.dto.request.LoginRequest;
import com.app.Harvest.dto.request.SignupRequest;
import com.app.Harvest.dto.response.LoginResponse;
import com.app.Harvest.dto.response.SignupResponse;

public interface AuthService {

    LoginResponse login(LoginRequest loginRequest);

    SignupResponse signup(SignupRequest signupRequest);
}