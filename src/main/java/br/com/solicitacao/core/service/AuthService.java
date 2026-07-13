package br.com.solicitacao.core.service;

import br.com.solicitacao.api.dto.request.CreateUserRequest;
import br.com.solicitacao.api.dto.request.LoginRequest;
import br.com.solicitacao.api.dto.request.RegisterRequest;
import br.com.solicitacao.api.dto.response.AuthResponse;
import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.infrastructure.config.JwtUtil;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registrando usuário: email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado");
        }

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CLIENT)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("Usuário registrado com sucesso: id={}", user.getId());

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return buildAuthResponse(token, refreshToken, user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Tentando login: email={}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserEntity user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

            if (!user.getEnabled()) {
                throw new BusinessException("Usuário desabilitado");
            }

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

            log.info("Login realizado com sucesso: email={}", user.getEmail());
            return buildAuthResponse(token, refreshToken, user);
        } catch (Exception e) {
            log.error("Erro no login: {}", e.getMessage());
            throw new BusinessException("Credenciais inválidas");
        }
    }

    @Transactional
    public AuthResponse createUser(CreateUserRequest request) {
        log.info("Criando usuário: email={}, role={}", request.getEmail(), request.getRole());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado");
        }

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("Usuário criado com sucesso: id={}", user.getId());

        // Gerar token
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return buildAuthResponse(token, refreshToken, user);
    }

    private AuthResponse buildAuthResponse(String token, String refreshToken, UserEntity user) {
        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}