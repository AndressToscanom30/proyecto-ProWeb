package com.cronos.gestiontributaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación de gestión tributaria.
 *
 * <p>Spring Boot utiliza esta clase para iniciar el contexto, cargar la
 * configuración de seguridad, la conexión a MongoDB y los controladores web.</p>
 */
@SpringBootApplication
public class GestionTributariaApplication {

	/**
	 * Inicia la aplicación Spring Boot.
	 *
	 * @param args argumentos de arranque de la JVM
	 */
	public static void main(String[] args) {
		SpringApplication.run(GestionTributariaApplication.class, args);
	}

}
