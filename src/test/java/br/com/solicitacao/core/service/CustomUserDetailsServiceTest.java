package br.com.solicitacao.core.service;

import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private UserEntity userEntity;
    private String email;

    @BeforeEach
    void setUp() {
        email = "usuario@teste.com";
        userEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("senhaCodificada")
                .role(Role.CLIENT)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Deve retornar UserDetails com sucesso quando usuário for encontrado")
    void deveRetornarUserDetailsComSucesso() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("senhaCodificada");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CLIENT");

        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando usuário não existir")
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado com email: " + email);

        verify(userRepository).findByEmail(email);
    }
}