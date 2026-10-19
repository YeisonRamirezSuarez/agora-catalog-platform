package com.agora.catalog.empresa.service;

import com.agora.catalog.common.exception.DuplicateResourceException;
import com.agora.catalog.common.random.RandomCodes;
import com.agora.catalog.empresa.dto.response.AdminCreadoResponse;
import com.agora.catalog.empresa.repository.EmpresaRepository;
import com.agora.catalog.usuario.model.Rol;
import com.agora.catalog.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Crea el administrador de una empresa con contraseña temporal, que debe cambiar al entrar. */
@Service
public class CreateAdminEmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateAdminEmpresaService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
                                     PasswordEncoder passwordEncoder) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminCreadoResponse create(long empresaId, String nombre, String email) {
        SaveEmpresaService.require(empresaRepository, empresaId);
        if (usuarioRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo " + email.trim());
        }
        String password = RandomCodes.temporaryPassword();
        long id = usuarioRepository.insert(empresaId, nombre, email, passwordEncoder.encode(password),
                Rol.ADMIN_EMPRESA, true);
        return new AdminCreadoResponse(id, nombre.trim(), email.trim(), password, false, null);
    }
}
