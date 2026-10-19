package com.agora.catalog.auth.bootstrap;

import com.agora.catalog.usuario.model.Rol;
import com.agora.catalog.usuario.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el SUPER_ADMIN de {@code settings.superadmin} al arrancar si su correo todavía no existe.
 *
 * <p>No va en una migración porque el correo y la contraseña de una persona real no se versionan:
 * salen del {@code .env}. Nunca modifica un usuario existente, así que cambiar la contraseña desde
 * la pantalla no se pierde al reiniciar.
 */
@Slf4j
@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nombre;
    private final String email;
    private final String password;

    public SuperAdminBootstrap(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                               @Value("${settings.superadmin.nombre:}") String nombre,
                               @Value("${settings.superadmin.email:}") String email,
                               @Value("${settings.superadmin.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || usuarioRepository.existsByEmail(email)) {
            return;
        }
        if (password.length() < 8) {
            log.warn("[AUTH] SUPERADMIN_PASSWORD tiene menos de 8 caracteres: no se crea {}", email);
            return;
        }
        usuarioRepository.insert(null, nombre.isBlank() ? "Super admin" : nombre, email,
                passwordEncoder.encode(password), Rol.SUPER_ADMIN, true);
        log.info("[AUTH] Super admin {} creado; se le pedirá cambiar la contraseña al entrar", email);
    }
}
