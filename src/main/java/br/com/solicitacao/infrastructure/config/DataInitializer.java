package br.com.solicitacao.infrastructure.config;

import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("=== INICIALIZANDO DADOS ===");
        createAdminUser();
        log.info("=== DADOS INICIALIZADOS COM SUCESSO ===");
    }

    private void createAdminUser() {
        String adminEmail = "admin@email.com";

        // Verificar se o admin já existe
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin já existe: {}", adminEmail);
            return;
        }

        // Criar admin com senha fixa
        String adminPassword = "admin123";
        String passwordHash = passwordEncoder.encode(adminPassword);

        UserEntity admin = UserEntity.builder()
                .name("Administrador")
                .email(adminEmail)
                .passwordHash(passwordHash)
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(admin);
        log.info("✅ Admin criado com sucesso!");
        log.info("   Email: {}", adminEmail);
        log.info("   Senha: {}", adminPassword);
        log.info("   Role: {}", Role.ADMIN);
    }
}