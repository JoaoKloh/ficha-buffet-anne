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
import com.jbkloh.ficha_buffet.dtos.req.UpdatePratoRequest;
import com.jbkloh.ficha_buffet.dtos.resp.PratoDetalhadoResponseDTO;
import com.jbkloh.ficha_buffet.service.PratoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.jbkloh.ficha_buffet.dtos.req.CalcularCustoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PreparacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CustoPreparacaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FichasOpcoesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.service.FichaService;
import org.springframework.web.bind.annotation.RequestParam;

@RestController 
@RequestMapping ("/prato")
@RequiredArgsConstructor 
public class PratoController {

    private final PratoService pratoService;
    private final FichaService fichaService;

    @PostMapping ("/criar")
    public ResponseEntity<Void>criarPrato(@RequestBody @Valid CreatePratoRequestDTO req){
        pratoService.criarPrato(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping ("/atualizar")
    public ResponseEntity<Void>atualizarPrato(@RequestBody @Valid UpdatePratoRequest req){
        pratoService.atualizarPrato(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping ("/retornarTodos")
    public ResponseEntity<List<PratoDetalhadoResponseDTO>>retornarTodos(){
        return ResponseEntity.ok(pratoService.listarPratos());
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

    /* ---------- Anne Gestão: fichas técnicas (aba Preparações da tela Fichas) ---------- */

    /** Fichas em ordem alfabética com custo e situação calculados; `busca` filtra pelo nome. */
    @GetMapping("/fichas/retornarTodos")
    public ResponseEntity<List<PreparacaoListaItemDTO>> retornarFichas(@RequestParam(defaultValue = "") String busca) {
        return ResponseEntity.ok(fichaService.listarFichas(busca));
    }

    /** Opções dos formulários de ficha e de prato de venda. */
    @GetMapping("/fichas/opcoes")
    public ResponseEntity<FichasOpcoesResponseDTO> opcoesFichas() {
        return ResponseEntity.ok(fichaService.opcoes());
    }

    /** Custo do rascunho da ficha, para o resumo ao vivo do formulário. */
    @PostMapping("/fichas/calcularCusto")
    public ResponseEntity<CustoPreparacaoDTO> calcularCusto(@RequestBody @Valid CalcularCustoRequestDTO req) {
        return ResponseEntity.ok(fichaService.calcularCusto(req));
    }

    @PostMapping("/fichas/criar")
    public ResponseEntity<PreparacaoResponseDTO> criarFicha(@RequestBody @Valid PreparacaoRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fichaService.criarFicha(req));
    }

    @PutMapping("/fichas/atualizar/{id}")
    public ResponseEntity<PreparacaoResponseDTO> atualizarFicha(@PathVariable Long id, @RequestBody @Valid PreparacaoRequestDTO req) {
        return ResponseEntity.ok(fichaService.atualizarFicha(id, req));
    }

    @DeleteMapping("/fichas/apagar/{id}")
    public ResponseEntity<Void> apagarFicha(@PathVariable Long id) {
        fichaService.apagarFicha(id);
        return ResponseEntity.noContent().build();
    }
}
