package com.jbkloh.ficha_buffet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.CategoriaFinanceiraRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.service.CategoriaFinanceiraService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Categorias da DRE (Caixa › Categorias da DRE). */
@RestController
@RequestMapping("/api/categoriafinanceira")
@RequiredArgsConstructor
public class CategoriaFinanceiraController {

    private final CategoriaFinanceiraService categoriaService;

    @PostMapping("/criar")
    public ResponseEntity<CategoriaResponseDTO> criar(@RequestBody @Valid CategoriaFinanceiraRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.criarCategoria(req));
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<CategoriaResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid CategoriaFinanceiraRequestDTO req) {
        return ResponseEntity.ok(categoriaService.atualizarCategoria(id, req));
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        categoriaService.apagarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
