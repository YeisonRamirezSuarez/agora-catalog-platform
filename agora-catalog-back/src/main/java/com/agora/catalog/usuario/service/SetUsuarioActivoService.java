package com.agora.catalog.usuario.service;

import com.agora.catalog.auth.repository.RefreshTokenRepository;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetUsuarioActivoService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public SetUsuarioActivoService(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Desactivar revoca sus sesiones. Nadie se desactiva a sí mismo: quedaría la plataforma sin quién la opere. */
    @Transactional
    public void setActivo(long usuarioId, boolean activo, long solicitanteId) {
        if (!activo && usuarioId == solicitanteId) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
        }
        if (usuarioRepository.setActivo(usuarioId, activo) == 0) {
            throw new ResourceNotFoundException("El usuario no existe");
        }
        if (!activo) {
            refreshTokenRepository.revokeAllByUsuario(usuarioId);
        }
    }
}
