package it.econelsilenzio.voce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)   // RabbitMQ di prova, non eco-rabbitmq
class VoceApplicationTests {

	@Test
	void contextLoads() {
	}

}
