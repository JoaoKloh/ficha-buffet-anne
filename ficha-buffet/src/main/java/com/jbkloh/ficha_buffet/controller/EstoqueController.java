package com.jbkloh.ficha_buffet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.MarcarPedidoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueComprasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueConferenciaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueLoucasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueOpcoesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoquePerdasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueSaldoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueValidadesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemEstoqueDetalheDTO;
import com.jbkloh.ficha_buffet.service.EstoqueService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Telas de estoque calculadas a partir dos movimentos, das fichas e dos eventos. */
@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    /** Opções de um formulário do estoque: `para` = movimento, nf ou lote. */
    @GetMapping("/opcoes")
    public ResponseEntity<EstoqueOpcoesResponseDTO> opcoes(@RequestParam String para) {
        return ResponseEntity.ok(estoqueService.opcoes(para));
    }

    @GetMapping("/saldo")
    public ResponseEntity<EstoqueSaldoResponseDTO> saldo() {
        return ResponseEntity.ok(estoqueService.saldo());
    }

    @GetMapping("/item/{id}")
    public ResponseEntity<ItemEstoqueDetalheDTO> detalharItem(@PathVariable Long id) {
        return ResponseEntity.ok(estoqueService.detalharItem(id));
    }

    /** Disponibilidade de louças em `data` (AAAA-MM-DD); sem ela, na data do próximo evento que usa louças. */
    @GetMapping("/loucas")
    public ResponseEntity<EstoqueLoucasResponseDTO> loucas(@RequestParam(required = false) String data) {
        return ResponseEntity.ok(estoqueService.loucas(data));
    }

    /** Sugestão de compras para os eventos dos próximos `horizonte` dias (7, 15, 30 ou 60) e os mínimos. */
    @GetMapping("/compras")
    public ResponseEntity<EstoqueComprasResponseDTO> compras(@RequestParam(defaultValue = "15") int horizonte) {
        return ResponseEntity.ok(estoqueService.compras(horizonte));
    }

    /** Marca ou desmarca o "pedido feito" de um item da sugestão de compras. */
    @PutMapping("/compras/marcarPedido")
    public ResponseEntity<Void> marcarPedido(@RequestBody @Valid MarcarPedidoRequestDTO req) {
        estoqueService.marcarPedido(req);
        return ResponseEntity.noContent().build();
    }

    /** Insumos (pela validade da entrada) e pré-preparos vencidos ou vencendo em 7 dias. */
    @GetMapping("/validades")
    public ResponseEntity<EstoqueValidadesResponseDTO> validades() {
        return ResponseEntity.ok(estoqueService.validades());
    }

    /** Perdas, avarias e descartes de lote do mês `mes` (AAAA-MM). */
    @GetMapping("/perdas")
    public ResponseEntity<EstoquePerdasResponseDTO> perdas(@RequestParam(required = false) String mes) {
        return ResponseEntity.ok(estoqueService.perdas(mes));
    }

    /** Integridade do estoque: movimentos por origem e pontos para conferir. */
    @GetMapping("/conferencia")
    public ResponseEntity<EstoqueConferenciaResponseDTO> conferencia() {
        return ResponseEntity.ok(estoqueService.conferencia());
    }
}
