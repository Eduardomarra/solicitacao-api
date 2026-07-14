package br.com.solicitacao.core.service;

import br.com.solicitacao.api.dto.request.CoverageRequest;
import br.com.solicitacao.api.dto.request.CreateUserRequest;
import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.infrastructure.config.JwtUtil;
import br.com.solicitacao.infrastructure.persistence.entity.AnalystCoverageEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.AnalystCoverageRepository;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do AdminService")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AnalystCoverageRepository analystCoverageRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private UserEntity user;
    private CreateUserRequest createUserRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = UserEntity.builder()
                .id(userId)
                .name("Analista Teste")
                .email("analista@teste.com")
                .passwordHash("encodedPassword")
                .role(Role.ANALYST)
                .enabled(true)
                .build();

        createUserRequest = CreateUserRequest.builder()
                .name("Analista Teste")
                .email("analista@teste.com")
                .password("123456")
                .role(Role.ANALYST)
                .build();
    }

    @Test
    @DisplayName("Deve criar usuário ANALYST com sucesso")
    void deveCriarUsuarioAnalystComSucesso() {
        // Arrange
        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(createUserRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("jwtToken");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refreshToken");

        // Act
        var response = authService.createUser(createUserRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("analista@teste.com");
        assertThat(response.getRole()).isEqualTo("ANALYST");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Deve criar usuário ADMIN com sucesso")
    void deveCriarUsuarioAdminComSucesso() {
        // Arrange
        createUserRequest.setRole(Role.ADMIN);
        user.setRole(Role.ADMIN);

        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(createUserRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("jwtToken");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refreshToken");

        // Act
        var response = authService.createUser(createUserRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("analista@teste.com");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar usuário com email já cadastrado")
    void deveLancarExcecaoAoCriarUsuarioComEmailJaCadastrado() {
        // Arrange
        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.createUser(createUserRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email já cadastrado");
    }
}