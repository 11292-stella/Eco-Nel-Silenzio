package it.econelsilenzio.voce;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Un RabbitMQ vero, in Docker, solo per i test: parte prima e si spegne dopo.
 * @ServiceConnection: Spring usa indirizzo, porta e password di QUESTO container
 * al posto di spring.rabbitmq.* (i test non toccano mai eco-rabbitmq).
 * Si usa con @Import(TestcontainersConfiguration.class) sulla classe di test.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        // la stessa immagine di eco-rabbitmq: e' gia' scaricata, il test parte prima
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management"));
    }
}
