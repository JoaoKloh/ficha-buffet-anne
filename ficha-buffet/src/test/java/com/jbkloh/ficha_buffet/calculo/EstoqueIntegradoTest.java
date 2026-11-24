package com.jbkloh.ficha_buffet.calculo;

import static com.jbkloh.ficha_buffet.calculo.Fixtures.cardapio;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.evento;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.insumo;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.item;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.prep;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.producao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.resp.EstoqueComprasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueConferenciaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoquePerdasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueValidadesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SugestaoCompraDTO;

/** Estoque integrado sobre dados em memória: compras, validades, perdas, conferência e abatimento do uso de lote. */
class EstoqueIntegradoTest {

    private static final String HOJE = "2026-11-19";

    private static MovimentoResponseDTO mov(long id, String tipo, long insumoId, double qtd, String data, Double custoUnit, String origem, String validade,
            String motivo, Long eventoId, Long nfId, String unidadeUso) {
        return new MovimentoResponseDTO(id, tipo, insumoId, qtd, Math.abs(qtd), "kg", data, motivo, eventoId, custoUnit, origem, nfId, data + "T10:00:00Z",
            validade, unidadeUso, null);
    }

    private static MovimentoResponseDTO entrada(long id, long insumoId, double qtd, String data, String validade) {
        return mov(id, "entrada", insumoId, qtd, data, null, "manual", validade, "", null, null, null);
    }

    private static InsumoResponseDTO insumoCompra(long id, String nome, double minimo, double custo, Double fator, String fornecedor) {
        return new InsumoResponseDTO(id, nome, "ingrediente", "Mercearia", "kg", "Seco", "cx", fator, minimo, custo, fornecedor, "2026-01-01");
    }

    private static LoteResponseDTO lote(long id, long preparacaoId, double qtd, double saldo, String validade, String destino, Long eventoId,
            List<LoteResponseDTO.Historico> historico) {
        return new LoteResponseDTO(id, "L" + id, preparacaoId, qtd, saldo, "un", "2026-11-01", validade, destino, eventoId, !"evento".equals(destino), "", "",
            historico, "", null);
    }

    /* -------------------------------- Compras -------------------------------- */

    @Test
    void compras_reservasDoHorizonteMaisMinimoMenosSaldoArredondadoAEmbalagem() {
        Dados d = new Dados();
        // Arroz: evento contratado no horizonte reserva 20 kg (10 convidados × 1 porção ÷ rende 1 × 2 kg); mínimo 5; saldo 3.
        d.insumos.addAll(List.of(insumoCompra(1, "Arroz", 5, 4, 5.0, "Atacado"), insumoCompra(2, "Sal", 2, 0, null, ""),
            insumo(3, "Prato", "louca", "un", "Depósito de louças", 100, 10)));
        d.preparacoes.add(prep(10, "Risoto", "principal", 1, item(1L, null, 2, 1)));
        d.eventos.add(evento(1, "contratado", "2026-11-30", 10, 0, "", null));
        d.cardapio.add(cardapio(100, 1, 10, 1, "sim"));
        d.movimentos.add(entrada(1, 1, 3, "2026-11-01", null));

        EstoqueComprasResponseDTO r = EstoqueIntegrado.compras(d, 15, Map.of(), HOJE);

        assertThat(r.horizonte()).isEqualTo(15);
        // Falta 20 + 5 − 3 = 22 kg → 5 caixas de 5 kg = 25 kg; sal sem fornecedor vai por último.
        assertThat(r.fornecedores()).extracting(EstoqueComprasResponseDTO.Fornecedor::fornecedor).containsExactly("Atacado", EstoqueIntegrado.SEM_FORNECEDOR);
        SugestaoCompraDTO arroz = r.fornecedores().get(0).itens().get(0);
        assertThat(arroz.reservado()).isEqualTo(20);
        assertThat(arroz.falta()).isEqualTo(22);
        assertThat(arroz.embalagens()).isEqualTo(5);
        assertThat(arroz.comprar()).isEqualTo(25);
        assertThat(arroz.valor()).isEqualTo(100);
        assertThat(arroz.eventos()).containsExactly("26-001");
        SugestaoCompraDTO sal = r.fornecedores().get(1).itens().get(0);
        assertThat(sal.semPreco()).isTrue();
        assertThat(sal.embalagens()).isZero();
        assertThat(sal.comprar()).isEqualTo(2);
        // Louça não entra na sugestão.
        assertThat(r.fornecedores()).flatExtracting(EstoqueComprasResponseDTO.Fornecedor::itens).extracting(SugestaoCompraDTO::insumoId).doesNotContain(3L);
        assertThat(r.kpis()).isEqualTo(new EstoqueComprasResponseDTO.Kpis(2, 2, 100, true, 0, 1));

        // Fora do horizonte de 7 dias o evento não reserva.
        assertThat(EstoqueIntegrado.compras(d, 7, Map.of(), HOJE).fornecedores().get(0).itens().get(0).reservado()).isZero();
    }

    @Test
    void compras_pedidoFeitoSaiDosTotaisAteEntrarMercadoriaDepoisDoPedido() {
        Dados d = new Dados();
        d.insumos.add(insumoCompra(1, "Arroz", 10, 4, null, "Atacado"));
        d.movimentos.add(entrada(1, 1, 2, "2026-11-10", null));
        Map<Long, EstoqueIntegrado.Pedido> pedidos = Map.of(1L, new EstoqueIntegrado.Pedido("2026-11-18", "2026-11-18T13:00:00Z"));

        EstoqueComprasResponseDTO r = EstoqueIntegrado.compras(d, 15, pedidos, HOJE);
        SugestaoCompraDTO arroz = r.fornecedores().get(0).itens().get(0);
        assertThat(arroz.pedido()).isTrue();
        assertThat(arroz.pedidoEm()).isEqualTo("2026-11-18T13:00:00Z");
        assertThat(r.kpis().itens()).isZero();
        assertThat(r.kpis().pedidos()).isEqualTo(1);
        assertThat(r.fornecedores().get(0).valor()).isZero();

        // Entrada no dia do pedido (ou depois): o pedido deixa de valer.
        d.movimentos.add(entrada(2, 1, 1, "2026-11-18", null));
        SugestaoCompraDTO depois = EstoqueIntegrado.compras(d, 15, pedidos, HOJE).fornecedores().get(0).itens().get(0);
        assertThat(depois.pedido()).isFalse();
        assertThat(depois.pedidoEm()).isNull();
    }

    /* -------------------------------- Validades -------------------------------- */

    @Test
    void validades_oQueEntrouPrimeiroSaiPrimeiroELotesComSaldo() {
        Dados d = new Dados();
        d.insumos.addAll(List.of(insumoCompra(1, "Creme", 0, 10, null, ""), insumoCompra(2, "Farinha", 0, 2, null, "")));
        d.preparacoes.add(prep(10, "Molho", "base", 4, item(1L, null, 1, 1)));
        // Creme: entrou 5 (vence 18/11) e depois 3 (vence 22/11); saiu 4 → restam 4: 3 da entrada nova e 1 da antiga.
        d.movimentos.addAll(List.of(entrada(1, 1, 5, "2026-11-01", "2026-11-18"), entrada(2, 1, 3, "2026-11-10", "2026-11-22"),
            mov(3, "perda", 1, -4, "2026-11-12", null, "manual", null, "Venceu", null, null, null), entrada(4, 2, 10, "2026-11-01", null)));
        // Lotes de evento não baixam insumos (o evento baixa), então não mexem no saldo do creme.
        d.lotes.add(lote(7, 10, 8, 8, "2026-11-20", "evento", 1L, List.of()));
        d.lotes.add(lote(8, 10, 8, 0, "2026-11-19", "evento", 1L, List.of()));

        EstoqueValidadesResponseDTO r = EstoqueIntegrado.validades(d, HOJE);

        assertThat(r.itens()).extracting(x -> x.tipo() + ":" + x.validade() + ":" + x.qtd() + ":" + x.dias())
            .containsExactly("insumo:2026-11-18:1.0:-1", "lote:2026-11-20:8.0:1", "insumo:2026-11-22:3.0:3");
        assertThat(r.itens().get(0).detalhe()).isEqualTo("1 kg · entrada de 01/11/2026");
        assertThat(r.itens().get(1).loteId()).isEqualTo(7L);
        assertThat(r.itens().get(1).insumoId()).isNull();
        // Lote: 8 un × custo da ficha (10 ÷ 4 = 2,50 por un) = 20.
        assertThat(r.itens().get(1).valor()).isEqualTo(20);
        assertThat(r.kpis().vencidos()).isEqualTo(1);
        assertThat(r.kpis().vencem()).isEqualTo(2);
        assertThat(r.kpis().valorRisco()).isEqualTo(10 + 20 + 30);
        // Farinha tem saldo e nenhuma entrada com validade.
        assertThat(r.kpis().semValidade()).isEqualTo(1);
    }

    /* ---------------------------------- Perdas --------------------------------- */

    @Test
    void perdas_doMesComAvariaDescarteDeLoteEComparacaoComAsCompras() {
        Dados d = new Dados();
        d.insumos.addAll(List.of(insumoCompra(1, "Creme", 0, 10, null, ""), insumo(2, "Taça", "louca", "un", "Depósito de louças", 0, 15)));
        d.preparacoes.add(prep(10, "Molho", "base", 4, item(1L, null, 1, 1)));
        d.eventos.add(evento(1, "realizado", "2026-11-10", 50, 0, "", null));
        d.movimentos.addAll(List.of(entrada(1, 1, 20, "2026-11-01", null),
            mov(2, "perda", 1, -2, "2026-11-05", 12.0, "manual", null, "Venceu", null, null, null),
            mov(3, "perda", 2, -2, "2026-11-11", null, "checklist", null, "Avaria", 1L, null, null),
            mov(4, "sobra", 1, 1, "2026-11-11", null, "manual", null, "", 1L, null, null),
            mov(5, "perda", 1, -1, "2026-10-30", null, "manual", null, "Outubro", null, null, null)));
        d.lotes.add(lote(7, 10, 8, 4, "2026-11-20", "restaurante", null,
            List.of(new LoteResponseDTO.Historico("descarte", "2026-11-15", 4, "vencido", null, "", "2026-11-15T10:00:00Z"))));

        EstoquePerdasResponseDTO r = EstoqueIntegrado.perdas(d, "2026-11");

        assertThat(r.perdas()).extracting(x -> x.grupo() + ":" + x.valor())
            .containsExactly("Descarte de pré-preparo:10.0", "Avaria de louça e utensílio:30.0", "Perda de insumo:24.0");
        assertThat(r.perdas().get(0).motivo()).isEqualTo("Venceu");
        assertThat(r.perdas().get(0).unidade()).isEqualTo("un");
        assertThat(r.perdas().get(1).eventoNome()).isEqualTo("Evento 1");
        assertThat(r.kpis().total()).isEqualTo(64);
        assertThat(r.kpis().registros()).isEqualTo(3);
        assertThat(r.kpis().comprasMes()).isEqualTo(200);
        assertThat(r.kpis().pctCompras()).isCloseTo(0.32, within(1e-9));
        assertThat(r.kpis().sobras()).isEqualTo(10);
        assertThat(r.kpis().sobrasRegistros()).isEqualTo(1);
        assertThat(r.kpis().maiorCausa()).isEqualTo("Venceu");
        assertThat(r.porGrupo()).hasSize(3);

        EstoquePerdasResponseDTO vazio = EstoqueIntegrado.perdas(d, "2025-01");
        assertThat(vazio.perdas()).isEmpty();
        assertThat(vazio.kpis().pctCompras()).isNull();
        assertThat(vazio.kpis().maiorCausa()).isNull();
    }

    /* -------------------------------- Conferência -------------------------------- */

    @Test
    void conferencia_apontaDuplicidadeNotaIncompletaSaldoNegativoELoteSemEvento() {
        Dados d = new Dados();
        d.insumos.addAll(List.of(insumoCompra(1, "Creme", 0, 10, null, ""), insumoCompra(2, "Leite", 0, 5, null, "")));
        d.preparacoes.add(prep(10, "Molho", "base", 4, item(1L, null, 1, 1)));
        d.movimentos.addAll(List.of(entrada(1, 1, 5, "2026-11-01", null), entrada(2, 1, 5, "2026-11-01", null),
            mov(3, "entrada", 2, 1, "2026-11-02", null, "nf", null, "", null, 9L, null),
            mov(4, "consumo", 2, -3, "2026-11-03", null, "consumo", null, "", null, null, "restaurante")));
        d.nfs.add(new NotaFiscalResponseDTO(9L, "Laticínios", "123", "2026-11-02", null, false, null, "boleto", List.of(), false, "lancada",
            List.of(itemNf(), itemNf()), 0, 10, "", "", "", "", false, false, List.of(3L), List.of(), "", "", "buffet"));
        d.lotes.add(lote(7, 10, 8, 4, "2026-11-20", "restaurante", null,
            List.of(new LoteResponseDTO.Historico("uso", "2026-11-15", 2, "evento", null, "", "2026-11-15T10:00:00Z"),
                new LoteResponseDTO.Historico("uso", "2026-11-16", 2, "evento", 1L, "", "2026-11-16T10:00:00Z"))));

        EstoqueConferenciaResponseDTO r = EstoqueIntegrado.conferencia(d);

        assertThat(r.pontos()).extracting(x -> x.tom() + ":" + (x.insumoId() != null ? "i" + x.insumoId() : x.loteId() != null ? "l" + x.loteId() : "n" + x.nfId()))
            .containsExactly("warn:l7", "warn:i1", "bad:n9", "bad:i2");
        assertThat(r.pontos().get(1).texto()).isEqualTo("2 movimentos iguais de Creme (Entrada, 5 kg, 01/11/2026). Confira se não foi lançado duas vezes.");
        assertThat(r.pontos().get(3).texto()).startsWith("Leite com saldo negativo (−2 kg)");
        assertThat(r.porOrigem()).extracting(EstoqueConferenciaResponseDTO.PorOrigem::origem)
            .contains("Lançamento manual", "Compra recebida (NF)", "Consumo · Restaurante", "Produção de pré-preparo");
        assertThat(r.porOrigem().get(0).n()).isGreaterThanOrEqualTo(r.porOrigem().get(r.porOrigem().size() - 1).n());
    }

    private static NotaFiscalResponseDTO.Item itemNf() {
        return new NotaFiscalResponseDTO.Item("Leite", "", "un", 2L, 1, "estoque", 1, 5.0, 5, true);
    }

    /* ------------------------- Abatimento do uso de lote ------------------------- */

    @Test
    void usoDeLoteNoEvento_abateDaBaixaDoChecklistDaMesmaPreparacao() {
        Dados d = new Dados();
        d.insumos.add(insumo(1, 10));
        d.preparacoes.add(prep(10, "Molho", "base", 1, item(1L, null, 1, 1)));
        d.eventos.add(evento(1, "em_producao", 10));
        d.cardapio.add(cardapio(100, 1, 10, 1, "sim"));
        d.execucoes.add(producao(100, 10));
        assertThat(Estoque.calcular(d).fisico(1L)).isEqualTo(-10);

        // 4 un do lote do restaurante (que já baixou os insumos) foram usadas no evento 1: o checklist baixa só 6.
        d.lotes.add(lote(7, 10, 4, 0, "", "restaurante", null,
            List.of(new LoteResponseDTO.Historico("uso", "2026-11-15", 4, "evento", 1L, "", "2026-11-15T10:00:00Z"))));
        Dados d2 = new Dados();
        d2.insumos.addAll(d.insumos);
        d2.preparacoes.addAll(d.preparacoes);
        d2.eventos.addAll(d.eventos);
        d2.cardapio.addAll(d.cardapio);
        d2.execucoes.addAll(d.execucoes);
        d2.lotes.addAll(d.lotes);
        assertThat(Estoque.usoDeLoteNoEvento(d2, 1L, 10L)).isEqualTo(4);
        // −6 do checklist e −4 da produção do próprio lote: o total não dobra.
        assertThat(Estoque.calcular(d2).fisico(1L)).isEqualTo(-10);
        assertThat(Estoque.baixasVirtuais(d2)).extracting(MovimentoResponseDTO::qtd).containsExactlyInAnyOrder(-6.0, -4.0);
    }
}
