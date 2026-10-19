package com.agora.catalog.empresa.service;

import com.agora.catalog.auth.repository.RefreshTokenRepository;
import com.agora.catalog.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetEmpresaActivaService {

    private final EmpresaRepository empresaRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public SetEmpresaActivaService(EmpresaRepository empresaRepository, RefreshTokenRepository refreshTokenRepository) {
        this.empresaRepository = empresaRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Desactivar revoca las sesiones de sus usuarios: el acceso se corta cuando vence su token corto. */
    @Transactional
    public void setActiva(long id, boolean activa) {
        SaveEmpresaService.require(empresaRepository, id);
        empresaRepository.setActiva(id, activa);
        if (!activa) {
            refreshTokenRepository.revokeAllByEmpresa(id);
        }
    }
}
