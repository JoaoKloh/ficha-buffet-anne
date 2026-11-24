package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.TestRsaKeys;
import com.jbkloh.ficha_buffet.dtos.req.CategoriaFinanceiraRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ContratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.EventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LancamentoCaixaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ParcelaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PessoaEquipeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.DesvioCustoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.IndicadoresEventoDTO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Indicadores e previsto × realizado do evento no detalhe (det.indicadores / det.desvios). */
@SpringBootTest
@Transactional
class EventoIndicadoresTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    @Autowired private EventoService eventoService;
    @Autowired private EquipeEventoService equipeService;
    @Autowired private CategoriaFinanceiraService categoriaService;
    @Autowired private CaixaService caixaService;
    @Autowired private ParcelaService parcelaService;
    @Autowired private ObjectMapper json;

    private Long categoria(String nome, String grupo) {
        return categoriaService.criarCategoria(new CategoriaFinanceiraRequestDTO(nome, grupo, 1)).id();
    }

    @Test
    void detalhe_trazIndicadoresPorTipoDeCustoEDesviosSemPrevisto() {
        String hoje = LocalDate.now().toString();
        Long receita = categoria("Eventos sociais", "receita");
        Long ab = categoria("Alimentos e bebidas", "custo_direto");
        Long frete = categoria("Frete e transporte", "despesa_variavel");
        categoria("Mão de obra", "custo_direto");
        EventoResponseDTO e = eventoService.criarEvento(new EventoRequestDTO("Casamento Indicadores", null,
            new EventoRequestDTO.NovoCliente("Cliente Indicadores", "11 99999-0000"), "casamento", "lead", LocalDate.now().plusDays(20).toString(), "19:30",
            "Salão", "São Paulo", 40, 0.0, ""));

        // Sem contrato nem custos: tudo zerado, sem desvio.
        EventoDetalheResponseDTO vazio = eventoService.detalharEvento(e.id());
        assertThat(vazio.indicadores().contratado()).isZero();
        assertThat(vazio.indicadores().convidados()).isEqualTo(40);
        assertThat(vazio.indicadores().cmvRealizado()).isNull();
        assertThat(vazio.desvios()).isEmpty();

        eventoService.registrarContrato(e.id(), new ContratoRequestDTO("C-9", hoje, 50, new BigDecimal("20.00"), new BigDecimal("1000.00"), BigDecimal.ZERO,
            receita, 2, LocalDate.now().plusDays(5).toString(), 1));
        Long parcela = eventoService.detalharEvento(e.id()).parcelas().get(0).id();
        parcelaService.atualizarParcela(parcela, new ParcelaRequestDTO(LocalDate.now().plusDays(5).toString(), new BigDecimal("500.00"), hoje, "pix", "banco"));
        equipeService.criar(e.id(), new PessoaEquipeRequestDTO(null, "Garçom Indicadores", "Garçom", "", "", new BigDecimal("300.00"), "", ""));
        caixaService.criarLancamento(new LancamentoCaixaRequestDTO("Compra A&B", new BigDecimal("250.00"), ab, e.id(), hoje, "realizado", null, hoje, "banco",
            "buffet", "Mercado"));
        caixaService.criarLancamento(new LancamentoCaixaRequestDTO("Frete", new BigDecimal("80.00"), frete, e.id(), hoje, "previsto", hoje, null, "banco",
            "buffet", "Transportadora"));
        equipeService.lancar(e.id());

        EventoDetalheResponseDTO det = eventoService.detalharEvento(e.id());
        IndicadoresEventoDTO i = det.indicadores();
        assertThat(i.contratado()).isEqualTo(1000);
        assertThat(i.receitaPrevista()).isEqualTo(1000);
        assertThat(i.comExtras()).isFalse();
        assertThat(i.recebido()).isEqualTo(500);
        assertThat(i.aReceber()).isEqualTo(500);
        assertThat(i.convidados()).isEqualTo(50);
        // Previsto: A&B pelas fichas (cardápio vazio) e equipe pela escala; transporte e outros sem orçamento.
        assertThat(i.previsto().ab()).isZero();
        assertThat(i.previsto().equipe()).isEqualTo(300);
        assertThat(i.totalPrevisto()).isEqualTo(300);
        // Realizado pelo tipo da categoria: A&B 250 (pago), equipe 300 (diária lançada), transporte 80 (frete em aberto).
        assertThat(i.realizado().ab()).isEqualTo(250);
        assertThat(i.realizado().equipe()).isEqualTo(300);
        assertThat(i.realizado().transporte()).isEqualTo(80);
        assertThat(i.realizado().outros()).isZero();
        assertThat(i.totalRealizado()).isEqualTo(630);
        assertThat(i.aPagar()).isEqualTo(380);
        assertThat(i.contasAPagar()).isEqualTo(2);
        assertThat(i.resultadoPrevisto()).isEqualTo(700);
        assertThat(i.resultadoRealizado()).isEqualTo(370);
        assertThat(i.cmvPrevisto()).isZero();
        assertThat(i.cmvRealizado()).isCloseTo(0.25, within(1e-9));
        // A&B e transporte com realizado e sem previsto; equipe dentro do previsto.
        assertThat(det.desvios()).extracting(DesvioCustoDTO::tipo).containsExactly("ab", "transporte");
        assertThat(det.desvios()).allSatisfy(x -> assertThat(x.desvio()).isNull());

        JsonNode j = json.valueToTree(det);
        assertThat(j.get("indicadores").get("previsto").get("equipe").asDouble()).isEqualTo(300);
        assertThat(j.get("indicadores").get("realizado").has("total")).isFalse();
        assertThat(j.get("desvios").get(0).get("tipo").asString()).isEqualTo("ab");
    }
}
