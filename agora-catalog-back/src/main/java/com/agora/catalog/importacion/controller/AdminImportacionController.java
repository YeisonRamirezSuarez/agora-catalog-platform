package com.agora.catalog.importacion.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.importacion.dto.ImportacionDtos.ReporteImagenes;
import com.agora.catalog.importacion.dto.ImportacionDtos.ReporteImportacion;
import com.agora.catalog.importacion.service.ExcelProductos;
import com.agora.catalog.importacion.service.FotosZipService;
import com.agora.catalog.importacion.service.ImportarProductosService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/admin/importacion")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class AdminImportacionController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ImportarProductosService importar;
    private final FotosZipService fotos;

    public AdminImportacionController(ImportarProductosService importar, FotosZipService fotos) {
        this.importar = importar;
        this.fotos = fotos;
    }

    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla() {
        return excel("plantilla-productos.xlsx", ExcelProductos.plantilla());
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal UsuarioPrincipal u) {
        return excel("productos.xlsx", importar.exportar(u.empresaId()));
    }

    @PostMapping(value = "/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReporteImportacion previsualizar(@AuthenticationPrincipal UsuarioPrincipal u,
                                            @RequestPart("archivo") MultipartFile archivo) throws IOException {
        return importar.previsualizar(u.empresaId(), archivo.getBytes());
    }

    /** Con errores responde 422 y el reporte: no se guardó nada. */
    @PostMapping(value = "/confirmar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReporteImportacion> confirmar(@AuthenticationPrincipal UsuarioPrincipal u,
                                                        @RequestPart("archivo") MultipartFile archivo) throws IOException {
        ReporteImportacion r = importar.confirmar(u.empresaId(), archivo.getBytes());
        return ResponseEntity.status(r.aplicado() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_CONTENT).body(r);
    }

    @PostMapping(value = "/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReporteImagenes imagenes(@AuthenticationPrincipal UsuarioPrincipal u,
                                    @RequestPart("archivo") MultipartFile archivo) throws IOException {
        return fotos.asignar(u.empresaId(), archivo.getBytes());
    }

    private static ResponseEntity<byte[]> excel(String nombre, byte[] contenido) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(contenido);
    }
}
