package it.econelsilenzio.voce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan   // trova da sola le classi con @ConfigurationProperties (VoceProperties)
public class VoceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VoceApplication.class, args);
	}
}