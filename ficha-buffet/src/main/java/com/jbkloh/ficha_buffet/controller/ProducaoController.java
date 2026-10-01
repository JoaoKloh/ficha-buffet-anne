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

import com.jbkloh.ficha_buffet.dtos.req.CreateProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdateProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ProducaoResponseDTO;
import com.jbkloh.ficha_buffet.service.ProducaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/producao")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor 
public class ProducaoController {
    private final ProducaoService producaoService;

    @PostMapping ("/criar")
    public ResponseEntity<Void>criarEvento(@RequestBody @Valid CreateProducaoRequestDTO req){
        producaoService.criarEvento(req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping ("/atualizar")
    public ResponseEntity<Void>atualizarEvento(@RequestBody @Valid UpdateProducaoRequestDTO req){
        producaoService.atualizarEvento(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping ("/retornarTodos")
    public ResponseEntity<List<ProducaoResponseDTO>>retornarTodos(){
        return ResponseEntity.status(HttpStatus.CREATED).body(producaoService.listarEventos());
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarProducao(@PathVariable Long id) {
        producaoService.apagarProducao(id);
        return ResponseEntity.noContent().build();
    }
}
