package com.jbkloh.ficha_buffet.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.AdicionarItensPlanoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AjustarItemPlanoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CalcularCartaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarPlanoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoProducaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanosProducaoResponseDTO;
import com.jbkloh.ficha_buffet.service.PlanoProducaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Planos de produção do restaurante e da chocolates (tela Produção). k = chave do item no plano. */
@RestController
@RequestMapping("/planosProducao")
@RequiredArgsConstructor
public class PlanoProducaoController {

    private final PlanoProducaoService planoService;

    /** Planos da unidade (restaurante ou chocolates) com os KPIs da tela. */
    @GetMapping("/retornarTodos")
    public ResponseEntity<PlanosProducaoResponseDTO> retornarTodos(@RequestParam(defaultValue = "restaurante") String unidade) {
        return ResponseEntity.ok(planoService.listar(unidade));
    }

    /** Plano com a lista de produção, os insumos por item e a lista de compras. */
    @GetMapping("/detalhar/{id}")
    public ResponseEntity<PlanoDetalheResponseDTO> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(planoService.detalhar(id));
    }

    @PostMapping("/criar")
    public ResponseEntity<PlanoProducaoResponseDTO> criar(@RequestBody @Valid SalvarPlanoRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planoService.criar(req));
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<PlanoProducaoResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid SalvarPlanoRequestDTO req) {
        return ResponseEntity.ok(planoService.atualizar(id, req));
    }

    /** Exclui o plano; os lotes já registrados continuam em Pré-preparos. */
    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        planoService.apagar(id);
        return ResponseEntity.noContent().build();
    }

    /* ------------------------------------- Itens ------------------------------------- */

    /** Adiciona fichas ao plano, com a quantidade na unidade da ficha. */
    @PostMapping("/itens/adicionar/{id}")
    public ResponseEntity<PlanoProducaoResponseDTO> adicionarItens(@PathVariable Long id, @RequestBody @Valid AdicionarItensPlanoRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planoService.adicionarItens(id, req));
    }

    /** Ajusta a quantidade de um item ainda não concluído. */
    @PatchMapping("/itens/ajustar/{id}/{k}")
    public ResponseEntity<PlanoProducaoResponseDTO> ajustarItem(@PathVariable Long id, @PathVariable String k, @RequestBody @Valid AjustarItemPlanoRequestDTO req) {
        return ResponseEntity.ok(planoService.ajustarItem(id, k, req.qtd()));
    }

    /** Tira do plano um item ainda não concluído. */
    @DeleteMapping("/itens/apagar/{id}/{k}")
    public ResponseEntity<Void> removerItem(@PathVariable Long id, @PathVariable String k) {
        planoService.removerItem(id, k);
        return ResponseEntity.noContent().build();
    }

    /** Conclui o item: lote em Pré-preparos com a validade da ficha e baixa dos insumos. Responde { loteId, codigo }. */
    @PostMapping("/itens/concluir/{id}/{k}")
    public ResponseEntity<Map<String, Object>> concluirItem(@PathVariable Long id, @PathVariable String k) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planoService.concluirItem(id, k));
    }

    /** Volta o item para a produção e apaga o lote (422 se o lote já teve uso ou descarte). */
    @PostMapping("/itens/desfazer/{id}/{k}")
    public ResponseEntity<Void> desfazerItem(@PathVariable Long id, @PathVariable String k) {
        planoService.desfazerItem(id, k);
        return ResponseEntity.noContent().build();
    }

    /** Pratos da carta previstos no período viram as fichas a produzir. Responde { fichas }. */
    @PostMapping("/calcularCarta/{id}")
    public ResponseEntity<Map<String, Integer>> calcularCarta(@PathVariable Long id, @RequestBody @Valid CalcularCartaRequestDTO req) {
        return ResponseEntity.ok(planoService.calcularCarta(id, req));
    }
}
