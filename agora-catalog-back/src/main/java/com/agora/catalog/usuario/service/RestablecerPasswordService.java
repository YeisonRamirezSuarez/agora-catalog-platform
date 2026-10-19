package com.agora.catalog.usuario.service;

import com.agora.catalog.auth.repository.RefreshTokenRepository;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.common.random.RandomCodes;
import com.agora.catalog.empresa.dto.response.AdminCreadoResponse;
import com.agora.catalog.usuario.model.Rol;
import com.agora.catalog.usuario.model.Usuario;
import com.agora.catalog.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El administrador de un negocio olvidó su contraseña: el super admin le pone una temporal nueva.
 * Sus sesiones abiertas se cierran, por si alguien más la conocía.
 */
@Service
public class RestablecerPasswordService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public RestablecerPasswordService(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository,
                                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** La empresa va aparte: el correo la nombra, la respuesta no la lleva. */
    public record Restablecida(AdminCreadoResponse admin, long empresaId, String empresaNombre) {
    }

    @Transactional
    public Restablecida restablecer(long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));
        if (u.rol() != Rol.ADMIN_EMPRESA) {
            throw new IllegalArgumentException("La contraseña del super admin se cambia desde su propia cuenta");
        }
        String password = RandomCodes.temporaryPassword();
        usuarioRepository.setPasswordTemporal(usuarioId, passwordEncoder.encode(password));
        refreshTokenRepository.revokeAllByUsuario(usuarioId);
        return new Restablecida(new AdminCreadoResponse(u.id(), u.nombre(), u.email(), password, false, null),
                u.empresaId(), u.empresaNombre());
    }
}
