package com.jbkloh.ficha_buffet.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.AdicionarPratosCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AnalisarCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CopiarCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ImportarCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ItemCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PatchItemCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ImportarCardapioAnaliseResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.service.ItemCardapioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Cardápio do evento. A lista vem na ficha do evento (GET /producao/eventos/detalhar/{id}). */
@RestController
@RequestMapping("/cardapio")
@RequiredArgsConstructor
public class ItemCardapioController {

    private final ItemCardapioService itemCardapioService;

    @PostMapping("/criar")
    public ResponseEntity<ItemCardapioResponseDTO> criarItem(@RequestBody @Valid ItemCardapioRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemCardapioService.criarItem(req));
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<ItemCardapioResponseDTO> atualizarItem(@PathVariable Long id, @RequestBody @Valid ItemCardapioRequestDTO req) {
        return ResponseEntity.ok(itemCardapioService.atualizarItem(id, req));
    }

    /** Situação (sim, definir, nao) e fator por convidado: só o que vier no corpo muda. */
    @PatchMapping("/ajustar/{id}")
    public ResponseEntity<ItemCardapioResponseDTO> ajustarItem(@PathVariable Long id, @RequestBody @Valid PatchItemCardapioRequestDTO req) {
        return ResponseEntity.ok(itemCardapioService.ajustarItem(id, req));
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarItem(@PathVariable Long id) {
        itemCardapioService.apagarItem(id);
        return ResponseEntity.noContent().build();
    }

    /** Seleção múltipla: adiciona as fichas marcadas que o cardápio ainda não tem. Responde { adicionados }. */
    @PostMapping("/adicionarVarios/{eventoId}")
    public ResponseEntity<Map<String, Integer>> adicionarPratos(@PathVariable Long eventoId, @RequestBody @Valid AdicionarPratosCardapioRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("adicionados", itemCardapioService.adicionarPratos(eventoId, req.preparacaoIds())));
    }

    /** Copia para o evento os pratos de outro evento que ele ainda não tem. Responde { copiados }. */
    @PostMapping("/copiar/{eventoId}")
    public ResponseEntity<Map<String, Integer>> copiarCardapio(@PathVariable Long eventoId, @RequestBody @Valid CopiarCardapioRequestDTO req) {
        return ResponseEntity.ok(Map.of("copiados", itemCardapioService.copiarCardapio(eventoId, req.origemEventoId())));
    }

    /** Confere o cardápio colado como texto contra as fichas, sem gravar. */
    @PostMapping("/analisarImportacao/{eventoId}")
    public ResponseEntity<ImportarCardapioAnaliseResponseDTO> analisarImportacao(@PathVariable Long eventoId, @RequestBody @Valid AnalisarCardapioRequestDTO req) {
        return ResponseEntity.ok(itemCardapioService.analisarImportacao(eventoId, req.texto()));
    }

    /** Grava as linhas conferidas da importação. Responde { adicionados }. */
    @PostMapping("/confirmarImportacao/{eventoId}")
    public ResponseEntity<Map<String, Integer>> confirmarImportacao(@PathVariable Long eventoId, @RequestBody @Valid ImportarCardapioRequestDTO req) {
        return ResponseEntity.ok(Map.of("adicionados", itemCardapioService.confirmarImportacao(eventoId, req)));
    }
}
