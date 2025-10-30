package com.s3manager.service;

import com.s3manager.domain.entity.RefreshToken;
import com.s3manager.domain.entity.User;
import com.s3manager.domain.entity.UserRole;
import com.s3manager.dto.auth.*;
import com.s3manager.dto.user.UserDTO;
import com.s3manager.exception.BadRequestException;
import com.s3manager.exception.UnauthorizedException;
import com.s3manager.repository.RefreshTokenRepository;
import com.s3manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        // Validate username and email uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Save refresh token
        saveRefreshToken(user, refreshToken);

        log.info("User registered successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .user(mapToUserDTO(user))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Check if account is locked
        if (!user.getAccountNonLocked()) {
            throw new UnauthorizedException("Account is locked. Please contact administrator.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Reset failed login attempts
            if (user.getFailedLoginAttempts() > 0) {
                userRepository.updateFailedLoginAttempts(user.getId(), 0);
            }

            // Update last login timestamp
            userRepository.updateLastLoginAt(user.getId());

            // Generate tokens
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            // Revoke old refresh tokens and save new one
            refreshTokenRepository.revokeAllByUser(user);
            saveRefreshToken(user, refreshToken);

            // Log successful login
            auditService.logLoginSuccess(user);

            log.info("User logged in successfully: {}", user.getUsername());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtService.getExpirationTime())
                    .user(mapToUserDTO(user))
                    .build();

        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            userRepository.updateFailedLoginAttempts(user.getId(), attempts);

            // Lock account after 5 failed attempts
            if (attempts >= 5) {
                user.setAccountNonLocked(false);
                userRepository.save(user);
                log.warn("Account locked due to multiple failed login attempts: {}", user.getUsername());
            }

            // Log failed login
            auditService.logLoginFailure(user);

            throw new UnauthorizedException("Invalid credentials");
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenStr = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenStr)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        // Revoke old refresh token and save new one
        refreshTokenRepository.revokeByToken(refreshTokenStr);
        saveRefreshToken(user, newRefreshToken);

        log.info("Tokens refreshed for user: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .user(mapToUserDTO(user))
                .build();
    }

    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        refreshTokenRepository.revokeAllByUser(user);
        auditService.logLogout(user);

        log.info("User logged out: {}", username);
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusSeconds(
                        jwtService.getRefreshExpirationTime() / 1000
                ))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}