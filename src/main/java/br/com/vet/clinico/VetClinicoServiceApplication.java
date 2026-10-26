package br.com.vet.clinico;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VetClinicoServiceApplication {

	public static void main(String[] args) {
		// Carrega o .env e define as chaves automaticamente no System.setProperty
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .systemProperties()
                .load();
		SpringApplication.run(VetClinicoServiceApplication.class, args);
	}

}
