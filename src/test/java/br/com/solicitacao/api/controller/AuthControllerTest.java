package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.LoginRequest;
import br.com.solicitacao.api.dto.request.RegisterRequest;
import br.com.solicitacao.api.dto.response.AuthResponse;
import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.core.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do AuthController")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private String email;
    private String password;
    private String token;
    private UUID userId;

    @BeforeEach
    void setUp() {
        email = "usuario@teste.com";
        password = "senha123";
        token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvQHRlc3RlLmNvbSJ9...";
        userId = UUID.randomUUID();

        loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        registerRequest = RegisterRequest.builder()
                .name("Usuário Teste")
                .email(email)
                .password(password)
                .build();
    }

    // ========== TESTES DE LOGIN ==========

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar token + dados do usuário")
    void deveRealizarLoginComSucesso() {
        // Arrange
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(token)
                .refreshToken("refreshToken")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .email(email)
                .name("Usuário Teste")
                .role("CLIENT")
                .build();

        when(authService.login(loginRequest)).thenReturn(authResponse);

        // Act
        ResponseEntity<AuthResponse> responseEntity = authController.login(loginRequest);

        // Assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getEmail()).isEqualTo(email);
        assertThat(responseEntity.getBody().getRole()).isEqualTo("CLIENT");
        assertThat(responseEntity.getBody().getAccessToken()).isEqualTo(token);

        verify(authService, times(1)).login(loginRequest);
    }

    @Test
    @DisplayName("Deve lançar exceção quando credenciais são inválidas no login")
    void deveLancarExcecaoQuandoCredenciaisInvalidasNoLogin() {
        // Arrange
        when(authService.login(loginRequest))
                .thenThrow(new BusinessException("Credenciais inválidas"));

        // Act & Assert
        assertThatThrownBy(() -> authController.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Credenciais inválidas");

        verify(authService, times(1)).login(loginRequest);
    }

    @Test
    @DisplayName("Deve lançar exceção quando email é nulo no login")
    void deveLancarExcecaoQuandoEmailNuloNoLogin() {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email(null)
                .password(password)
                .build();

        when(authService.login(invalidRequest))
                .thenThrow(new IllegalArgumentException("Email não pode ser nulo ou vazio"));

        // Act & Assert
        assertThatThrownBy(() -> authController.login(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email não pode ser nulo ou vazio");

        verify(authService, times(1)).login(invalidRequest);
    }

    @Test
    @DisplayName("Deve lançar exceção quando senha é nula no login")
    void deveLancarExcecaoQuandoSenhaNulaNoLogin() {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email(email)
                .password(null)
                .build();

        when(authService.login(invalidRequest))
                .thenThrow(new IllegalArgumentException("Senha não pode ser nula ou vazia"));

        // Act & Assert
        assertThatThrownBy(() -> authController.login(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha não pode ser nula ou vazia");

        verify(authService, times(1)).login(invalidRequest);
    }

    @Test
    @DisplayName("Deve lançar exceção quando email é vazio no login")
    void deveLancarExcecaoQuandoEmailVazioNoLogin() {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("")
                .password(password)
                .build();

        when(authService.login(invalidRequest))
                .thenThrow(new IllegalArgumentException("Email não pode ser nulo ou vazio"));

        // Act & Assert
        assertThatThrownBy(() -> authController.login(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email não pode ser nulo ou vazio");

        verify(authService, times(1)).login(invalidRequest);
    }

    // ========== TESTES DE REGISTER ==========

    @Test
    @DisplayName("Deve registrar usuário com sucesso")
    void deveRegistrarUsuarioComSucesso() {
        // Arrange
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(token)
                .refreshToken("refreshToken")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .email(email)
                .name("Usuário Teste")
                .role("CLIENT")
                .build();

        when(authService.register(registerRequest)).thenReturn(authResponse);

        // Act
        ResponseEntity<AuthResponse> responseEntity = authController.register(registerRequest);

        // Assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getEmail()).isEqualTo(email);
        assertThat(responseEntity.getBody().getRole()).isEqualTo("CLIENT");
        assertThat(responseEntity.getBody().getAccessToken()).isEqualTo(token);

        verify(authService, times(1)).register(registerRequest);
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar com email já cadastrado")
    void deveLancarExcecaoAoRegistrarComEmailJaCadastrado() {
        // Arrange
        when(authService.register(registerRequest))
                .thenThrow(new BusinessException("Email já cadastrado"));

        // Act & Assert
        assertThatThrownBy(() -> authController.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email já cadastrado");

        verify(authService, times(1)).register(registerRequest);
    }
}