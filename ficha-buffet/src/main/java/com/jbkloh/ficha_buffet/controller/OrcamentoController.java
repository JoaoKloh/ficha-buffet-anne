package com.jbkloh.ficha_buffet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.CreateOrcamentoRequest;
import com.jbkloh.ficha_buffet.dtos.resp.OrcamentoDetalhadoDTO;
import com.jbkloh.ficha_buffet.service.OrcamentoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/orcamento")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor 
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    // 201 com o orçamento criado: o frontend precisa do id gerado para listar/excluir depois.
    @PostMapping ("/criar")
    public ResponseEntity<OrcamentoDetalhadoDTO>criarOrcamento(@RequestBody @Valid CreateOrcamentoRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(orcamentoService.criarOrcamento(req));
    }

    @GetMapping ("/retornarTodos")
    public ResponseEntity<List<OrcamentoDetalhadoDTO>>retornarTodos(){
        return ResponseEntity.ok(orcamentoService.listarOrcamentos());
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarOrcamento(@PathVariable Long id) {
        orcamentoService.apagarOrcamento(id);
        return ResponseEntity.noContent().build();
    }
}
