package com.jbkloh.ficha_buffet.controller;

import java.util.Map;

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

import com.jbkloh.ficha_buffet.dtos.req.ClassificarUnidadeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LancamentoCaixaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PagamentoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.RateioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SaldosContasRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.TransferenciaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CaixaMesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ContasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.DreResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FluxoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SemUnidadeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.TransferenciasResponseDTO;
import com.jbkloh.ficha_buffet.service.CaixaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/lancamentos")
@RequiredArgsConstructor
public class LancamentoController {

    private final CaixaService caixaService;

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarLancamento(@PathVariable Long id) {
        caixaService.apagarLancamento(id);
        return ResponseEntity.noContent().build();
    }

    /** Aba Lançamentos do mês (AAAA-MM) com o filtro todos, receita, despesa, previsto ou realizado, na unidade pedida ("" todas, "_sem" sem unidade). */
    @GetMapping("/caixa/retornarMes")
    public ResponseEntity<CaixaMesResponseDTO> retornarMes(@RequestParam(required = false) String mes, @RequestParam(defaultValue = "todos") String filtro,
            @RequestParam(required = false) String unidade) {
        return ResponseEntity.ok(caixaService.retornarMes(mes, filtro, unidade));
    }

    /** Contas a pagar ou a receber (tipo), com o filtro abertas, vencidas, semana, parciais ou quitadas. */
    @GetMapping("/caixa/contas")
    public ResponseEntity<ContasResponseDTO> contas(@RequestParam(defaultValue = "pagar") String tipo, @RequestParam(defaultValue = "abertas") String filtro,
            @RequestParam(required = false) String unidade) {
        return ResponseEntity.ok(caixaService.contas(tipo, filtro, unidade));
    }

    @GetMapping("/caixa/fluxo")
    public ResponseEntity<FluxoCaixaResponseDTO> fluxo(@RequestParam(required = false) String unidade) {
        return ResponseEntity.ok(caixaService.fluxo(unidade));
    }

    @GetMapping("/caixa/transferencias")
    public ResponseEntity<TransferenciasResponseDTO> transferencias(@RequestParam(required = false) String unidade) {
        return ResponseEntity.ok(caixaService.transferencias(unidade));
    }

    @GetMapping("/caixa/semUnidade")
    public ResponseEntity<SemUnidadeResponseDTO> semUnidade() {
        return ResponseEntity.ok(caixaService.semUnidade());
    }

    @GetMapping("/caixa/detalhar/{id}")
    public ResponseEntity<LancamentoListaItemDTO> detalharLancamento(@PathVariable Long id) {
        return ResponseEntity.ok(caixaService.detalharLancamento(id));
    }

    @PostMapping("/caixa/criar")
    public ResponseEntity<LancamentoCaixaResponseDTO> criarNoCaixa(@RequestBody @Valid LancamentoCaixaRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caixaService.criarLancamento(req));
    }

    @PutMapping("/caixa/atualizar/{id}")
    public ResponseEntity<LancamentoCaixaResponseDTO> atualizarNoCaixa(@PathVariable Long id, @RequestBody @Valid LancamentoCaixaRequestDTO req) {
        return ResponseEntity.ok(caixaService.atualizarLancamento(id, req));
    }

    /** Paga ou recebe hoje o saldo inteiro. */
    @PostMapping("/caixa/pagar/{id}")
    public ResponseEntity<Void> pagarLancamento(@PathVariable Long id) {
        caixaService.pagarLancamento(id);
        return ResponseEntity.noContent().build();
    }

    /** Registra um pagamento ou recebimento, inteiro ou de uma parte. */
    @PostMapping("/caixa/pagamentos/registrar/{id}")
    public ResponseEntity<LancamentoCaixaResponseDTO> registrarPagamento(@PathVariable Long id, @RequestBody @Valid PagamentoRequestDTO req) {
        return ResponseEntity.ok(caixaService.registrarPagamento(id, req));
    }

    /** Desfaz o último pagamento ou recebimento. */
    @PostMapping("/caixa/pagamentos/desfazer/{id}")
    public ResponseEntity<LancamentoCaixaResponseDTO> desfazerPagamento(@PathVariable Long id) {
        return ResponseEntity.ok(caixaService.desfazerPagamento(id));
    }

    @PostMapping("/caixa/transferencias/criar")
    public ResponseEntity<LancamentoCaixaResponseDTO> criarTransferencia(@RequestBody @Valid TransferenciaRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caixaService.criarTransferencia(req));
    }

    @PutMapping("/caixa/transferencias/atualizar/{id}")
    public ResponseEntity<LancamentoCaixaResponseDTO> atualizarTransferencia(@PathVariable Long id, @RequestBody @Valid TransferenciaRequestDTO req) {
        return ResponseEntity.ok(caixaService.atualizarTransferencia(id, req));
    }

    /** Põe lançamentos numa unidade de negócio. */
    @PutMapping("/caixa/classificar")
    public ResponseEntity<Map<String, Integer>> classificar(@RequestBody @Valid ClassificarUnidadeRequestDTO req) {
        return ResponseEntity.ok(Map.of("classificados", caixaService.classificar(req)));
    }

    @PutMapping("/caixa/saldos")
    public ResponseEntity<Void> salvarSaldos(@RequestBody @Valid SaldosContasRequestDTO req) {
        caixaService.salvarSaldos(req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/caixa/rateio")
    public ResponseEntity<Void> salvarRateio(@RequestBody @Valid RateioRequestDTO req) {
        caixaService.salvarRateio(req);
        return ResponseEntity.noContent().build();
    }

    /**
     * DRE do ano (AAAA) e período ("ano" ou "01".."12"). unidade: "" consolidado, uma unidade, "_sem" ou "_cmp" (comparar).
     * regime: competencia ou caixa. previstos=false considera só o que já foi pago. ratear distribui a estrutura compartilhada.
     */
    @GetMapping("/dre")
    public ResponseEntity<DreResponseDTO> dre(@RequestParam(required = false) String ano, @RequestParam(required = false) String periodo,
            @RequestParam(defaultValue = "true") boolean previstos, @RequestParam(required = false) String unidade,
            @RequestParam(defaultValue = "competencia") String regime, @RequestParam(defaultValue = "true") boolean ratear) {
        return ResponseEntity.ok(caixaService.dre(ano, periodo, previstos, unidade, regime, ratear));
    }
}
