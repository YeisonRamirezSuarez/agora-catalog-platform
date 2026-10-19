package com.agora.catalog.empresa.service;

import com.agora.catalog.empresa.dto.response.EmpresaResponse;
import com.agora.catalog.empresa.model.Empresa;
import com.agora.catalog.empresa.repository.EmpresaRepository;
import com.agora.catalog.usuario.model.Usuario;
import com.agora.catalog.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ListEmpresasService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    public ListEmpresasService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // shortcut: lista completa sin paginar; pasar a PageResponse cuando haya cientos de empresas.
    public List<EmpresaResponse> list() {
        List<Empresa> empresas = empresaRepository.findAll();
        Map<Long, List<Usuario>> admins = usuarioRepository.findByEmpresaIds(empresas.stream().map(Empresa::id).toList())
                .stream().collect(Collectors.groupingBy(Usuario::empresaId));
        return empresas.stream()
                .map(e -> EmpresaResponse.of(e, admins.getOrDefault(e.id(), List.of())))
                .toList();
    }

    public EmpresaResponse get(long id) {
        Empresa empresa = SaveEmpresaService.require(empresaRepository, id);
        return EmpresaResponse.of(empresa, usuarioRepository.findByEmpresaIds(List.of(id)));
    }
}
