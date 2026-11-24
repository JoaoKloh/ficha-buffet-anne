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

import com.jbkloh.ficha_buffet.dtos.req.ImportarXmlRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.NfDeParaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.NotaFiscalRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.DeParaSugestaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalImportadaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.service.NotaFiscalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Notas fiscais de compra. */
@RestController
@RequestMapping("/notasfiscais")
@RequiredArgsConstructor
public class NotaFiscalController {

    private final NotaFiscalService notaFiscalService;

    @GetMapping("/retornarTodos")
    public ResponseEntity<List<NotaFiscalListaItemDTO>> retornarTodos() {
        return ResponseEntity.ok(notaFiscalService.listarNotas());
    }

    @GetMapping("/detalhar/{id}")
    public ResponseEntity<NotaFiscalListaItemDTO> detalharNota(@PathVariable Long id) {
        return ResponseEntity.ok(notaFiscalService.detalharNota(id));
    }

    /** Salva um rascunho novo. */
    @PostMapping("/salvarRascunho")
    public ResponseEntity<NotaFiscalResponseDTO> criarRascunho(@RequestBody @Valid NotaFiscalRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notaFiscalService.salvarRascunho(null, req));
    }

    @PutMapping("/salvarRascunho/{id}")
    public ResponseEntity<NotaFiscalResponseDTO> atualizarRascunho(@PathVariable Long id, @RequestBody @Valid NotaFiscalRequestDTO req) {
        return ResponseEntity.ok(notaFiscalService.salvarRascunho(id, req));
    }

    /** Dá entrada de uma nota nova: estoque, preço dos itens e contas a pagar. */
    @PostMapping("/lancar")
    public ResponseEntity<NotaFiscalResponseDTO> lancarNota(@RequestBody @Valid NotaFiscalRequestDTO req) {
        return ResponseEntity.ok(notaFiscalService.lancarNota(null, req));
    }

    /** Dá entrada de um rascunho existente, já com os dados do formulário. */
    @PostMapping("/lancar/{id}")
    public ResponseEntity<NotaFiscalResponseDTO> lancarRascunho(@PathVariable Long id, @RequestBody @Valid NotaFiscalRequestDTO req) {
        return ResponseEntity.ok(notaFiscalService.lancarNota(id, req));
    }

    /** Desfaz a entrada: a nota volta a rascunho. */
    @PostMapping("/estornar/{id}")
    public ResponseEntity<Void> estornarNota(@PathVariable Long id) {
        notaFiscalService.estornarNota(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarNota(@PathVariable Long id) {
        notaFiscalService.apagarNota(id);
        return ResponseEntity.noContent().build();
    }

    /** Lê o XML de uma NF-e e devolve a nota pré-preenchida, sem gravar. */
    @PostMapping("/importarXml")
    public ResponseEntity<NotaFiscalImportadaDTO> importarXml(@RequestBody @Valid ImportarXmlRequestDTO req) {
        return ResponseEntity.ok(notaFiscalService.importarXml(req.xml()));
    }

    /** Para cada linha enviada, o item do cadastro sugerido pelo histórico do fornecedor (ou null), na mesma ordem. */
    @PostMapping("/sugerirItens")
    public ResponseEntity<List<DeParaSugestaoDTO>> sugerirItens(@RequestBody @Valid NfDeParaRequestDTO req) {
        return ResponseEntity.ok(notaFiscalService.sugerirItens(req));
    }
}
