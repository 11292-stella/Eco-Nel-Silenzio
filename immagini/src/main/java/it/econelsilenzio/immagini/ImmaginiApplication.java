package it.econelsilenzio.immagini;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan   // trova da sola le classi con @ConfigurationProperties (ImmaginiProperties)
public class ImmaginiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImmaginiApplication.class, args);
	}
}
