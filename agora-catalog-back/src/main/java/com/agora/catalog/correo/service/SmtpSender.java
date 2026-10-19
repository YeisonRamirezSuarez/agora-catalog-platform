package com.agora.catalog.correo.service;

import com.agora.catalog.correo.model.ProveedorCorreo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Envía un correo HTML con la configuración que está en la base en ese momento: cambiar el
 * proveedor desde la pantalla no exige reiniciar. Aparte de {@link EnviarCorreoService} para poder
 * reemplazarlo en las pruebas sin un servidor SMTP.
 */
@Component
public class SmtpSender {

    private static final String TIMEOUT_MS = "10000";

    public void send(ProveedorCorreo proveedor, String password, String destinatario, String asunto, String html)
            throws MessagingException, UnsupportedEncodingException {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(proveedor.host());
        sender.setPort(proveedor.puerto());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        boolean auth = proveedor.usuario() != null && !proveedor.usuario().isBlank();
        if (auth) {
            sender.setUsername(proveedor.usuario());
            sender.setPassword(password);
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        props.put("mail.smtp.timeout", TIMEOUT_MS);
        props.put("mail.smtp.writetimeout", TIMEOUT_MS);
        switch (proveedor.seguridad()) {
            case STARTTLS -> {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
            }
            case SSL -> props.put("mail.smtp.ssl.enable", "true");
            case NINGUNA -> { }
        }

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(proveedor.remitenteEmail(), proveedor.remitenteNombre());
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(html, true);
        sender.send(message);
    }
}
