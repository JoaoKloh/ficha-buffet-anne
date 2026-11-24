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

import com.jbkloh.ficha_buffet.dtos.req.ExtraEventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExtraResponseDTO;
import com.jbkloh.ficha_buffet.service.ExtraEventoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Extras cobrados no evento, além do contrato. */
@RestController
@RequestMapping("/extras")
@RequiredArgsConstructor
public class ExtraEventoController {

    private final ExtraEventoService extraEventoService;

    @PostMapping("/criar")
    public ResponseEntity<ExtraResponseDTO> criarExtra(@RequestBody @Valid ExtraEventoRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(extraEventoService.criarExtra(req));
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<ExtraResponseDTO> atualizarExtra(@PathVariable Long id, @RequestBody @Valid ExtraEventoRequestDTO req) {
        return ResponseEntity.ok(extraEventoService.atualizarExtra(id, req));
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarExtra(@PathVariable Long id) {
        extraEventoService.apagarExtra(id);
        return ResponseEntity.noContent().build();
    }
}
