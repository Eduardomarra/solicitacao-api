package br.com.solicitacao.core.service;

import br.com.solicitacao.api.dto.request.LoginRequest;
import br.com.solicitacao.api.dto.request.RegisterRequest;
import br.com.solicitacao.api.dto.response.AuthResponse;
import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.infrastructure.config.JwtUtil;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserEntity user;
    private String email;
    private String password;
    private String encodedPassword;
    private UUID userId;

    @BeforeEach
    void setUp() {
        email = "usuario@teste.com";
        password = "senha123";
        encodedPassword = "encodedPasswordHash";
        userId = UUID.randomUUID();

        registerRequest = RegisterRequest.builder()
                .name("Usuário Teste")
                .email(email)
                .password(password)
                .build();

        loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        user = UserEntity.builder()
                .id(userId)
                .name("Usuário Teste")
                .email(email)
                .passwordHash(encodedPassword)
                .role(Role.CLIENT)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve registrar usuário com sucesso")
    void deveRegistrarUsuarioComSucesso() {
        // Arrange
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(jwtUtil.generateToken(email, Role.CLIENT.name())).thenReturn("jwtToken");
        when(jwtUtil.generateRefreshToken(email)).thenReturn("refreshToken");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getRole()).isEqualTo("CLIENT");
        assertThat(response.getAccessToken()).isEqualTo("jwtToken");

        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(UserEntity.class));
        verify(jwtUtil).generateToken(email, Role.CLIENT.name());
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar com email já cadastrado")
    void deveLancarExcecaoAoRegistrarComEmailJaCadastrado() {
        // Arrange
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email já cadastrado");

        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar com nome vazio")
    void deveLancarExcecaoAoRegistrarComNomeVazio() {
        // Arrange
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .name("")
                .email(email)
                .password(password)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> authService.register(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nome não pode ser vazio");

        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("Deve fazer login com sucesso")
    void deveFazerLoginComSucesso() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(email, user.getRole().name())).thenReturn("jwtToken");
        when(jwtUtil.generateRefreshToken(email)).thenReturn("refreshToken");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getRole()).isEqualTo("CLIENT");
        assertThat(response.getAccessToken()).isEqualTo("jwtToken");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(email);
        verify(jwtUtil).generateToken(email, user.getRole().name());
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com credenciais inválidas")
    void deveLancarExcecaoAoFazerLoginComCredenciaisInvalidas() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Credenciais inválidas");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com usuário desabilitado")
    void deveLancarExcecaoAoFazerLoginComUsuarioDesabilitado() {
        // Arrange
        user.setEnabled(false);
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Usuário desabilitado");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com email nulo")
    void deveLancarExcecaoAoFazerLoginComEmailNulo() {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email(null)
                .password(password)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> authService.login(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email não pode ser nulo ou vazio");

        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com senha nula")
    void deveLancarExcecaoAoFazerLoginComSenhaNula() {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email(email)
                .password(null)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> authService.login(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha não pode ser nula ou vazia");

        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com email vazio")
    void deveLancarExcecaoAoFazerLoginComEmailVazio() {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("")
                .password(password)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> authService.login(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email não pode ser nulo ou vazio");

        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}