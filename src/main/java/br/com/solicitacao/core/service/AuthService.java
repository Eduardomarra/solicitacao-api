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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

        // Validar campos
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome não pode ser vazio");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email não pode ser nulo ou vazio");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou vazia");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Senha deve ter no mínimo 6 caracteres");
        }

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

        // Validar campos
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email não pode ser nulo ou vazio");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou vazia");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserEntity user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

            if (!user.getEnabled()) {
                throw new DisabledException("Usuário desabilitado");
            }

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

            log.info("Login realizado com sucesso: email={}", user.getEmail());
            return buildAuthResponse(token, refreshToken, user);

        } catch (BadCredentialsException e) {
            log.error("Credenciais inválidas para email: {}", request.getEmail());
            throw new BusinessException("Credenciais inválidas");
        } catch (DisabledException e) {
            log.error("Usuário desabilitado: {}", request.getEmail());
            throw new BusinessException("Usuário desabilitado");
        } catch (UsernameNotFoundException e) {
            log.error("Usuário não encontrado: {}", request.getEmail());
            throw new BusinessException("Credenciais inválidas");
        } catch (Exception e) {
            log.error("Erro no login: {}", e.getMessage());
            throw new BusinessException("Credenciais inválidas");
        }
    }

    @Transactional
    public AuthResponse createUser(CreateUserRequest request) {
        log.info("Criando usuário: email={}, role={}", request.getEmail(), request.getRole());

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome não pode ser vazio");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email não pode ser nulo ou vazio");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou vazia");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Senha deve ter no mínimo 6 caracteres");
        }

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