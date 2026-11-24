package com.jbkloh.ficha_buffet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.ParcelaRequestDTO;
import com.jbkloh.ficha_buffet.service.ParcelaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Parcelas do contrato de um evento (criadas junto com o contrato). */
@RestController
@RequestMapping("/parcelas")
@RequiredArgsConstructor
public class ParcelaController {

    private final ParcelaService parcelaService;

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<Void> atualizarParcela(@PathVariable Long id, @RequestBody @Valid ParcelaRequestDTO req) {
        parcelaService.atualizarParcela(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarParcela(@PathVariable Long id) {
        parcelaService.apagarParcela(id);
        return ResponseEntity.noContent().build();
    }
}
