package com.agora.catalog.empresa.repository;

import com.agora.catalog.empresa.dto.ConfiguracionDtos.ConfiguracionRequest;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.ConfiguracionResponse;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.HorarioDto;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.Recorte;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.SelloDto;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.WhatsappDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lectura y escritura de la configuración del catálogo de una empresa. */
@Repository
public class ConfiguracionRepository {

    // jsonb reordena las claves al guardarlas: cada una se busca por separado.
    private static final Pattern CLAVE = Pattern.compile("\"(x|y|ancho|alto)\"\\s*:\\s*(-?\\d+)");

    private final JdbcTemplate jdbc;

    public ConfiguracionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ConfiguracionResponse> find(long empresaId) {
        List<WhatsappDto> whatsapps = jdbc.query(
                "SELECT numero, nombre FROM main.empresa_whatsapp WHERE empresa_id = ? ORDER BY orden, id",
                (rs, i) -> new WhatsappDto(rs.getString("numero"), rs.getString("nombre")), empresaId);
        List<SelloDto> sellos = jdbc.query(
                "SELECT icono, texto FROM main.empresa_sello WHERE empresa_id = ? ORDER BY orden, id",
                (rs, i) -> new SelloDto(rs.getString("icono"), rs.getString("texto")), empresaId);
        List<HorarioDto> horario = jdbc.query("""
                SELECT dia, to_char(abre, 'HH24:MI') AS abre, to_char(cierra, 'HH24:MI') AS cierra
                  FROM main.empresa_horario WHERE empresa_id = ? ORDER BY dia
                """, (rs, i) -> new HorarioDto(rs.getInt("dia"), rs.getString("abre"), rs.getString("cierra")), empresaId);
        return jdbc.query("SELECT * FROM main.empresa WHERE id = ?", (rs, i) -> new ConfiguracionResponse(
                rs.getLong("id"), rs.getString("nombre"), rs.getString("slug"), rs.getString("public_hash"),
                "/c/" + rs.getString("slug") + "-" + rs.getString("public_hash"),
                rs.getString("nombre_corto"), rs.getString("lema"),
                rs.getString("logo_url"), rs.getString("portada_url"),
                rs.getString("logo_original_url"), recorte(rs, "logo_recorte"),
                rs.getString("portada_original_url"), recorte(rs, "portada_recorte"),
                rs.getString("portada_antetitulo"), rs.getString("portada_titulo"), rs.getString("portada_titulo_acento"),
                rs.getString("portada_subtitulo"), rs.getString("portada_nota"), rs.getString("portada_sello_texto"),
                rs.getString("catalogo_titulo"), rs.getString("catalogo_acento"), rs.getString("catalogo_subtitulo"),
                rs.getString("colecciones_titulo"), rs.getString("colecciones_texto"), rs.getString("colecciones_nota"),
                rs.getString("visita_nota"), rs.getString("fachada"), rs.getBoolean("adornos"),
                rs.getString("direccion"), rs.getString("horario"), rs.getString("info_domicilios"),
                rs.getBoolean("mostrar_precios"), rs.getBoolean("pedidos_whatsapp"), rs.getBoolean("mostrar_agotados"),
                rs.getString("color_primario"), rs.getString("color_secundario"), whatsapps, sellos, horario), empresaId)
                .stream().findFirst();
    }

    /** Todo menos fotos, hash y estado; los textos vacíos quedan en NULL (el catálogo usa su valor por defecto). */
    public void update(long empresaId, ConfiguracionRequest r) {
        jdbc.update("""
                UPDATE main.empresa SET nombre = ?, slug = ?, nombre_corto = ?, lema = ?,
                       portada_antetitulo = ?, portada_titulo = ?, portada_titulo_acento = ?, portada_subtitulo = ?,
                       portada_nota = ?, portada_sello_texto = ?, catalogo_titulo = ?, catalogo_acento = ?, catalogo_subtitulo = ?,
                       visita_nota = ?, fachada = ?, adornos = ?, direccion = ?, horario = ?, info_domicilios = ?,
                       mostrar_precios = ?, pedidos_whatsapp = ?, mostrar_agotados = ?, color_primario = ?, color_secundario = ?,
                       updated_at = now()
                 WHERE id = ?
                """, r.nombre().trim(), r.slug(), t(r.nombreCorto()), t(r.lema()),
                t(r.portadaAntetitulo()), t(r.portadaTitulo()), t(r.portadaTituloAcento()), t(r.portadaSubtitulo()),
                t(r.portadaNota()), t(r.portadaSelloTexto()), t(r.catalogoTitulo()), t(r.catalogoAcento()), t(r.catalogoSubtitulo()),
                t(r.visitaNota()), t(r.fachada()), r.adornos(), t(r.direccion()), t(r.horario()), t(r.infoDomicilios()),
                r.mostrarPrecios(), r.pedidosWhatsapp(), r.mostrarAgotados(),
                r.colorPrimario().toUpperCase(), r.colorSecundario().toUpperCase(), empresaId);
        jdbc.update("DELETE FROM main.empresa_whatsapp WHERE empresa_id = ?", empresaId);
        for (int i = 0; i < r.whatsapps().size(); i++) {
            WhatsappDto w = r.whatsapps().get(i);
            jdbc.update("INSERT INTO main.empresa_whatsapp (empresa_id, numero, nombre, orden) VALUES (?, ?, ?, ?)",
                    empresaId, w.numero(), t(w.nombre()), i + 1);
        }
        jdbc.update("DELETE FROM main.empresa_sello WHERE empresa_id = ?", empresaId);
        for (int i = 0; i < r.sellos().size(); i++) {
            SelloDto s = r.sellos().get(i);
            jdbc.update("INSERT INTO main.empresa_sello (empresa_id, icono, texto, orden) VALUES (?, ?, ?, ?)",
                    empresaId, s.icono(), s.texto().trim(), i + 1);
        }
        jdbc.update("DELETE FROM main.empresa_horario WHERE empresa_id = ?", empresaId);
        for (HorarioDto h : r.horarioSemana()) {
            jdbc.update("INSERT INTO main.empresa_horario (empresa_id, dia, abre, cierra) VALUES (?, ?, ?::time, ?::time)",
                    empresaId, h.dia(), h.abre(), h.cierra());
        }
    }

    public void updateColecciones(long empresaId, String titulo, String texto, String nota) {
        jdbc.update("UPDATE main.empresa SET colecciones_titulo = ?, colecciones_texto = ?, colecciones_nota = ?, updated_at = now() WHERE id = ?",
                titulo.trim(), t(texto), t(nota), empresaId);
    }

    /** {@code tipo}: "logo" o "portada". */
    public void setFoto(long empresaId, String tipo, String url, String originalUrl, Recorte recorte) {
        String json = recorte == null ? null
                : "{\"x\":%d,\"y\":%d,\"ancho\":%d,\"alto\":%d}".formatted(recorte.x(), recorte.y(), recorte.ancho(), recorte.alto());
        jdbc.update("UPDATE main.empresa SET %1$s_url = ?, %1$s_original_url = ?, %1$s_recorte = ?::jsonb, updated_at = now() WHERE id = ?"
                .formatted(columna(tipo)), url, originalUrl, json, empresaId);
    }

    private static String columna(String tipo) {
        if (!tipo.equals("logo") && !tipo.equals("portada")) throw new IllegalArgumentException("Tipo de foto no válido");
        return tipo;
    }

    private static Recorte recorte(ResultSet rs, String columna) throws SQLException {
        String json = rs.getString(columna);
        if (json == null) return null;
        java.util.Map<String, Integer> v = new java.util.HashMap<>();
        Matcher m = CLAVE.matcher(json);
        while (m.find()) v.put(m.group(1), Integer.parseInt(m.group(2)));
        return v.size() == 4 ? new Recorte(v.get("x"), v.get("y"), v.get("ancho"), v.get("alto")) : null;
    }

    private static String t(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
