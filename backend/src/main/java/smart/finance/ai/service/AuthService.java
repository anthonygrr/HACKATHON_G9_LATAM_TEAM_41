package smart.finance.ai.service;

import smart.finance.ai.dto.request.LoginRequest;
import smart.finance.ai.dto.request.SignupRequest;
import smart.finance.ai.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse signin(LoginRequest request);
}