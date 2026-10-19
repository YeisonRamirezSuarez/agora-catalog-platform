package com.agora.catalog.auth.service;

import com.agora.catalog.auth.dto.response.LoginResponse;
import com.agora.catalog.auth.dto.response.SesionUsuarioResponse;
import com.agora.catalog.usuario.model.Usuario;
import com.agora.catalog.usuario.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login, refresh y logout. Login y refresh rechazan al usuario inactivo o de una empresa inactiva. */
@Slf4j
@Service
public class LoginService {

    static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos";
    static final String CUENTA_DESACTIVADA = "Tu cuenta está desactivada. Escríbele a quien administra la plataforma.";
    static final String SESION_VENCIDA = "Tu sesión venció. Ingresa de nuevo.";

    // Hash de una contraseña cualquiera: con un correo que no existe se compara igual contra algo,
    // para que el tiempo de respuesta no revele qué correos tienen cuenta.
    private static final String DUMMY_HASH = "$2a$10$/gAsXoNT68ZmFyb1UYN5j.Q.EKMERhZZxdg.YfERq5iNv0F9QH7QO";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public LoginService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public LoginResponse login(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        String hash = usuario == null ? DUMMY_HASH : usuario.passwordHash();
        if (!passwordEncoder.matches(password, hash) || usuario == null) {
            log.warn("[AUTH] Login fallido para {}", email);
            throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
        }
        // Solo después de la contraseña correcta: si no, cualquiera sabría qué cuentas están desactivadas.
        if (!usuario.puedeIngresar()) {
            throw new BadCredentialsException(CUENTA_DESACTIVADA);
        }
        return session(usuario);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        long usuarioId = refreshTokenService.consume(refreshToken)
                .orElseThrow(() -> new BadCredentialsException(SESION_VENCIDA));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .filter(Usuario::puedeIngresar)
                .orElseThrow(() -> new BadCredentialsException(CUENTA_DESACTIVADA));
        return session(usuario);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private LoginResponse session(Usuario usuario) {
        return new LoginResponse(
                jwtService.generateAccessToken(usuario),
                refreshTokenService.issue(usuario.id()),
                jwtService.accessSeconds(),
                SesionUsuarioResponse.of(usuario));
    }
}
