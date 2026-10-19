package com.agora.catalog.correo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Plantillas HTML de los correos de la plataforma. Estilos en línea y tablas: es lo único que
 * respetan Gmail y Outlook. Todo valor que entra se escapa.
 */
@Component
public class PlantillasCorreo {

    public record Correo(String asunto, String html) {
    }

    private final String frontendUrl;

    public PlantillasCorreo(@Value("${settings.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public Correo bienvenida(String nombre, String empresa, String email, String passwordTemporal) {
        String login = frontendUrl + "/login";
        String cuerpo = """
                <p style="margin:0 0 16px">Hola, %s:</p>
                <p style="margin:0 0 16px">Ya puedes administrar el catálogo de <b>%s</b> en Agorá Catalog:
                productos, precios, ofertas y fotos.</p>
                <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 20px;background:#F6F2EA;border-radius:10px;width:100%%">
                  <tr><td style="padding:16px 18px;font-size:14px;line-height:1.7">
                    Correo: <b>%s</b><br>
                    Contraseña temporal: <b style="font-family:Consolas,monospace;font-size:16px">%s</b>
                  </td></tr>
                </table>
                <p style="margin:0 0 24px">Al entrar te pediremos que elijas una contraseña propia.</p>
                %s
                """.formatted(esc(nombre), esc(empresa), esc(email), esc(passwordTemporal),
                boton("Ingresar a mi panel", login));
        // Sin saltos de línea en el asunto: es una cabecera del correo.
        return new Correo("Tu acceso a Agorá Catalog — " + empresa.replaceAll("[\r\n]+", " "), marco(cuerpo));
    }

    public Correo prueba() {
        String cuerpo = """
                <p style="margin:0 0 16px">Este es un correo de prueba.</p>
                <p style="margin:0">Si lo estás leyendo, el proveedor de correo de Agorá Catalog quedó bien configurado.</p>
                """;
        return new Correo("Prueba del proveedor de correo — Agorá Catalog", marco(cuerpo));
    }

    private static String boton(String texto, String url) {
        return """
                <a href="%s" style="display:inline-block;background:#1C1814;color:#ffffff;text-decoration:none;font-weight:600;padding:12px 22px;border-radius:10px">%s</a>
                """.formatted(esc(url), esc(texto));
    }

    private static String marco(String cuerpo) {
        return """
                <!doctype html>
                <html lang="es"><body style="margin:0;padding:24px 12px;background:#F6F2EA;font-family:Arial,Helvetica,sans-serif;color:#1C1814">
                <table role="presentation" cellpadding="0" cellspacing="0" align="center" style="max-width:520px;width:100%%;background:#ffffff;border:1px solid #ECE6DC;border-radius:16px">
                  <tr><td style="padding:28px 28px 8px;font-family:Georgia,'Times New Roman',serif;font-size:22px;font-weight:700">Agorá <span style="color:#B07A1E;font-style:italic">Catalog</span></td></tr>
                  <tr><td style="padding:12px 28px 28px;font-size:15px;line-height:1.55">%s</td></tr>
                </table>
                <p style="text-align:center;color:#776D63;font-size:12px;margin:16px 0 0">Correo automático de Agorá Catalog.</p>
                </body></html>
                """.formatted(cuerpo);
    }

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
