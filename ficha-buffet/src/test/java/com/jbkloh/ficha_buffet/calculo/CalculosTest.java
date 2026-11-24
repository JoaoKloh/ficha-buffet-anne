package com.jbkloh.ficha_buffet.calculo;

import static com.jbkloh.ficha_buffet.calculo.Fixtures.cardapio;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.categoria;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.evento;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.insumo;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.item;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.lanc;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.lote;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.mov;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.parcela;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.prep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.AlertaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CmvDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CustoPreparacaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ImportarCardapioAnaliseResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalImportadaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PainelResumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ResultadoEventoDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;

/** Regras de negócio das telas, sobre dados em memória: fichas, eventos, caixa, estoque, notas e painel. */
class CalculosTest {

    /* ------------------------------ Fichas técnicas ------------------------------ */

    private static Dados fichas() {
        Dados d = new Dados();
        d.insumos.addAll(List.of(insumo(1, 40), insumo(2, 0), insumo(3, 10)));
        d.preparacoes.addAll(List.of(
            prep(1, "Molho", "base", 2, item(1L, null, 1, 1.25)),
            prep(2, "Prato", "volante", 4, item(null, 1L, 1, 1), item(3L, null, 2, 1)),
            prep(3, "Sem preço", "volante", 10, item(2L, null, 1, 1)),
            prep(4, "Vazia", "volante", 10)));
        return d;
    }

    @Test
    void custoDaFicha_aplicaFatorDeCorrecaoECustoPorUnidadeDasSubReceitas() {
        Dados d = fichas();
        assertThat(Fichas.custoPrep(d, d.preparacao(1L))).isEqualTo(new CustoPreparacaoDTO(50, 25, 0));
        // 1 un do molho (25) + 2 × 10 = 45; rende 4.
        assertThat(Fichas.custoPrep(d, d.preparacao(2L))).isEqualTo(new CustoPreparacaoDTO(45, 11.25, 0));
    }

    @Test
    void situacaoDaFicha_completaSemPrecoOuIncompleta() {
        Dados d = fichas();
        assertThat(Fichas.fichaStatus(d, d.preparacao(2L))).isEqualTo("Completa");
        assertThat(Fichas.fichaStatus(d, d.preparacao(3L))).isEqualTo("Sem preço");
        assertThat(Fichas.fichaStatus(d, d.preparacao(4L))).isEqualTo("Incompleta");
    }

    @Test
    void custoDaFicha_naoEntraEmLacoComSubReceitaCircular() {
        Dados d = new Dados();
        d.preparacoes.add(prep(1, "Circular", "base", 1, item(null, 1L, 1, 1)));
        assertThat(Fichas.custoPrep(d, d.preparacao(1L)).total()).isZero();
    }

    @Test
    void cmvDoPratoDeVenda_somaFichasEInsumosDiretosEDividePeloPreco() {
        Dados d = fichas();
        CmvDTO r = Fichas.cmv(d, List.of(new PratoVendaResponseDTO.Componente(2L, 2)), List.of(new PratoVendaResponseDTO.Extra(3L, 0.5)), 100);
        assertThat(r.custo()).isCloseTo(27.5, within(1e-9));
        assertThat(r.cmv()).isCloseTo(0.275, within(1e-9));
        assertThat(Fichas.cmv(d, List.of(), List.of(), 0).cmv()).isNull();
    }

    /* ---------------------------------- Eventos ---------------------------------- */

    @Test
    void convidados_usaOsContratadosEArredondaAMargemParaCima() {
        EventoResponseDTO e = evento(1, "contratado", "2026-12-12", 90, 5, "", new EventoResponseDTO.Contrato("", "2026-01-01", 101, 0, 1000, 0, 1L));
        assertThat(Eventos.convidados(e)).isEqualTo(new Eventos.Convidados(101, 5, 107));
        assertThat(Eventos.convidados(evento(2, "lead", 100)).n()).isEqualTo(100);
    }

    @Test
    void necessidades_explodeSubReceitasESeparaConfirmadosDeADefinir() {
        Dados d = new Dados();
        d.insumos.addAll(List.of(insumo(1, 20), insumo(2, 0)));
        d.preparacoes.addAll(List.of(
            prep(10, "Massa", "base", 2, item(1L, null, 1, 1)),
            prep(11, "Quiche", "volante", 50, item(null, 10L, 1, 1), item(2L, null, 0.5, 1.2)),
            prep(12, "Bolo", "sobremesa", 10, item(1L, null, 1, 1))));
        d.eventos.add(evento(1, "contratado", 100));
        d.cardapio.addAll(List.of(cardapio(1, 1, 11, 1, "sim"), cardapio(2, 1, 12, 1, "definir")));

        Eventos.Necessidades nd = Eventos.necessidades(d, 1L);
        Eventos.NecessidadeInsumo i1 = nd.insumos().stream().filter(x -> x.insumo.id() == 1).findFirst().orElseThrow();
        // Quiche: 100 un / 50 = 2 receitas → 2 × 1 massa (rende 2) = 1 receita de massa → 1 kg do insumo 1.
        assertThat(i1.q).isCloseTo(1, within(1e-9));
        // Bolo a definir: 100/10 = 10 receitas → 10 kg, só em qDef.
        assertThat(i1.qDef).isCloseTo(10, within(1e-9));
        assertThat(nd.faltando()).isEqualTo(1);
        assertThat(nd.custo()).isCloseTo(20, within(1e-9));
        assertThat(nd.custoDef()).isCloseTo(200, within(1e-9));
        assertThat(nd.pratos()).extracting(p -> p.preparacao().nome()).containsExactly("Quiche", "Bolo");
    }

    @Test
    void parcelas_divideEmPartesIguaisEPoeORestoNaUltima() {
        assertThat(Caixa.dividirParcelas(1000, 3)).containsExactly(333.33, 333.33, 333.34);
        assertThat(Caixa.dividirParcelas(500, 1)).containsExactly(500.0);
    }

    @Test
    void resultadoETravaDoEvento() {
        Dados d = new Dados();
        d.categorias.addAll(List.of(categoria(1, "receita"), categoria(2, "custo_direto")));
        d.eventos.add(evento(1, "contratado", 100));
        d.lancamentos.addAll(List.of(lanc(1, 1, 1L, 1000, "previsto", "2026-05-10"), lanc(2, 2, 1L, 300, "realizado", "2026-05-10"),
            lanc(3, 2, 1L, 50, "cancelado", "2026-05-10")));
        assertThat(Eventos.resultado(d, 1L, false)).isEqualTo(new ResultadoEventoDTO(1000, 300, 700, 0.7));
        assertThat(Eventos.resultado(d, 1L, true).rec()).isZero();
        assertThat(Eventos.travado(d, d.evento(1L))).isEqualTo("1 lançamento pago ou recebido");
    }

    @Test
    void importarCardapio_leOCabecalhoECasaLinhasComAsFichas() {
        Dados d = new Dados();
        d.preparacoes.addAll(List.of(prep(1, "Mini quiche de alho-poró", "volante", 10), prep(2, "Risoto de brie e aspargos", "principal", 10)));
        ImportarCardapioAnaliseResponseDTO r = ImportacaoCardapio.analisar(d, 1L,
            "Casamento — 13/11 — 150 convidados\n- quiche alho poró\nRisoto brie e aspargos\nPicolé de manga");
        assertThat(r.cabecalho()).contains("150 convidados");
        assertThat(r.linhas()).extracting(ImportarCardapioAnaliseResponseDTO.Linha::situacao).containsExactly("ok", "ok", "nao");
        assertThat(r.linhas().get(0).sugestoes().get(0).preparacaoId()).isEqualTo(1L);
    }

    /* ----------------------------------- Caixa ----------------------------------- */

    @Test
    void dre_montaACascataSoComRealizadosEIncluiPrevistosQuandoPedido() {
        Dados d = new Dados();
        List<String> grupos = List.copyOf(Dominio.GRUPOS_DRE.keySet());
        for (int i = 0; i < grupos.size(); i++) {
            d.categorias.add(categoria(i + 1, grupos.get(i)));
        }
        d.lancamentos.addAll(List.of(lanc(1, 1, null, 10000, "realizado", "2026-05-10"), lanc(2, 2, null, 600, "realizado", "2026-05-10"),
            lanc(3, 3, null, 4000, "realizado", "2026-05-10"), lanc(4, 4, null, 400, "realizado", "2026-05-10"), lanc(5, 5, null, 2000, "realizado", "2026-05-10"),
            lanc(6, 6, null, 300, "realizado", "2026-05-10"), lanc(7, 3, null, 999, "previsto", "2026-05-10"), lanc(8, 3, null, 777, "cancelado", "2026-05-10"),
            lanc(9, 3, null, 50, "realizado", "2026-06-01")));

        Map<String, Caixa.DreGrupo> realizado = Caixa.dreCalc(d, data -> data.startsWith("2026-05"), false);
        assertThat(Caixa.dreLinhas(realizado)).isEqualTo(new Caixa.DreLinhas(10000, 9400, 5400, 5000, 3000, 2700));
        assertThat(realizado.get("custo_direto").categorias.get(3L)).isEqualTo(4000);
        assertThat(Caixa.dreCalc(d, data -> data.startsWith("2026-05"), true).get("custo_direto").total).isEqualTo(4999);
    }

    @Test
    void dre_regimeDeCaixaContaSoOQueFoiPagoNoPeriodo_eCompetenciaDeEventoEhADataDoEvento() {
        Dados d = new Dados();
        d.categorias.addAll(List.of(categoria(1, "receita"), categoria(2, "custo_direto")));
        d.eventos.add(evento(1, "contratado", "2026-07-20", 50, 0, "", null));
        LancamentoCaixaResponseDTO base = lanc(1, 1, 1L, 1000, "previsto", "2026-05-02");
        // Recebeu 400 em maio e 600 ainda em aberto; a competência é a data do evento (julho).
        LancamentoCaixaResponseDTO parcial = new LancamentoCaixaResponseDTO(base.id(), base.tipo(), base.categoriaId(), base.eventoId(), base.descricao(),
            base.valor(), base.dataCompetencia(), null, null, "previsto", "parcela", "banco", null, null, null, null, null, null, base.criadoEm(), null, null, null,
            null, null, null, null, null, null, List.of(new LancamentoCaixaResponseDTO.Pagamento(1L, "2026-05-10", 400, "banco", "pix", null, "")), null, null,
            null);
        d.lancamentos.add(parcial);

        assertThat(Caixa.pago(parcial)).isEqualTo(400);
        assertThat(Caixa.saldo(parcial)).isEqualTo(600);
        assertThat(Caixa.competencia(d, parcial)).isEqualTo("2026-07-20");
        assertThat(Caixa.unidadeDe(parcial)).isEqualTo("buffet");
        assertThat(Caixa.dreCalc(d, data -> data.startsWith("2026-05"), false, "", "caixa", true).get("receita").total).isEqualTo(400);
        assertThat(Caixa.dreCalc(d, data -> data.startsWith("2026-05"), true).get("receita").total).isZero();
        assertThat(Caixa.dreCalc(d, data -> data.startsWith("2026-07"), true).get("receita").total).isEqualTo(1000);
        assertThat(Caixa.dreCalc(d, data -> data.startsWith("2026-07"), false).get("receita").total).isEqualTo(400);
        // Realizado sem histórico (anterior aos pagamentos em partes) conta como pago de uma vez.
        assertThat(Caixa.pago(lanc(2, 2, null, 250, "realizado", "2026-05-03"))).isEqualTo(250);
    }

    /* ---------------------------------- Estoque ---------------------------------- */

    private static Dados estoque() {
        Dados d = new Dados();
        d.insumos.addAll(List.of(insumo(1, "Farinha", "ingrediente", "kg", "Seco", 5, 10), insumo(2, "Bandeja", "louca", "un", "Depósito de louças", 0, 0)));
        d.preparacoes.add(prep(10, "Coxinha", "volante", 10, item(1L, null, 2, 1)));
        d.eventos.addAll(List.of(evento(1, "contratado", 50), evento(2, "lead", 50)));
        d.cardapio.addAll(List.of(cardapio(100, 1, 10, 1, "sim"), cardapio(101, 2, 10, 1, "sim")));
        d.movimentos.addAll(List.of(mov(1, 1, 12, "entrada"), mov(2, 1, -1, "perda")));
        return d;
    }

    @Test
    void saldo_fisicoSomaMovimentosEReservaSoVemDeEventoContratado() {
        Dados d = estoque();
        Estoque est = Estoque.calcular(d);
        assertThat(est.fisico(1L)).isEqualTo(11);
        // 50 convidados / rende 10 = 5 receitas × 2 kg.
        assertThat(est.reservado(1L)).isEqualTo(10);
        assertThat(est.disponivel(1L)).isEqualTo(1);
        assertThat(est.situacao(d.insumo(1L))).containsExactly("warn", "Abaixo do mínimo");
    }

    @Test
    void producaoNoChecklist_daBaixaVirtualELiberaAReserva() {
        Dados d = estoque();
        d.execucoes.add(Fixtures.producao(100, 50));
        Estoque est = Estoque.calcular(d);
        assertThat(Estoque.baixasVirtuais(d)).extracting(MovimentoResponseDTO::insumoId, MovimentoResponseDTO::qtd).containsExactly(org.assertj.core.groups.Tuple.tuple(1L, -10.0));
        assertThat(est.fisico(1L)).isEqualTo(1);
        assertThat(est.reservado(1L)).isZero();
    }

    @Test
    void loteDoRestauranteBaixaInsumos_loteDeEventoNao() {
        Dados restaurante = estoque();
        restaurante.lotes.add(lote(1, 10, 20, 20, "", "restaurante"));
        assertThat(Estoque.baixasVirtuais(restaurante)).extracting(MovimentoResponseDTO::qtd).containsExactly(-4.0);
        Dados deEvento = estoque();
        deEvento.lotes.add(lote(1, 10, 20, 20, "", "evento"));
        assertThat(Estoque.baixasVirtuais(deEvento)).isEmpty();
    }

    @Test
    void situacaoDaLoucaNaoOlhaReservas_eValidadeDoLote() {
        Dados d = estoque();
        d.movimentos.add(mov(3, 2, 30, "entrada"));
        assertThat(Estoque.calcular(d).situacao(d.insumo(2L))).containsExactly("good", "OK");
        assertThat(Estoque.loteSituacao(lote(1, 10, 20, 20, "2026-11-09", "restaurante"), "2026-11-10").get(1)).isEqualTo("Vencido");
        assertThat(Estoque.loteSituacao(lote(1, 10, 20, 20, "2026-11-10", "restaurante"), "2026-11-10").get(1)).isEqualTo("Vence hoje");
        assertThat(Estoque.loteSituacao(lote(1, 10, 20, 20, "2026-11-11", "restaurante"), "2026-11-10").get(1)).isEqualTo("Vence amanhã");
        assertThat(Estoque.loteSituacao(lote(1, 10, 20, 0, "", "restaurante"), "2026-11-10").get(1)).isEqualTo("Esgotado");
    }

    /* ------------------------------- Notas fiscais ------------------------------- */

    @Test
    void notaFiscal_totalEConversaoPorEmbalagem() {
        List<NotaFiscalResponseDTO.Item> itens = List.of(new NotaFiscalResponseDTO.Item("", "", "", null, 0, "estoque", 0, null, 10.1, false),
            new NotaFiscalResponseDTO.Item("", "", "", null, 0, "estoque", 0, null, 5.2, false));
        assertThat(NotasFiscais.total(itens, 1.33)).isEqualTo(16.63);
        assertThat(NotasFiscais.qtdConvertida(3, "compra", 0.2)).isEqualTo(0.6);
        assertThat(NotasFiscais.qtdConvertida(3, "estoque", 0.2)).isEqualTo(3);
    }

    @Test
    void xmlDaNFe_rateiaFreteEIpiNasLinhasELeAsDuplicatas() {
        String xml = """
            <?xml version="1.0"?><nfeProc><NFe><infNFe Id="NFe35260112345678000199550010000012341000012345"><ide><nNF>1234</nNF><serie>1</serie><dhEmi>2026-11-03T10:00:00-03:00</dhEmi></ide>
            <emit><CNPJ>12345678000199</CNPJ><xNome>Hortifruti Serra LTDA</xNome><xFant>Hortifruti Serra</xFant></emit>
            <det nItem="1"><prod><cProd>R1</cProd><xProd>RUCULA</xProd><uCom>MC</uCom><qCom>10</qCom><vProd>30.00</vProd><vFrete>2.00</vFrete></prod><imposto><IPI><vIPI>1.00</vIPI></IPI></imposto></det>
            <det nItem="2"><prod><cProd>T2</cProd><xProd>TOMATE</xProd><uCom>KG</uCom><qCom>5</qCom><vProd>40.00</vProd><vDesc>4.00</vDesc></prod><imposto></imposto></det>
            <total><ICMSTot><vNF>69.00</vNF></ICMSTot></total><cobr><dup><dVenc>2026-11-20</dVenc><vDup>34.50</vDup></dup><dup><dVenc>2026-12-20</dVenc><vDup>34.50</vDup></dup></cobr></infNFe></NFe></nfeProc>""";
        NotaFiscalImportadaDTO x = NotasFiscais.lerNFeXML(xml);
        assertThat(x.chave()).isEqualTo("35260112345678000199550010000012341000012345");
        assertThat(x.fornecedor()).isEqualTo("Hortifruti Serra");
        assertThat(x.itens()).extracting(NotaFiscalResponseDTO.Item::desc, NotaFiscalResponseDTO.Item::qtdNota, NotaFiscalResponseDTO.Item::valor)
            .containsExactly(org.assertj.core.groups.Tuple.tuple("RUCULA", 10.0, 33.0), org.assertj.core.groups.Tuple.tuple("TOMATE", 5.0, 36.0));
        assertThat(x.total()).isEqualTo(69);
        assertThat(x.formaPag()).isEqualTo("boleto");
        assertThat(x.parcelas()).extracting(NotaFiscalResponseDTO.Parcela::venc).containsExactly("2026-11-20", "2026-12-20");
        assertThatThrownBy(() -> NotasFiscais.lerNFeXML("<nada/>")).isInstanceOf(AppException.class).hasMessageContaining("infNFe");
    }

    /* ----------------------------------- Painel ----------------------------------- */

    @Test
    void painel_kpisDoMesEAlertasOrdenadosPorUrgencia() {
        Dados d = new Dados();
        d.categorias.addAll(List.of(categoria(1, "receita"), categoria(2, "custo_direto")));
        d.eventos.add(evento(1, "contratado", "2026-12-12", 100, 0, "", null));
        d.parcelas.addAll(List.of(parcela(1, 1, "2026-11-01", 1000, null), parcela(2, 1, "2026-11-20", 500, null), parcela(3, 1, "2026-10-01", 700, "2026-10-01")));
        d.lancamentos.addAll(List.of(lanc(1, 1, null, 3000, "realizado", "2026-11-05"), lanc(2, 2, null, 1000, "previsto", "2026-11-08"),
            lanc(3, 1, null, 1500, "previsto", "2026-11-01")));

        PainelResumoResponseDTO.Kpis kpis = Painel.kpis(d, "2026-11-10");
        assertThat(kpis.faturamento()).isEqualTo(4500);
        assertThat(kpis.recebido()).isEqualTo(3000);
        assertThat(kpis.resultado()).isEqualTo(3500);
        assertThat(kpis.aReceber()).isEqualTo(1500);
        assertThat(kpis.contasReceber()).isEqualTo(1);
        assertThat(kpis.receberVencidas()).isEqualTo(1);
        assertThat(kpis.aPagar()).isEqualTo(1000);
        assertThat(kpis.pagarVencidas()).isEqualTo(1);

        List<AlertaDTO> alertas = Painel.alertas(d, null, "2026-11-10");
        assertThat(alertas).isSortedAccordingTo(java.util.Comparator.comparingLong(AlertaDTO::ord));
        // Parcela vencida há 9 dias, conta a pagar vencida, conta a receber vencida, cardápio a 2 dias do prazo (12/11),
        // nenhum backup registrado e lançamentos sem unidade de negócio.
        assertThat(alertas).extracting(AlertaDTO::ord).contains(-9L, -1L, 0L, 2L, 4L, 60L);
        assertThat(Painel.alertas(d, "2026-11-08T12:00:00Z", "2026-11-10")).extracting(AlertaDTO::ord).doesNotContain(4L);

        PainelResumoResponseDTO.Graficos g = Painel.graficos(d, "2026-11-10");
        assertThat(g.meses()).hasSize(12).contains("2026-11");
        assertThat(g.meses().get(g.mesAtual())).isEqualTo("2026-11");
        assertThat(g.receitas().get(5)).isEqualTo(4500);
        assertThat(g.eventos().get(1)).isEqualTo(1);
        assertThat(Painel.proximosEventos(d, "2026-11-10")).hasSize(1);
    }
}
