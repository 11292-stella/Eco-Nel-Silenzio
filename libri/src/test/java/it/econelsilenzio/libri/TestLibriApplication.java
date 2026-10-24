package it.econelsilenzio.libri;

import org.springframework.boot.SpringApplication;

public class TestLibriApplication {

	public static void main(String[] args) {
		SpringApplication.from(LibriApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
