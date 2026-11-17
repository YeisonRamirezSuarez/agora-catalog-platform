package com.agora.catalog.modulo.controller;

import com.agora.catalog.common.dto.ActivoRequest;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.empresa.repository.EmpresaRepository;
import com.agora.catalog.modulo.dto.ModuloResponse;
import com.agora.catalog.modulo.model.Modulo;
import com.agora.catalog.modulo.repository.ModuloRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** El super admin enciende y apaga los módulos de cada empresa. */
@RestController
@RequestMapping("/superadmin/empresas/{empresaId}/modulos")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminModuloController {

    private final ModuloRepository modulos;
    private final EmpresaRepository empresas;

    public SuperAdminModuloController(ModuloRepository modulos, EmpresaRepository empresas) {
        this.modulos = modulos;
        this.empresas = empresas;
    }

    /** Todos los módulos, en el orden de {@link Modulo}, con si la empresa los tiene. */
    @GetMapping
    public List<ModuloResponse> list(@PathVariable long empresaId) {
        existe(empresaId);
        Set<Modulo> activos = modulos.activos(empresaId);
        return Arrays.stream(Modulo.values())
                .map(m -> new ModuloResponse(m.name(), m.nombre(), m.descripcion(), activos.contains(m)))
                .toList();
    }

    /** Un nombre de módulo que no existe responde 400. Apagar no borra los datos del módulo. */
    @PatchMapping("/{modulo}")
    public List<ModuloResponse> set(@PathVariable long empresaId, @PathVariable Modulo modulo,
                                    @Valid @RequestBody ActivoRequest r) {
        existe(empresaId);
        modulos.set(empresaId, modulo, r.activo());
        return list(empresaId);
    }

    private void existe(long empresaId) {
        if (empresas.findById(empresaId).isEmpty()) throw new ResourceNotFoundException("La empresa no existe");
    }
}
