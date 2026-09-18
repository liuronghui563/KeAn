package com.kean.service;

import com.kean.dto.LoginRequest;
import com.kean.dto.RegisterRequest;
import com.kean.vo.LoginVO;
import com.kean.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    UserVO register(RegisterRequest request);

    LoginVO login(LoginRequest request, HttpServletRequest httpRequest);

    UserVO currentUser();

    void logout(HttpServletRequest httpRequest);
}
