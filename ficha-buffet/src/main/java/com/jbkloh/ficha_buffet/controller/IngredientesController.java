package com.jbkloh.ficha_buffet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.AssociationIngredientePratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreateIngredienteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdateIngredienteRequestDto;
import com.jbkloh.ficha_buffet.dtos.resp.IngredienteResponseDTO;
import com.jbkloh.ficha_buffet.service.IngredientesService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/ingredientes")
@RequiredArgsConstructor 
public class IngredientesController {

    private final IngredientesService ingredientesService;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(ingredientesService.listarIngredientes());
    }

    @PostMapping ("/associarPratos")
    public ResponseEntity<Void>associarEventos(@RequestBody @Valid AssociationIngredientePratoRequestDTO req){
        ingredientesService.associarPratos(req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarIngrediente(@PathVariable Long id) {
        ingredientesService.apagarIngrediente(id);
        return ResponseEntity.noContent().build();
    }
}
