package br.com.solicitacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients
@EnableAspectJAutoProxy
@EnableJpaRepositories(basePackages = "br.com.solicitacao.infrastructure.persistence.repository")
public class SolicitacaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolicitacaoApplication.class, args);
	}
}