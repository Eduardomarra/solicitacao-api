package br.com.solicitacao.infrastructure.aop;

import br.com.solicitacao.api.annotation.Audit;
import br.com.solicitacao.infrastructure.persistence.entity.AuditLogEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.AuditLogRepository;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Around("@annotation(br.com.solicitacao.api.annotation.Audit)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        Object result = null;

        // Obter informações da anotação
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audit audit = method.getAnnotation(Audit.class);

        String action = audit.action();
        String entity = audit.entity();

        // Obter usuário autenticado
        UUID userId = getCurrentUserId();
        String userRole = getCurrentUserRole();

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Salvar log de auditoria
            try {
                AuditLogEntity auditLog = AuditLogEntity.builder()
                        .userId(userId)
                        .userRole(userRole)
                        .action(action + " - " + entity)
                        .entityId(extractEntityId(joinPoint))
                        .success(success)
                        .errorMessage(errorMessage)
                        .durationMs(duration)
                        .build();

                auditLogRepository.save(auditLog);
                log.info("AUDIT: user={}, action={}, success={}, duration={}ms",
                        userId, action, success, duration);
            } catch (Exception e) {
                log.error("Erro ao salvar log de auditoria: {}", e.getMessage());
            }
        }
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(UserEntity::getId)
                .orElse(null);
    }

    private String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "UNAUTHENTICATED";
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("UNKNOWN");
    }

    private String extractEntityId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return arg.toString();
            }
            if (arg instanceof String) {
                try {
                    UUID.fromString((String) arg);
                    return (String) arg;
                } catch (IllegalArgumentException e) {
                    // Não é um UUID
                }
            }
        }
        return null;
    }
}