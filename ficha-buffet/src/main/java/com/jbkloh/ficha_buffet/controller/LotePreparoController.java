package com.jbkloh.ficha_buffet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.LotePreparoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentarLoteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoquePreparosResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.service.LotePreparoService;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;

/** Lotes de pré-preparo. */
@RestController
@RequestMapping("/lotes")
@RequiredArgsConstructor
public class LotePreparoController {

    private final LotePreparoService lotePreparoService;

    /** KPIs, pronto agora e até 300 lotes do filtro (ativos, atencao ou todos). */
    @GetMapping("/retornarTodos")
    public ResponseEntity<EstoquePreparosResponseDTO> retornarTodos(@RequestParam(defaultValue = "ativos") String filtro) {
        return ResponseEntity.ok(lotePreparoService.listarLotes(filtro));
    }

    @GetMapping("/detalhar/{id}")
    public ResponseEntity<LoteListaItemDTO> detalharLote(@PathVariable Long id) {
        return ResponseEntity.ok(lotePreparoService.detalharLote(id));
    }

    @PostMapping("/criar")
    public ResponseEntity<LoteResponseDTO> criarLote(@RequestBody @Validated({ Default.class, LotePreparoRequestDTO.AoCriar.class }) LotePreparoRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lotePreparoService.criarLote(req));
    }

    /** Lote sem uso: corpo completo. Com uso: só validade, responsável e observações (sem preparacaoId). */
    @PutMapping("/atualizar/{id}")
    public ResponseEntity<LoteResponseDTO> atualizarLote(@PathVariable Long id, @RequestBody @Valid LotePreparoRequestDTO req) {
        return ResponseEntity.ok(lotePreparoService.atualizarLote(id, req));
    }

    /** Uso ou descarte de parte do lote. */
    @PostMapping("/movimentar/{id}")
    public ResponseEntity<LoteResponseDTO> movimentarLote(@PathVariable Long id, @RequestBody @Valid MovimentarLoteRequestDTO req) {
        return ResponseEntity.ok(lotePreparoService.movimentarLote(id, req));
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarLote(@PathVariable Long id) {
        lotePreparoService.apagarLote(id);
        return ResponseEntity.noContent().build();
    }
}
