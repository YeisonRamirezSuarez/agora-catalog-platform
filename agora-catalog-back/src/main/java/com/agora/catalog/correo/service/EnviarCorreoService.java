package com.agora.catalog.correo.service;

import com.agora.catalog.common.encryption.AesEncryption;
import com.agora.catalog.common.exception.RootCauseMessage;
import com.agora.catalog.correo.model.ProveedorCorreo;
import com.agora.catalog.correo.repository.CorreoEnvioRepository;
import com.agora.catalog.correo.repository.ProveedorCorreoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Envía un correo con el proveedor activo y lo deja en la bitácora. Nunca lanza: un correo que no
 * sale no debe deshacer lo que lo originó (el administrador ya quedó creado); el resultado dice si
 * salió y por qué no.
 */
@Slf4j
@Service
public class EnviarCorreoService {

    static final String SIN_PROVEEDOR = "No hay un proveedor de correo activo. Configúralo en Correo.";

    public record Resultado(boolean enviado, String error) {
    }

    private final ProveedorCorreoRepository proveedorRepository;
    private final CorreoEnvioRepository envioRepository;
    private final SmtpSender smtpSender;
    private final AesEncryption aes;

    public EnviarCorreoService(ProveedorCorreoRepository proveedorRepository, CorreoEnvioRepository envioRepository,
                               SmtpSender smtpSender, AesEncryption aes) {
        this.proveedorRepository = proveedorRepository;
        this.envioRepository = envioRepository;
        this.smtpSender = smtpSender;
        this.aes = aes;
    }

    public boolean hayProveedorActivo() {
        return proveedorRepository.find().filter(ProveedorCorreo::activo).isPresent();
    }

    public Resultado enviar(Long empresaId, String tipo, String destinatario, String asunto, String html) {
        ProveedorCorreo proveedor = proveedorRepository.find().filter(ProveedorCorreo::activo).orElse(null);
        Resultado resultado;
        if (proveedor == null) {
            resultado = new Resultado(false, SIN_PROVEEDOR);
        } else {
            try {
                String password = proveedor.passwordCifrada() == null ? null : aes.decrypt(proveedor.passwordCifrada());
                smtpSender.send(proveedor, password, destinatario, asunto, html);
                resultado = new Resultado(true, null);
            } catch (Exception e) {
                log.warn("[CORREO] No salió {} a {}: {}", tipo, destinatario, RootCauseMessage.from(e));
                resultado = new Resultado(false, RootCauseMessage.from(e));
            }
        }
        envioRepository.insert(empresaId, destinatario, asunto, tipo, resultado.enviado(), resultado.error());
        return resultado;
    }
}
