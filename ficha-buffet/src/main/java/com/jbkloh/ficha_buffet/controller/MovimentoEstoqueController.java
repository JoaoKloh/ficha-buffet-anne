package com.jbkloh.ficha_buffet.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.InventarioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentoEstoqueRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentosResponseDTO;
import com.jbkloh.ficha_buffet.service.MovimentoEstoqueService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/movimentos")
@RequiredArgsConstructor
public class MovimentoEstoqueController {

    private final MovimentoEstoqueService movimentoEstoqueService;

    /** Os 300 movimentos mais recentes do filtro (todos, entradas, saidas ou contagens) e o total. */
    @GetMapping("/retornarTodos")
    public ResponseEntity<MovimentosResponseDTO> retornarTodos(@RequestParam(defaultValue = "todos") String filtro) {
        return ResponseEntity.ok(movimentoEstoqueService.listarMovimentos(filtro));
    }

    @PostMapping("/criar")
    public ResponseEntity<MovimentoResponseDTO> criarMovimento(@RequestBody @Valid MovimentoEstoqueRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimentoEstoqueService.criarMovimento(req));
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarMovimento(@PathVariable Long id) {
        movimentoEstoqueService.apagarMovimento(id);
        return ResponseEntity.noContent().build();
    }

    /** Lança a contagem de inventário: um movimento por item com diferença. Responde { lancados }. */
    @PostMapping("/inventario")
    public ResponseEntity<Map<String, Integer>> lancarInventario(@RequestBody @Valid InventarioRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("lancados", movimentoEstoqueService.lancarInventario(req)));
    }
}
