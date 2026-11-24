package com.jbkloh.ficha_buffet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.AjustarEquipeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EquipeResponseDTO;
import com.jbkloh.ficha_buffet.service.EquipeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Cadastro da equipe do buffet. A escala de cada evento fica em ProducaoController (/producao/eventos/equipe/*). */
@RestController
@RequestMapping("/equipe")
@RequiredArgsConstructor
public class EquipeController {

    private final EquipeService equipeService;

    /** Todas as pessoas do cadastro da equipe, em ordem alfabética (checklist da escala). */
    @GetMapping("/retornarTodos")
    public ResponseEntity<List<EquipeResponseDTO>> retornarTodos() {
        return ResponseEntity.ok(equipeService.listar());
    }

    /** Edita o cadastro da pessoa: só os campos informados mudam (função e diária padrão valem para as próximas escalas). */
    @PatchMapping("/ajustar/{id}")
    public ResponseEntity<EquipeResponseDTO> ajustar(@PathVariable Long id, @RequestBody @Valid AjustarEquipeRequestDTO req) {
        return ResponseEntity.ok(equipeService.ajustar(id, req));
    }
}
