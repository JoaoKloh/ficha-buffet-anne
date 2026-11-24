package com.jbkloh.ficha_buffet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.CreateIngredienteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdateIngredienteRequestDto;
import com.jbkloh.ficha_buffet.dtos.resp.IngredienteResponseDTO;
import com.jbkloh.ficha_buffet.service.IngredientesService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.jbkloh.ficha_buffet.dtos.req.InsumoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SituacaoInsumoDTO;
import com.jbkloh.ficha_buffet.service.InsumoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;

@RestController 
@RequestMapping ("/ingredientes")
@RequiredArgsConstructor 
public class IngredientesController {

    private final IngredientesService ingredientesService;
    private final InsumoService insumoService;

    @PostMapping ("/criar")
    public ResponseEntity<Void>criarIngrediente(@RequestBody @Valid CreateIngredienteRequestDTO req){
        ingredientesService.criarIngrediente(req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping ("/atualizar")
    public ResponseEntity<Void>atualizarIngrediente(@RequestBody @Valid UpdateIngredienteRequestDto req){
        ingredientesService.atualizarIngrediente(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping ("/retornarTodos")
    public ResponseEntity<List<IngredienteResponseDTO>>retornarTodos(){
        return ResponseEntity.ok(ingredientesService.listarIngredientes());
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarIngrediente(@PathVariable Long id) {
        ingredientesService.apagarIngrediente(id);
        return ResponseEntity.noContent().build();
    }

    /* ---------- Anne Gestão: cadastro de itens de estoque (aba Itens da tela Fichas) ---------- */

    /** Itens em ordem alfabética, com em quantas fichas cada um aparece; `busca` filtra pelo nome. */
    @GetMapping("/itens/retornarTodos")
    public ResponseEntity<List<InsumoListaItemDTO>> retornarItens(@RequestParam(defaultValue = "") String busca) {
        return ResponseEntity.ok(insumoService.listarItens(busca));
    }

    /** Diz se o item pode ser excluído: uso em fichas e movimentos de estoque. */
    @GetMapping("/itens/situacao/{id}")
    public ResponseEntity<SituacaoInsumoDTO> situacaoItem(@PathVariable Long id) {
        return ResponseEntity.ok(insumoService.situacaoItem(id));
    }

    @PostMapping("/itens/criar")
    public ResponseEntity<InsumoResponseDTO> criarItem(@RequestBody @Valid InsumoRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insumoService.criarItem(req));
    }

    @PutMapping("/itens/atualizar/{id}")
    public ResponseEntity<InsumoResponseDTO> atualizarItem(@PathVariable Long id, @RequestBody @Valid InsumoRequestDTO req) {
        return ResponseEntity.ok(insumoService.atualizarItem(id, req));
    }

    @DeleteMapping("/itens/apagar/{id}")
    public ResponseEntity<Void> apagarItem(@PathVariable Long id) {
        insumoService.apagarItem(id);
        return ResponseEntity.noContent().build();
    }
}
