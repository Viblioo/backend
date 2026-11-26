package com.revibe.backend.application.ports.output;

public interface PasswordHasherPort {

    String hash(String rawPassword);

    /** Comprueba la contraseña contra un hash guardado. Tiempo constante respecto al contenido. */
    boolean matches(String rawPassword, String hash);
}
