package com.jbkloh.ficha_buffet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.CalcularCmvRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PratoVendaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CmvDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratosVendaResponseDTO;
import com.jbkloh.ficha_buffet.service.PratoVendaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Pratos da carta do restaurante. */
@RestController
@RequestMapping("/pratosvenda")
@RequiredArgsConstructor
public class PratoVendaController {

    private final PratoVendaService pratoVendaService;

    /** KPIs de CMV e pratos por seção; `busca` filtra pelo nome. */
    @GetMapping("/retornarTodos")
    public ResponseEntity<PratosVendaResponseDTO> retornarTodos(@RequestParam(defaultValue = "") String busca) {
        return ResponseEntity.ok(pratoVendaService.listarPratos(busca));
    }

    /** Custo e CMV do rascunho do prato, para o resumo ao vivo do formulário. */
    @PostMapping("/calcularCusto")
    public ResponseEntity<CmvDTO> calcularCusto(@RequestBody @Valid CalcularCmvRequestDTO req) {
        return ResponseEntity.ok(pratoVendaService.calcularCmv(req));
    }

    @PostMapping("/criar")
    public ResponseEntity<PratoVendaResponseDTO> criarPrato(@RequestBody @Valid PratoVendaRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pratoVendaService.criarPrato(req));
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<PratoVendaResponseDTO> atualizarPrato(@PathVariable Long id, @RequestBody @Valid PratoVendaRequestDTO req) {
        return ResponseEntity.ok(pratoVendaService.atualizarPrato(id, req));
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarPrato(@PathVariable Long id) {
        pratoVendaService.apagarPrato(id);
        return ResponseEntity.noContent().build();
    }
}
