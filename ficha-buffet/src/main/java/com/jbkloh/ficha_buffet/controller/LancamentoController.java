package com.jbkloh.ficha_buffet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.AtualizarLancamentoDTO;
import com.jbkloh.ficha_buffet.dtos.req.CriarLancamentoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoResponseDTO;
import com.jbkloh.ficha_buffet.service.LancamentoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/lancamentos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor 
public class LancamentoController {

    private final LancamentoService lancamentoService;

    // Diferente de /producao/criar (204): o frontend precisa do id gerado para exibir o lançamento na lista.
    @PostMapping ("/criar")
    public ResponseEntity<LancamentoResponseDTO>criarLancamento(@RequestBody @Valid CriarLancamentoDTO req){
        return ResponseEntity.status(HttpStatus.CREATED).body(lancamentoService.criarLancamento(req));
    }

    @PutMapping ("/atualizar")
    public ResponseEntity<LancamentoResponseDTO>atualizarLancamento(@RequestBody @Valid AtualizarLancamentoDTO req){
        return ResponseEntity.ok(lancamentoService.atualizarLancamento(req));
    }

    @GetMapping ("/retornarTodos")
    public ResponseEntity<List<LancamentoResponseDTO>>retornarTodos(){
        return ResponseEntity.ok(lancamentoService.listarLancamentos());
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarLancamento(@PathVariable Long id) {
        lancamentoService.apagarLancamento(id);
        return ResponseEntity.noContent().build();
    }
}
