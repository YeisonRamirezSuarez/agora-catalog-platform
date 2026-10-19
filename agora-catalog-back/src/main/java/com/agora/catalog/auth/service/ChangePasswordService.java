package com.agora.catalog.auth.service;

import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.usuario.model.Usuario;
import com.agora.catalog.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Cambia la contraseña propia y apaga el aviso de contraseña temporal. */
    @Transactional
    public void change(long usuarioId, String actual, String nueva) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));
        // 400 y no 401: un 401 haría que el front intente renovar la sesión y la cierre.
        if (!passwordEncoder.matches(actual, usuario.passwordHash())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        if (actual.equals(nueva)) {
            throw new IllegalArgumentException("La contraseña nueva debe ser distinta de la actual");
        }
        usuarioRepository.updatePassword(usuarioId, passwordEncoder.encode(nueva));
    }
}
