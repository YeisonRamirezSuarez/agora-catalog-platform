package com.agora.catalog.correo.service;

import com.agora.catalog.common.encryption.AesEncryption;
import com.agora.catalog.correo.dto.request.ProveedorCorreoRequest;
import com.agora.catalog.correo.model.ProveedorCorreo;
import com.agora.catalog.correo.repository.ProveedorCorreoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuardarProveedorCorreoService {

    private final ProveedorCorreoRepository repository;
    private final AesEncryption aes;

    public GuardarProveedorCorreoService(ProveedorCorreoRepository repository, AesEncryption aes) {
        this.repository = repository;
        this.aes = aes;
    }

    /** Sin contraseña nueva se conserva la guardada: la pantalla nunca la recibe de vuelta. */
    @Transactional
    public ProveedorCorreo save(ProveedorCorreoRequest r) {
        String usuario = r.usuario() == null || r.usuario().isBlank() ? null : r.usuario().trim();
        String cifrada = r.password() == null || r.password().isEmpty()
                ? repository.find().map(ProveedorCorreo::passwordCifrada).orElse(null)
                : aes.encrypt(r.password());
        if (usuario == null) {
            cifrada = null;
        }
        repository.save(new ProveedorCorreo(r.host().trim(), r.puerto(), usuario, cifrada, r.remitenteEmail().trim(),
                r.remitenteNombre().trim(), r.seguridad(), r.activo(), null));
        return repository.find().orElseThrow();
    }
}
