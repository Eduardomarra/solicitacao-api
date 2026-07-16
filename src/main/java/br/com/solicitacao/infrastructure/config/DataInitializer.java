package br.com.solicitacao.infrastructure.config;

import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
        createAnalystUser();
        createClientUsers();
        log.info("=== DADOS INICIALIZADOS COM SUCESSO ===");
    }

    private void createAdminUser() {
        String adminEmail = "admin@email.com";

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin já existe: {}", adminEmail);
            return;
        }

        String adminPassword = "admin123";
        String passwordHash = passwordEncoder.encode(adminPassword);

        log.info("Hash gerado para admin: {}", passwordHash);

        UserEntity admin = UserEntity.builder()
                .name("Administrador")
                .email(adminEmail)
                .passwordHash(passwordHash)
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.info("✅ Admin criado com sucesso!");
        log.info("   Email: {}", adminEmail);
        log.info("   Senha: {}", adminPassword);
    }

    private void createAnalystUser() {
        String analystEmail = "analyst@email.com";

        if (userRepository.findByEmail(analystEmail).isPresent()) {
            log.info("Analyst já existe: {}", analystEmail);
            return;
        }

        String analystPassword = "123456";
        String passwordHash = passwordEncoder.encode(analystPassword);

        UserEntity analyst = UserEntity.builder()
                .name("Analista 1")
                .email(analystEmail)
                .passwordHash(passwordHash)
                .role(Role.ANALYST)
                .enabled(true)
                .build();

        userRepository.save(analyst);
        log.info("✅ Analyst criado com sucesso!");
        log.info("   Email: {}", analystEmail);
        log.info("   Senha: {}", analystPassword);
    }

    private void createClientUsers() {
        String[] clients = {
                "João Silva,joao@email.com,123456",
                "Maria Souza,maria@email.com,123456",
                "Pedro Santos,pedro@email.com,123456"
        };

        for (String clientData : clients) {
            String[] parts = clientData.split(",");
            String name = parts[0];
            String email = parts[1];
            String password = parts[2];

            if (userRepository.findByEmail(email).isPresent()) {
                log.info("Cliente já existe: {}", email);
                continue;
            }

            String passwordHash = passwordEncoder.encode(password);

            UserEntity client = UserEntity.builder()
                    .name(name)
                    .email(email)
                    .passwordHash(passwordHash)
                    .role(Role.CLIENT)
                    .enabled(true)
                    .build();

            userRepository.save(client);
            log.info("✅ Cliente criado: {} / {}", email, password);
        }
    }
}