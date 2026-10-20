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

import com.jbkloh.ficha_buffet.dtos.req.AssociationPratoProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreatePratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PratoDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdatePratoRequest;
import com.jbkloh.ficha_buffet.dtos.resp.PratoDetalhadoResponseDTO;
import com.jbkloh.ficha_buffet.model.PratoEntity;
import com.jbkloh.ficha_buffet.service.PratoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/prato")
@RequiredArgsConstructor 
public class PratoController {

    private final PratoService pratoService;

    @PostMapping("/criar-lote")
    public ResponseEntity<List<PratoEntity>> criarEmLote(@RequestBody List<PratoDTO> pratosDTO) {
        List<PratoEntity> pratosSalvos = pratoService.salvarPratosComIngredientes(pratosDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(pratosSalvos);
    }

    @PostMapping ("/criar")
    public ResponseEntity<Void>criarPrato(@RequestBody @Valid CreatePratoRequestDTO req){
        pratoService.criarPrato(req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping ("/atualizar")
    public ResponseEntity<Void>atualizarPrato(@RequestBody @Valid UpdatePratoRequest req){
        pratoService.atualizarPrato(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping ("/retornarTodos")
    public ResponseEntity<List<PratoDetalhadoResponseDTO>>retornarTodos(){
        return ResponseEntity.status(HttpStatus.CREATED).body(pratoService.listarPratos());
    }

    @PostMapping ("/associarEventos")
    public ResponseEntity<Void>associarEventos(@RequestBody @Valid AssociationPratoProducaoRequestDTO req){
        pratoService.associarEventos(req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarPrato(@PathVariable Long id) {
        pratoService.apagarPrato(id);
        return ResponseEntity.noContent().build();
    }
}
