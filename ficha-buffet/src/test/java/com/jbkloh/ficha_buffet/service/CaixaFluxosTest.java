package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import com.jbkloh.ficha_buffet.dtos.req.ClassificarUnidadeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ContratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.EventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LancamentoCaixaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PagamentoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ParcelaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.RateioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SaldosContasRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.TransferenciaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ContasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.DreResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FluxoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoListaItemDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;

/** Unidades de negócio, pagamentos em partes, transferências, saldos e rateio no caixa. */
@SpringBootTest
@Transactional
class CaixaFluxosTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    @Autowired private CaixaService caixaService;
    @Autowired private CategoriaFinanceiraService categoriaService;
    @Autowired private EventoService eventoService;
    @Autowired private ParcelaService parcelaService;

    private static final String HOJE = LocalDate.now().toString();
    private static final String MES = HOJE.substring(0, 7);

    private CategoriaResponseDTO categoria(String nome, String grupo) {
        return categoriaService.criarCategoria(new CategoriaFinanceiraRequestDTO(nome, grupo, 1));
    }

    private LancamentoCaixaResponseDTO lancamento(CategoriaResponseDTO c, String valor, String status, String unidade, String vencimento) {
        return caixaService.criarLancamento(new LancamentoCaixaRequestDTO("Conta " + valor, new BigDecimal(valor), c.id(), null, HOJE, status, vencimento,
            "realizado".equals(status) ? HOJE : null, "banco", unidade, "Fornecedor X"));
    }

    private static HttpStatus status(Throwable t) {
        if (!(t instanceof AppException e)) {
            throw new AssertionError("Esperava AppException, veio " + t, t);
        }
        return e.getHttpStatus();
    }

    private LancamentoListaItemDTO item(Long id) {
        return caixaService.detalharLancamento(id);
    }

    @Test
    void pagamentoEmPartes_acompanhaSaldoESituacao_eODesfazerReabre() {
        CategoriaResponseDTO ab = categoria("Alimentos e bebidas (A&B)", "custo_direto");
        LancamentoCaixaResponseDTO conta = lancamento(ab, "300.00", "previsto", "buffet", HOJE);

        caixaService.registrarPagamento(conta.id(), new PagamentoRequestDTO(new BigDecimal("100.00"), HOJE, "banco", "pix"));
        LancamentoListaItemDTO parcial = item(conta.id());
        assertThat(parcial.lancamento().status()).isEqualTo("previsto");
        assertThat(parcial.pago()).isEqualTo(100);
        assertThat(parcial.saldo()).isEqualTo(200);
        assertThat(parcial.situacao()).isEqualTo("Parcial");
        assertThat(parcial.lancamento().pagamentos()).hasSize(1);

        // Não deixa pagar mais que o saldo.
        assertThatThrownBy(() -> caixaService.registrarPagamento(conta.id(), new PagamentoRequestDTO(new BigDecimal("250.00"), HOJE, "banco", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        caixaService.registrarPagamento(conta.id(), new PagamentoRequestDTO(new BigDecimal("200.00"), HOJE, "caixa", "dinheiro"));
        LancamentoListaItemDTO quitado = item(conta.id());
        assertThat(quitado.lancamento().status()).isEqualTo("realizado");
        assertThat(quitado.lancamento().dataPagamento()).isEqualTo(HOJE);
        assertThat(quitado.saldo()).isZero();

        // Editar para um valor menor que o já pago é recusado.
        assertThatThrownBy(() -> caixaService.atualizarLancamento(conta.id(), new LancamentoCaixaRequestDTO("Conta", new BigDecimal("50.00"), ab.id(), null, HOJE,
            "previsto", HOJE, null, "banco", "buffet", null))).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        caixaService.desfazerPagamento(conta.id());
        LancamentoListaItemDTO reaberto = item(conta.id());
        assertThat(reaberto.lancamento().status()).isEqualTo("previsto");
        assertThat(reaberto.saldo()).isEqualTo(200);

        ContasResponseDTO contas = caixaService.contas("pagar", "parciais", null);
        assertThat(contas.contas()).extracting(c -> c.lancamento().id()).containsExactly(conta.id());
        assertThat(contas.parcial().pago()).isEqualTo(100);
        assertThat(contas.emAberto().valor()).isEqualTo(200);
    }

    @Test
    void lancamentoPagoNaCriacao_ganhaUmPagamentoNoHistorico() {
        CategoriaResponseDTO fixa = categoria("Aluguel da cozinha", "despesa_fixa");
        LancamentoCaixaResponseDTO l = lancamento(fixa, "1200.00", "realizado", "compartilhado", null);
        assertThat(l.status()).isEqualTo("realizado");
        assertThat(l.pagamentos()).hasSize(1);
        assertThat(l.pagamentos().get(0).valor()).isEqualTo(1200);
    }

    @Test
    void parcelaDoContrato_recebidaEmPartesPeloCaixa_ficaRecebidaQuandoQuita() {
        CategoriaResponseDTO receita = categoria("Eventos sociais", "receita");
        EventoResponseDTO evento = eventoService.criarEvento(new EventoRequestDTO("Festa", null, new EventoRequestDTO.NovoCliente("Cliente", ""), "casamento",
            "lead", LocalDate.now().plusDays(40).toString(), "", "", "", 50, 0.0, ""));
        eventoService.registrarContrato(evento.id(), new ContratoRequestDTO("", HOJE, 50, BigDecimal.ZERO, new BigDecimal("1000.00"), BigDecimal.ZERO,
            receita.id(), 1, HOJE, 1));
        EventoDetalheResponseDTO det = eventoService.detalharEvento(evento.id());
        Long lancId = det.parcelas().get(0).lancamentoId();

        caixaService.registrarPagamento(lancId, new PagamentoRequestDTO(new BigDecimal("400.00"), HOJE, "banco", "pix"));
        EventoDetalheResponseDTO parcial = eventoService.detalharEvento(evento.id());
        assertThat(parcial.parcelas().get(0).recebido()).isEqualTo(400);
        assertThat(parcial.parcelas().get(0).pagoEm()).isNull();
        assertThat(parcial.recebido()).isEqualTo(400);
        assertThat(parcial.contratoTemParcelaPaga()).isTrue();
        assertThat(parcial.travado()).contains("1 lançamento");
        assertThatThrownBy(() -> eventoService.apagarContrato(evento.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));

        // "Recebida por inteiro" na parcela completa o que falta.
        Long parcelaId = parcial.parcelas().get(0).id();
        parcelaService.atualizarParcela(parcelaId, new ParcelaRequestDTO(parcial.parcelas().get(0).vencimento(), new BigDecimal("1000.00"), HOJE, "pix", "banco"));
        EventoDetalheResponseDTO quitada = eventoService.detalharEvento(evento.id());
        assertThat(quitada.parcelas().get(0).pagoEm()).isEqualTo(HOJE);
        assertThat(quitada.parcelas().get(0).recebido()).isEqualTo(1000);
        assertThat(item(lancId).lancamento().pagamentos()).extracting(LancamentoCaixaResponseDTO.Pagamento::valor).containsExactly(400.0, 600.0);

        // A receita do evento conta como Buffet sem precisar classificar.
        assertThat(item(lancId).unidadeEfetiva()).isEqualTo("buffet");
    }

    @Test
    void unidadesRateioETransferencias_naDre() {
        CategoriaResponseDTO receita = categoria("Vendas", "receita");
        CategoriaResponseDTO ab = categoria("Alimentos e bebidas (A&B)", "custo_direto");
        CategoriaResponseDTO fixa = categoria("Aluguel", "despesa_fixa");
        lancamento(receita, "5000.00", "realizado", "restaurante", null);
        lancamento(fixa, "1000.00", "realizado", "compartilhado", null);
        LancamentoCaixaResponseDTO semUnidade = caixaService.criarLancamento(new LancamentoCaixaRequestDTO("Avulso", new BigDecimal("10.00"), fixa.id(), null, HOJE,
            "realizado", null, HOJE, "banco", "buffet", null));

        assertThatThrownBy(() -> caixaService.salvarRateio(new RateioRequestDTO(50.0, 30.0, 10.0)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        caixaService.salvarRateio(new RateioRequestDTO(50.0, 30.0, 20.0));

        // Molho da fábrica do buffet usado pelo restaurante: custo sai do Buffet e entra no Restaurante.
        assertThatThrownBy(() -> caixaService.criarTransferencia(new TransferenciaRequestDTO(HOJE, "Ana", "buffet", "buffet", null, "Molho", 2.0, "kg",
            new BigDecimal("80.00"), ab.id(), ""))).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        LancamentoCaixaResponseDTO transf = caixaService.criarTransferencia(new TransferenciaRequestDTO(HOJE, "Ana", "buffet", "restaurante", null, "Molho", 2.0,
            "kg", new BigDecimal("80.00"), ab.id(), ""));
        assertThat(transf.descricao()).isEqualTo("Transferência Buffet → Restaurante · Molho");
        assertThatThrownBy(() -> caixaService.registrarPagamento(transf.id(), new PagamentoRequestDTO(BigDecimal.TEN, HOJE, "banco", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        String periodo = HOJE.substring(5, 7);
        String ano = HOJE.substring(0, 4);
        DreResponseDTO rest = caixaService.dre(ano, periodo, true, "restaurante", "competencia", true);
        // 5000 de receita − 80 de custo transferido − 30% de 1000 da estrutura compartilhada.
        assertThat(rest.resultado()).isCloseTo(5000 - 80 - 300, within(0.001));
        DreResponseDTO buffet = caixaService.dre(ano, periodo, true, "buffet", "competencia", true);
        assertThat(buffet.resultado()).isCloseTo(80 - 500 - 10, within(0.001));
        DreResponseDTO consolidado = caixaService.dre(ano, periodo, true, "", "competencia", true);
        assertThat(consolidado.resultado()).isCloseTo(5000 - 1000 - 10, within(0.001));

        DreResponseDTO cmp = caixaService.dre(ano, periodo, true, "_cmp", "competencia", true);
        assertThat(cmp.comparativo().colunas()).extracting(DreResponseDTO.Coluna::chave).containsExactly("buffet", "restaurante", "chocolates", "_cons");
        assertThat(cmp.comparativo().linhas().get(cmp.comparativo().linhas().size() - 1).valores().get(3)).isCloseTo(3990, within(0.001));

        assertThat(caixaService.transferencias(null).porUnidade()).filteredOn(p -> p.unidade().equals("restaurante"))
            .extracting(p -> p.recebeu()).containsExactly(80.0);

        // Classificar: o lançamento passa para a unidade escolhida.
        caixaService.classificar(new ClassificarUnidadeRequestDTO("chocolates", List.of(semUnidade.id())));
        assertThat(item(semUnidade.id()).unidadeEfetiva()).isEqualTo("chocolates");
        assertThat(caixaService.retornarMes(MES, "todos", "chocolates").lancamentos()).extracting(x -> x.lancamento().id()).contains(semUnidade.id());
    }

    @Test
    void fluxoDeCaixa_somaOQueEntrouESaiuDepoisDoSaldoInformado() {
        CategoriaResponseDTO receita = categoria("Vendas", "receita");
        CategoriaResponseDTO fixa = categoria("Luz", "despesa_fixa");
        String ontem = LocalDate.now().minusDays(1).toString();
        caixaService.salvarSaldos(new SaldosContasRequestDTO(ontem, Map.of("banco", new BigDecimal("1000.00"))));
        lancamento(receita, "500.00", "realizado", "buffet", null);
        lancamento(fixa, "200.00", "realizado", "buffet", null);
        lancamento(fixa, "300.00", "previsto", "buffet", LocalDate.now().plusDays(5).toString());

        FluxoCaixaResponseDTO f = caixaService.fluxo(null);
        assertThat(f.temSaldo()).isTrue();
        assertThat(f.contas()).filteredOn(c -> c.conta().equals("banco")).extracting(FluxoCaixaResponseDTO.Conta::saldo).containsExactly(1300.0);
        assertThat(f.saldoTotal()).isEqualTo(1300);
        assertThat(f.saidas30()).isEqualTo(300);
        assertThat(f.projetado30()).isEqualTo(1000);
        assertThat(f.recebidoMes()).isEqualTo(500);
    }

    @Test
    void lancamentoSemUnidade_eRecusado() {
        CategoriaResponseDTO fixa = categoria("Luz", "despesa_fixa");
        assertThat(caixaService.semUnidade().total()).isZero();
        // O DTO exige a unidade: o controller responde 400 antes de chegar aqui.
        LancamentoCaixaRequestDTO req = new LancamentoCaixaRequestDTO("Luz", BigDecimal.TEN, fixa.id(), null, HOJE, "previsto", null, null, "banco", null, null);
        assertThat(jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator().validate(req))
            .extracting(v -> v.getMessage()).contains("Escolha a unidade de negócio.");
    }
}
