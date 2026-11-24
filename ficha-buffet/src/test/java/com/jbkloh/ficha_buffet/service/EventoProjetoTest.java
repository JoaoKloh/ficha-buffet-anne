package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.TestRsaKeys;
import com.jbkloh.ficha_buffet.dtos.req.CategoriaFinanceiraRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ContratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.EventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PatchEventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PessoaEquipeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarExecucaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarLogisticaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarOrcamentoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EtapaEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FechamentoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ProjetoEventoDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.repositories.EventoRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Evento como projeto: logística, orçamento, assinaturas, pendências e fechamento (det.projeto). */
@SpringBootTest
@Transactional
class EventoProjetoTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    @Autowired private EventoService eventoService;
    @Autowired private ProjetoEventoService projetoService;
    @Autowired private EquipeEventoService equipeService;
    @Autowired private CategoriaFinanceiraService categoriaService;
    @Autowired private CozinhaService cozinhaService;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private ObjectMapper json;

    private EventoResponseDTO evento(String nome) {
        return eventoService.criarEvento(new EventoRequestDTO(nome, null, new EventoRequestDTO.NovoCliente("Cliente " + nome, "11 99999-0000"), "casamento",
            "lead", LocalDate.now().plusDays(20).toString(), "19:30", "Salão", "São Paulo", 40, 0.0, ""));
    }

    private static EtapaEventoDTO etapa(EventoDetalheResponseDTO det, String k) {
        return det.etapas().stream().filter(x -> k.equals(x.etapa())).findFirst().orElseThrow();
    }

    private static HttpStatus status(Throwable t) {
        if (!(t instanceof AppException e)) {
            throw new AssertionError("Esperava AppException, veio " + t, t);
        }
        return e.getHttpStatus();
    }

    @Test
    void logistica_semPlanoSaiVaziaESalvarDetectaAEtapa() {
        EventoResponseDTO e = evento("Logística");
        EventoDetalheResponseDTO antes = eventoService.detalharEvento(e.id());
        assertThat(antes.projeto().logistica().veiculos()).isEmpty();
        assertThat(antes.projeto().logistica().atualizadoEm()).isNull();
        assertThat(antes.projeto().fechamento()).isNull();
        assertThat(antes.projeto().assinaturas().carga()).isNull();
        assertThat(etapa(antes, "logistica").feito()).isFalse();

        // Só veículo, sem saída: ainda não é plano completo.
        projetoService.salvarLogistica(e.id(), new SalvarLogisticaRequestDTO(" 2 vans ", "Mauricio", "2", "", "", "", null));
        EventoDetalheResponseDTO parcial = eventoService.detalharEvento(e.id());
        assertThat(etapa(parcial, "logistica").feito()).isFalse();
        assertThat(etapa(parcial, "logistica").info()).isEqualTo("2 vans");

        projetoService.salvarLogistica(e.id(), new SalvarLogisticaRequestDTO("2 vans", "Mauricio", "2", "09:00", "14:30", "23:00", "Doca nos fundos"));
        EventoDetalheResponseDTO det = eventoService.detalharEvento(e.id());
        ProjetoEventoDTO p = det.projeto();
        assertThat(p.logistica().veiculos()).isEqualTo("2 vans");
        assertThat(p.logistica().motorista()).isEqualTo("Mauricio");
        assertThat(p.logistica().saida()).isEqualTo("09:00");
        assertThat(p.logistica().montagem()).isEqualTo("14:30");
        assertThat(p.logistica().retorno()).isEqualTo("23:00");
        assertThat(p.logistica().obs()).isEqualTo("Doca nos fundos");
        assertThat(p.logistica().atualizadoEm()).isNotNull();
        assertThat(etapa(det, "logistica").feito()).isTrue();
        assertThat(etapa(det, "logistica").auto()).isTrue();
        assertThat(etapa(det, "logistica").info()).isEqualTo("2 vans · saída 09:00");

        // Salvar de novo substitui o plano inteiro: horário vazio volta a ficar a definir.
        projetoService.salvarLogistica(e.id(), new SalvarLogisticaRequestDTO("Fiorino", "", "", "", "", "", ""));
        EventoDetalheResponseDTO depois = eventoService.detalharEvento(e.id());
        assertThat(depois.projeto().logistica().saida()).isEmpty();
        assertThat(depois.projeto().logistica().motorista()).isEmpty();
        assertThat(etapa(depois, "logistica").feito()).isFalse();
    }

    @Test
    void orcamento_entraNoPrevistoEEquipeDaEscalaPrevaleceSobreOrcamento() {
        categoria("Mão de obra", "custo_direto");
        EventoResponseDTO e = evento("Orçamento");
        assertThat(eventoService.detalharEvento(e.id()).projeto().orcamento().equipe()).isNull();

        projetoService.salvarOrcamento(e.id(), new SalvarOrcamentoRequestDTO(new BigDecimal("500.00"), new BigDecimal("3000.00"), new BigDecimal("200.00")));
        EventoDetalheResponseDTO semEscala = eventoService.detalharEvento(e.id());
        assertThat(semEscala.projeto().orcamento().equipe()).isEqualTo(500);
        assertThat(semEscala.projeto().orcamento().transporte()).isEqualTo(3000);
        assertThat(semEscala.projeto().orcamento().atualizadoEm()).isNotNull();
        assertThat(semEscala.indicadores().previsto().equipe()).isEqualTo(500);
        assertThat(semEscala.indicadores().previsto().transporte()).isEqualTo(3000);
        assertThat(semEscala.indicadores().previsto().outros()).isEqualTo(200);
        assertThat(semEscala.indicadores().totalPrevisto()).isEqualTo(3700);

        // Com escala, a equipe prevista é o total das diárias; equipe nula no corpo mantém o valor gravado.
        equipeService.criar(e.id(), new PessoaEquipeRequestDTO(null, "Garçom Orçamento", "Garçom", "", "", new BigDecimal("300.00"), "", ""));
        projetoService.salvarOrcamento(e.id(), new SalvarOrcamentoRequestDTO(null, new BigDecimal("2500.00"), BigDecimal.ZERO));
        EventoDetalheResponseDTO comEscala = eventoService.detalharEvento(e.id());
        assertThat(comEscala.projeto().orcamento().equipe()).isEqualTo(500);
        assertThat(comEscala.projeto().orcamento().transporte()).isEqualTo(2500);
        assertThat(comEscala.indicadores().previsto().equipe()).isEqualTo(300);
        assertThat(comEscala.indicadores().previsto().transporte()).isEqualTo(2500);
        assertThat(comEscala.indicadores().previsto().outros()).isZero();
    }

    @Test
    void assinaturasDoChecklistDeTransporteSaemNoProjeto() {
        EventoResponseDTO e = evento("Assinaturas");
        cozinhaService.salvarExecucao(new SalvarExecucaoRequestDTO("tr_" + e.id(), "transporte", e.id(), null, null, null,
            Map.of("cargaPor", "Lázaro", "cargaHora", "15:10")));
        ProjetoEventoDTO.Assinaturas a = eventoService.detalharEvento(e.id()).projeto().assinaturas();
        assertThat(a.carga()).isEqualTo(new ProjetoEventoDTO.Assinatura("Lázaro", "15:10"));
        assertThat(a.recebimento()).isNull();
        assertThat(a.retorno()).isNull();
    }

    @Test
    void fechar_guardaRetratoPassaParaAcertadoERecusaFecharDeNovo() {
        Long receita = categoria("Eventos sociais", "receita");
        EventoResponseDTO e = evento("Fechamento");
        eventoService.registrarContrato(e.id(), new ContratoRequestDTO("C-1", LocalDate.now().toString(), 40, new BigDecimal("25.00"),
            new BigDecimal("1000.00"), BigDecimal.ZERO, receita, 1, LocalDate.now().plusDays(5).toString(), 1));
        projetoService.salvarOrcamento(e.id(), new SalvarOrcamentoRequestDTO(null, new BigDecimal("100.00"), BigDecimal.ZERO));

        EventoDetalheResponseDTO antes = eventoService.detalharEvento(e.id());
        assertThat(antes.projeto().pendenciasFechamento())
            .contains("O evento ainda não está como Realizado.")
            .anyMatch(x -> x.startsWith("Falta receber ") && x.endsWith(" do cliente."))
            .anyMatch(x -> x.startsWith("Etapas sem marcar: ") && x.contains("Logística e transporte"));

        EventoResponseDTO fechado = eventoService.fecharEvento(e.id());
        assertThat(fechado.status()).isEqualTo("acertado");

        EventoDetalheResponseDTO det = eventoService.detalharEvento(e.id());
        FechamentoEventoDTO f = det.projeto().fechamento();
        assertThat(f).isNotNull();
        assertThat(f.em()).isNotBlank();
        assertThat(f.receita()).isEqualTo(1000);
        assertThat(f.recebido()).isZero();
        assertThat(f.custoPrevisto()).isEqualTo(100);
        assertThat(f.custoRealizado()).isZero();
        assertThat(f.resultado()).isEqualTo(1000);
        assertThat(f.cmv()).isZero();
        assertThat(f.porTipo().previsto().transporte()).isEqualTo(100);
        assertThat(f.pendencias()).contains("logistica", "equipe").doesNotContain("fechamento", "cadastro");
        assertThat(etapa(det, "fechamento").feito()).isTrue();
        assertThat(etapa(det, "fechamento").info()).startsWith("fechado em ");

        assertThat(status(catchThrowable(() -> eventoService.fecharEvento(e.id())))).isEqualTo(HttpStatus.CONFLICT);

        // Reaberto e fechado de novo: o retrato é substituído (um só por evento).
        eventoService.ajustarEvento(e.id(), new PatchEventoRequestDTO("realizado", null, null, null));
        eventoService.fecharEvento(e.id());
        assertThat(eventoService.detalharEvento(e.id()).projeto().fechamento().pendencias()).doesNotContain("execucao");

        JsonNode j = json.valueToTree(eventoService.detalharEvento(e.id()));
        assertThat(j.get("projeto").get("fechamento").get("porTipo").get("realizado").has("total")).isFalse();
        assertThat(j.get("projeto").get("assinaturas").get("carga").isNull()).isTrue();
    }

    @Test
    void fechar_recusaPerdidoOuCanceladoEEventoInexistente() {
        EventoResponseDTO e = evento("Cancelado");
        eventoService.ajustarEvento(e.id(), new PatchEventoRequestDTO("cancelado", null, null, null));
        assertThatThrownBy(() -> eventoService.fecharEvento(e.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> eventoService.fecharEvento(999_999L)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> projetoService.salvarLogistica(999_999L, new SalvarLogisticaRequestDTO("", "", "", "", "", "", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void apagarEvento_levaLogisticaOrcamentoERetrato() {
        EventoResponseDTO e = evento("Apagar projeto");
        projetoService.salvarLogistica(e.id(), new SalvarLogisticaRequestDTO("Van", "", "", "08:00", "", "", ""));
        projetoService.salvarOrcamento(e.id(), new SalvarOrcamentoRequestDTO(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO));
        eventoService.ajustarEvento(e.id(), new PatchEventoRequestDTO("realizado", null, null, null));
        eventoService.fecharEvento(e.id());

        eventoService.apagarEvento(e.id());
        assertThat(eventoRepository.findById(e.id())).isEmpty();
        assertThat(projetoService.fechamento(e.id())).isNull();
        assertThat(projetoService.logistica(e.id()).atualizadoEm()).isNull();
        assertThat(projetoService.orcamento(e.id()).atualizadoEm()).isNull();
    }

    private Long categoria(String nome, String grupo) {
        return categoriaService.criarCategoria(new CategoriaFinanceiraRequestDTO(nome, grupo, 1)).id();
    }
}
