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
import com.jbkloh.ficha_buffet.dtos.req.AjustarEquipeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarSetoresRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarLiberacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PlanoOrdemRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LiberarMapaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AutorizarLiberacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AcaoOrdemRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AjustarEtapaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CategoriaFinanceiraRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ContratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CriarOcorrenciaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.EventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.InsumoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.InventarioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ItemCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LotePreparoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AdicionarItensPlanoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CalcularCartaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentarLoteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentoEstoqueRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PratoVendaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarPlanoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.NotaFiscalRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ParcelaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PatchEventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PessoaEquipeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PreparacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarExecucaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CaixaMesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SetorProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OrdemProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LiberacaoExpedicaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteListaItemResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EtapaEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CompraPlanoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumosItemPlanoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemPlanoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LinhaPlanoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoProducaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanosProducaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.AssociarEquipeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EquipeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancarEquipeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OcorrenciaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PessoaEquipeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SalvarExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.repositories.EquipeRepository;
import com.jbkloh.ficha_buffet.repositories.EtapaEventoRepository;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;
import com.jbkloh.ficha_buffet.repositories.OcorrenciaEventoRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Fluxos de ponta a ponta do Anne Gestão sobre o banco de teste: o que cada tela grava e o que ela lê de volta. */
@SpringBootTest
@Transactional
class GestaoFluxosTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    @Autowired private InsumoService insumoService;
    @Autowired private FichaService fichaService;
    @Autowired private EventoService eventoService;
    @Autowired private ItemCardapioService itemCardapioService;
    @Autowired private ClienteService clienteService;
    @Autowired private ParcelaService parcelaService;
    @Autowired private CategoriaFinanceiraService categoriaService;
    @Autowired private CaixaService caixaService;
    @Autowired private MovimentoEstoqueService movimentoService;
    @Autowired private EstoqueService estoqueService;
    @Autowired private NotaFiscalService notaFiscalService;
    @Autowired private LotePreparoService loteService;
    @Autowired private CozinhaService cozinhaService;
    @Autowired private OrdemProducaoService ordemService;
    @Autowired private PainelService painelService;
    @Autowired private BackupService backupService;
    @Autowired private EtapaEventoRepository etapaRepository;
    @Autowired private EquipeEventoService equipeService;
    @Autowired private EquipeRepository equipeRepository;
    @Autowired private EquipeService equipeCadastro;
    @Autowired private LancamentoRepository lancamentoRepository;
    @Autowired private OcorrenciaEventoRepository ocorrenciaRepository;
    @Autowired private PlanoProducaoService planoService;
    @Autowired private PratoVendaService pratoVendaService;
    @Autowired private ObjectMapper json;

    private static final LocalDate DATA_EVENTO = LocalDate.now().plusDays(20);

    private InsumoResponseDTO insumo(String nome, String custo) {
        return insumoService.criarItem(new InsumoRequestDTO(nome, "ingrediente", "Mercearia", "kg", "Refrigerado", "caixa", 10.0, 2.0, new BigDecimal(custo), ""));
    }

    private PreparacaoResponseDTO ficha(String nome, Long insumoId) {
        return fichaService.criarFicha(new PreparacaoRequestDTO(nome, "volante", "", null, null, "", null, null, "", "", null, 10.0, "un", 1.0,
            List.of(new PreparacaoRequestDTO.Item(insumoId, null, 2.0, null)), null, null, null, null, null));
    }

    private EventoResponseDTO evento(String nome) {
        return eventoService.criarEvento(new EventoRequestDTO(nome, null, new EventoRequestDTO.NovoCliente("Cliente " + nome, "11 99999-0000"), "casamento", "lead",
            DATA_EVENTO.toString(), "19:30", "Salão", "São Paulo", 50, 0.0, ""));
    }

    private CategoriaResponseDTO categoria(String nome, String grupo) {
        return categoriaService.criarCategoria(new CategoriaFinanceiraRequestDTO(nome, grupo, 1));
    }

    private static HttpStatus status(Throwable t) {
        if (!(t instanceof AppException e)) {
            throw new AssertionError("Esperava AppException, veio " + t, t);
        }
        return e.getHttpStatus();
    }

    @Test
    void eventoComContrato_geraParcelasEReceitasPrevistasEAcompanhaOPagamento() {
        CategoriaResponseDTO receita = categoria("Eventos sociais", "receita");
        InsumoResponseDTO farinha = insumo("Farinha", "10.00");
        PreparacaoResponseDTO coxinha = ficha("Coxinha", farinha.id());
        EventoResponseDTO evento = evento("Casamento Ana");
        assertThat(evento.codigo()).matches("\\d{2}-\\d{3}");
        assertThat(evento.horaInicio()).isEqualTo("19:30");

        ItemCardapioResponseDTO item = itemCardapioService.criarItem(new ItemCardapioRequestDTO(evento.id(), coxinha.id(), 2.0, null, ""));
        assertThatThrownBy(() -> itemCardapioService.criarItem(new ItemCardapioRequestDTO(evento.id(), coxinha.id(), 1.0, null, "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));

        eventoService.registrarContrato(evento.id(), new ContratoRequestDTO("C-1", LocalDate.now().toString(), 50, new BigDecimal("20.00"), new BigDecimal("1000.00"),
            BigDecimal.ZERO, receita.id(), 3, LocalDate.now().plusDays(5).toString(), 1));

        EventoDetalheResponseDTO detalhe = eventoService.detalharEvento(evento.id());
        assertThat(detalhe.evento().status()).isEqualTo("contratado");
        assertThat(detalhe.evento().contrato().valorTotal()).isEqualTo(1000);
        assertThat(detalhe.parcelas()).extracting(p -> p.valor()).containsExactly(333.33, 333.33, 333.34);
        assertThat(detalhe.parcelas()).allSatisfy(p -> assertThat(p.lancamentoId()).isNotNull());
        assertThat(detalhe.resultado().rec()).isCloseTo(1000, within(0.001));
        // 50 convidados × 2 un = 100 un = 10 receitas × 2 kg × R$ 10.
        assertThat(detalhe.cardapio().n()).isEqualTo(50);
        assertThat(detalhe.cardapio().custo()).isCloseTo(200, within(0.001));
        assertThat(detalhe.cardapio().linhas().get(0).aProduzir()).isEqualTo("a produzir 100 un");
        assertThat(detalhe.travado()).isEmpty();

        // A reserva do evento contratado aparece no estoque.
        assertThat(estoqueService.detalharItem(farinha.id()).saldo().reservado()).isEqualTo(20);

        Long parcelaId = detalhe.parcelas().get(0).id();
        parcelaService.atualizarParcela(parcelaId, new ParcelaRequestDTO(LocalDate.now().toString(), new BigDecimal("333.33"), LocalDate.now().toString(), "pix", "banco"));
        EventoDetalheResponseDTO depois = eventoService.detalharEvento(evento.id());
        assertThat(depois.recebido()).isEqualTo(333.33);
        assertThat(depois.contratoTemParcelaPaga()).isTrue();
        assertThat(depois.travado()).isEqualTo("1 lançamento pago ou recebido");
        assertThatThrownBy(() -> eventoService.apagarEvento(evento.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> eventoService.apagarContrato(evento.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));

        CaixaMesResponseDTO caixa = caixaService.retornarMes(DATA_EVENTO.toString().substring(0, 7), "todos", null);
        assertThat(caixa.kpis().receitas()).isCloseTo(1000, within(0.001));
        assertThat(caixa.lancamentos()).hasSize(3);
        assertThat(caixa.lancamentos().get(0).grupo()).isEqualTo("receita");
        assertThat(caixaService.dre(String.valueOf(DATA_EVENTO.getYear()), "ano", true, null, "competencia", true).linhas().get(0).valor()).isCloseTo(1000, within(0.001));
        assertThat(painelService.resumo().proximos()).extracting(p -> p.evento().id()).contains(evento.id());

        eventoService.ajustarEvento(evento.id(), new PatchEventoRequestDTO(null, null, true, null));
        assertThat(eventoService.detalharEvento(evento.id()).evento().status()).isEqualTo("cardapio_fechado");
        assertThat(item.status()).isEqualTo("sim");
    }

    @Test
    void cardapio_adicionaVariosPratosDeUmaVezSemRepetirOsQueJaEstao() {
        InsumoResponseDTO farinha = insumo("Farinha", "10.00");
        PreparacaoResponseDTO coxinha = ficha("Coxinha", farinha.id());
        PreparacaoResponseDTO kibe = ficha("Kibe", farinha.id());
        PreparacaoResponseDTO empada = ficha("Empada", farinha.id());
        EventoResponseDTO evento = evento("Formatura");
        itemCardapioService.criarItem(new ItemCardapioRequestDTO(evento.id(), coxinha.id(), 2.0, null, ""));

        // Coxinha já está no cardápio e o kibe veio repetido: só entram kibe e empada.
        int adicionados = itemCardapioService.adicionarPratos(evento.id(), List.of(coxinha.id(), kibe.id(), kibe.id(), empada.id()));
        assertThat(adicionados).isEqualTo(2);
        EventoDetalheResponseDTO detalhe = eventoService.detalharEvento(evento.id());
        assertThat(detalhe.cardapio().linhas()).hasSize(3);
        // A coxinha mantém o fator que já tinha.
        assertThat(detalhe.cardapio().linhas()).filteredOn(l -> l.item().preparacaoId().equals(coxinha.id()))
            .singleElement().satisfies(l -> assertThat(l.item().porcao()).isEqualTo(2.0));

        assertThat(itemCardapioService.adicionarPratos(evento.id(), List.of(kibe.id()))).isZero();
        // Ficha inexistente: nada é gravado, nem as válidas do mesmo pedido.
        PreparacaoResponseDTO pastel = ficha("Pastel", farinha.id());
        assertThatThrownBy(() -> itemCardapioService.adicionarPratos(evento.id(), List.of(pastel.id(), 999_999L)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThat(eventoService.detalharEvento(evento.id()).cardapio().linhas()).hasSize(3);
        assertThatThrownBy(() -> itemCardapioService.adicionarPratos(999_999L, List.of(kibe.id())))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void cliente_detalhaComOResumoDosEventos() {
        EventoResponseDTO evento = evento("Bodas");
        ClienteListaItemResponseDTO cliente = clienteService.detalharCliente(evento.clienteId());
        assertThat(cliente.cliente().nome()).isEqualTo("Cliente Bodas");
        assertThat(cliente.totalEventos()).isEqualTo(1);
        assertThat(cliente.proximoEvento()).isEqualTo(DATA_EVENTO.toString());
        assertThat(clienteService.listarClientes("Bodas")).containsExactly(cliente);
        assertThatThrownBy(() -> clienteService.detalharCliente(999_999L)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void eventoSemHistorico_eExcluidoComParcelasECardapio() {
        CategoriaResponseDTO receita = categoria("Eventos sociais", "receita");
        PreparacaoResponseDTO ficha = ficha("Empada", insumo("Manteiga", "30.00").id());
        EventoResponseDTO evento = evento("Aniversário");
        itemCardapioService.criarItem(new ItemCardapioRequestDTO(evento.id(), ficha.id(), 1.0, null, ""));
        eventoService.registrarContrato(evento.id(), new ContratoRequestDTO("", LocalDate.now().toString(), 50, BigDecimal.ZERO, new BigDecimal("500.00"), BigDecimal.ZERO,
            receita.id(), 2, LocalDate.now().toString(), 1));
        assertThatThrownBy(() -> fichaService.apagarFicha(ficha.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));

        eventoService.apagarEvento(evento.id());

        assertThatThrownBy(() -> eventoService.detalharEvento(evento.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(caixaService.retornarMes(DATA_EVENTO.toString().substring(0, 7), "todos", null).lancamentos()).isEmpty();
        fichaService.apagarFicha(ficha.id());
    }

    @Test
    void notaFiscal_daEntradaNoEstoqueAtualizaOCustoELancaContasAPagar_eOEstornoDesfaz() {
        CategoriaResponseDTO ab = categoria("Alimentos e bebidas (A&B)", "custo_direto");
        InsumoResponseDTO leite = insumo("Leite", "0");
        String hoje = LocalDate.now().toString();
        NotaFiscalRequestDTO nota = new NotaFiscalRequestDTO("Laticínios Serra", "123", hoje, null, true, ab.id(), "boleto",
            List.of(new NotaFiscalRequestDTO.Parcela(LocalDate.now().plusDays(30).toString(), 60.0, false)),
            List.of(new NotaFiscalRequestDTO.Item("LEITE CX 10L", "L10", "CX", leite.id(), 2.0, "compra", null, 60.0, true)),
            null, "", "", "", "", false, false, "buffet", null);

        // Sem conferir a linha, a entrada é recusada como regra de negócio.
        NotaFiscalRequestDTO semConferir = new NotaFiscalRequestDTO(nota.fornecedor(), nota.numero(), hoje, null, true, ab.id(), "boleto", nota.parcelas(),
            List.of(new NotaFiscalRequestDTO.Item("LEITE CX 10L", "L10", "CX", leite.id(), 2.0, "compra", null, 60.0, false)), null, "", "", "", "", false, false,
            "buffet", null);
        assertThatThrownBy(() -> notaFiscalService.lancarNota(null, semConferir)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        NotaFiscalResponseDTO rascunho = notaFiscalService.salvarRascunho(null, nota);
        assertThat(rascunho.status()).isEqualTo("rascunho");
        // 2 caixas × fator 10 = 20 kg.
        assertThat(rascunho.itens().get(0).qtd()).isEqualTo(20);
        assertThat(rascunho.total()).isEqualTo(60);

        NotaFiscalResponseDTO lancada = notaFiscalService.lancarNota(rascunho.id(), nota);
        assertThat(lancada.status()).isEqualTo("lancada");
        assertThat(lancada.movIds()).hasSize(1);
        assertThat(lancada.lancIds()).hasSize(1);
        assertThat(estoqueService.detalharItem(leite.id()).saldo().fisico()).isEqualTo(20);
        assertThat(estoqueService.detalharItem(leite.id()).saldo().insumo().custo()).isEqualTo(3);
        assertThat(notaFiscalService.detalharNota(lancada.id()).quitada()).isFalse();
        assertThat(caixaService.contas("pagar", "abertas", null).emAberto().valor()).isEqualTo(60);
        // O histórico do fornecedor passa a sugerir o item para a mesma descrição.
        assertThat(notaFiscalService.importarXml("<NFe><infNFe Id=\"NFe" + "1".repeat(44) + "\"><ide><nNF>9</nNF></ide><emit><xNome>Laticínios Serra</xNome></emit>"
            + "<det><prod><cProd>L10</cProd><xProd>LEITE CX 10L</xProd><qCom>1</qCom><vProd>30.00</vProd></prod></det></infNFe></NFe>").itens().get(0).insumoId())
            .isEqualTo(leite.id());

        assertThatThrownBy(() -> notaFiscalService.apagarNota(lancada.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        notaFiscalService.estornarNota(lancada.id());
        assertThat(estoqueService.detalharItem(leite.id()).saldo().fisico()).isZero();
        assertThat(notaFiscalService.detalharNota(lancada.id()).nota().status()).isEqualTo("rascunho");
        notaFiscalService.apagarNota(lancada.id());
    }

    @Test
    void estoque_ajusteGravaADiferencaInventarioSoOQueMudouELoteBaixaInsumos() {
        InsumoResponseDTO acucar = insumo("Açúcar", "5.00");
        String hoje = LocalDate.now().toString();
        movimentoService.criarMovimento(new MovimentoEstoqueRequestDTO("entrada", acucar.id(), hoje, 10.0, new BigDecimal("6.50"), null, "", null, null, null));
        assertThat(estoqueService.detalharItem(acucar.id()).saldo().insumo().custo()).isEqualTo(6.5);
        assertThatThrownBy(() -> movimentoService.criarMovimento(new MovimentoEstoqueRequestDTO("perda", acucar.id(), hoje, 1.0, null, null, "", null, null, null)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThat(movimentoService.criarMovimento(new MovimentoEstoqueRequestDTO("ajuste", acucar.id(), hoje, 8.0, null, null, "Contagem", null, null, null)).qtd()).isEqualTo(-2);
        assertThat(movimentoService.lancarInventario(new InventarioRequestDTO(List.of(new InventarioRequestDTO.Contagem(acucar.id(), 9.0))))).isEqualTo(1);
        assertThat(estoqueService.saldo().itens()).filteredOn(x -> x.insumo().id().equals(acucar.id())).extracting(x -> x.fisico()).containsExactly(9.0);
        assertThat(movimentoService.listarMovimentos("contagens").total()).isEqualTo(2);

        PreparacaoResponseDTO calda = ficha("Calda", acucar.id());
        LoteResponseDTO lote = loteService.criarLote(new LotePreparoRequestDTO(calda.id(), 20.0, hoje, LocalDate.now().plusDays(3).toString(), "restaurante", null, "Ana", ""));
        assertThat(lote.codigo()).matches("L\\d{6}-01");
        // 20 un / rende 10 = 2 receitas × 2 kg saem do estoque como baixa calculada.
        assertThat(estoqueService.detalharItem(acucar.id()).saldo().fisico()).isEqualTo(5);
        assertThat(loteService.movimentarLote(lote.id(), new MovimentarLoteRequestDTO("uso", 5.0, hoje, "restaurante", null, "")).saldo()).isEqualTo(15);
        assertThatThrownBy(() -> loteService.movimentarLote(lote.id(), new MovimentarLoteRequestDTO("uso", 50.0, hoje, "restaurante", null, "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> loteService.apagarLote(lote.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThat(loteService.listarLotes("ativos").prontoAgora()).extracting(p -> p.total()).containsExactly(15.0);
        assertThatThrownBy(() -> insumoService.apagarItem(acucar.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void cozinha_registraProducaoEApagaOEnvioQuandoALinhaDeixaDeCumprirAsCondicoes() {
        CategoriaResponseDTO receita = categoria("Eventos sociais", "receita");
        InsumoResponseDTO frango = insumo("Frango", "20.00");
        PreparacaoResponseDTO coxinha = ficha("Coxinha", frango.id());
        EventoResponseDTO evento = evento("Formatura");
        ItemCardapioResponseDTO item = itemCardapioService.criarItem(new ItemCardapioRequestDTO(evento.id(), coxinha.id(), 1.0, null, ""));
        eventoService.registrarContrato(evento.id(), new ContratoRequestDTO("", LocalDate.now().toString(), 50, BigDecimal.ZERO, new BigDecimal("900.00"), BigDecimal.ZERO,
            receita.id(), 1, LocalDate.now().toString(), 0));

        CozinhaResponseDTO tela = cozinhaService.retornarTudo();
        assertThat(tela.projecoes()).extracting(p -> p.eventoId()).contains(evento.id());
        assertThat(tela.tarefas()).anySatisfy(t -> assertThat(t.tarefa().k()).isEqualTo("p_" + item.id()));

        String chave = "i_" + item.id();
        SalvarExecucaoResponseDTO produziu = cozinhaService.salvarExecucao(new SalvarExecucaoRequestDTO(chave, "item", evento.id(), item.id(), null, null,
            Map.of("produzido", 50, "executadoPor", " Ana ", "separadoPor", "Bia", "tempSaida", 3)));
        assertThat(produziu.baixaAlterada()).isTrue();
        assertThat(produziu.execucao().chave()).isEqualTo(chave);
        assertThat(produziu.execucao().executadoPor()).isEqualTo("Ana");
        assertThat(produziu.execucao().produzidoEm()).isEqualTo(LocalDate.now().toString());
        // A produção dá baixa de 5 receitas × 2 kg.
        assertThat(estoqueService.detalharItem(frango.id()).saldo().fisico()).isEqualTo(-10);

        SalvarExecucaoResponseDTO enviou = cozinhaService.salvarExecucao(new SalvarExecucaoRequestDTO(chave, "item", evento.id(), item.id(), null, null, Map.of("enviadoQ", 50)));
        assertThat(enviou.execucao().enviado()).isTrue();
        // Temperatura fora da faixa do refrigerado: o envio é apagado.
        SalvarExecucaoResponseDTO fora = cozinhaService.salvarExecucao(new SalvarExecucaoRequestDTO(chave, "item", evento.id(), item.id(), null, null, Map.of("tempSaida", 9)));
        assertThat(fora.envioApagado()).isTrue();
        assertThat(fora.execucao().enviadoQ()).isNull();
        assertThat(fora.execucao().enviado()).isFalse();

        assertThatThrownBy(() -> cozinhaService.salvarExecucao(new SalvarExecucaoRequestDTO(chave, "item", evento.id(), item.id(), null, null, Map.of("produzido", "muito"))))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.BAD_REQUEST));
        SalvarExecucaoResponseDTO tarefa = cozinhaService.salvarExecucao(new SalvarExecucaoRequestDTO("t_" + evento.id() + "__f_0", "tarefa", evento.id(), null, null, "f_0",
            Map.of("feito", true)));
        assertThat(tarefa.execucao().key()).isEqualTo("f_0");
        assertThat(cozinhaService.retornarTudo().tarefas()).filteredOn(t -> t.tarefa().k().equals("f_0") && t.eventoId().equals(evento.id())).allMatch(t -> t.feito());
        assertThat(eventoService.detalharEvento(evento.id()).travado()).contains("produção registrada no checklist");
    }

    @Test
    void etapas_marcadasAMaoFicamGravadasEAsDetectadasNaoDependemDaMarcacao() {
        EventoResponseDTO evento = evento("Coquetel");
        EventoDetalheResponseDTO antes = eventoService.detalharEvento(evento.id());
        assertThat(antes.etapas()).extracting(EtapaEventoDTO::etapa).containsExactly("cadastro", "dados", "contrato", "degustacao", "cardapio", "fichas",
            "compras", "producao", "equipe", "logistica", "carregamento", "execucao", "conferencia", "fechamento");
        // Cliente, data, local e convidados preenchidos: as duas primeiras o sistema detecta.
        assertThat(etapa(antes, "cadastro")).satisfies(x -> assertThat(x.auto()).isTrue());
        assertThat(etapa(antes, "dados")).satisfies(x -> assertThat(x.feito() && x.auto()).isTrue());
        // Sem contrato a 20 dias do evento: etapa crítica (30 dias) atrasada.
        assertThat(etapa(antes, "contrato")).satisfies(x -> {
            assertThat(x.feito()).isFalse();
            assertThat(x.info()).isEqualTo("sem contrato");
            assertThat(x.atrasada()).isTrue();
            assertThat(x.diasParaEvento()).isEqualTo(20);
        });

        eventoService.ajustarEtapa(evento.id(), new AjustarEtapaRequestDTO("equipe", true, null));
        eventoService.ajustarEtapa(evento.id(), new AjustarEtapaRequestDTO("degustacao", null, true));
        EventoDetalheResponseDTO marcado = eventoService.detalharEvento(evento.id());
        assertThat(etapa(marcado, "equipe")).satisfies(x -> {
            assertThat(x.feito() && x.manual()).isTrue();
            assertThat(x.auto() || x.atrasada()).isFalse();
            assertThat(x.em()).isNotBlank();
        });
        assertThat(etapa(marcado, "degustacao")).satisfies(x -> assertThat(x.dispensada() && !x.feito()).isTrue());
        assertThat(etapaRepository.findByEventoId(evento.id())).hasSize(2);

        // Desmarcar apaga a linha; marcar uma etapa detectada não muda nada além de gravar a marcação.
        eventoService.ajustarEtapa(evento.id(), new AjustarEtapaRequestDTO("equipe", false, null));
        eventoService.ajustarEtapa(evento.id(), new AjustarEtapaRequestDTO("dados", false, null));
        EventoDetalheResponseDTO desmarcado = eventoService.detalharEvento(evento.id());
        assertThat(etapa(desmarcado, "equipe").feito()).isFalse();
        assertThat(etapa(desmarcado, "dados").feito()).isTrue();
        assertThat(etapaRepository.findByEventoId(evento.id())).extracting(x -> x.getEtapa()).containsExactly("degustacao");

        assertThatThrownBy(() -> eventoService.ajustarEtapa(evento.id(), new AjustarEtapaRequestDTO("fechamento", true, null)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> eventoService.ajustarEtapa(evento.id(), new AjustarEtapaRequestDTO("equipe", null, true)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> eventoService.ajustarEtapa(999_999L, new AjustarEtapaRequestDTO("equipe", true, null)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));

        // Excluir o evento leva as etapas marcadas junto.
        eventoService.apagarEvento(evento.id());
        assertThat(etapaRepository.findByEventoId(evento.id())).isEmpty();
    }

    private static PessoaEquipeRequestDTO pessoa(Long equipeId, String nome, String funcao, String chegada, String valor, String contato, String pix) {
        return new PessoaEquipeRequestDTO(equipeId, nome, funcao, chegada, "", new BigDecimal(valor), contato, pix);
    }

    @Test
    void equipe_cadastroReaproveitadoEntreEventosEDiariasViramContasAPagar() {
        EventoResponseDTO sabado = evento("Casamento Bia");
        EventoResponseDTO domingo = evento("Formatura Caio");
        long antes = equipeRepository.count();

        PessoaEquipeResponseDTO joao = equipeService.criar(sabado.id(), pessoa(null, " João Silva ", "Garçom", "18:00", "150.00", "11 98888-0000", "joao@pix"));
        assertThat(joao.id()).isNotNull();
        assertThat(joao.nome()).isEqualTo("João Silva");
        assertThat(joao.chegada()).isEqualTo("18:00");
        assertThat(joao.saida()).isEmpty();
        assertThat(joao.lancamentoId()).isNull();
        assertThat(joao.pago()).isFalse();
        equipeService.criar(sabado.id(), pessoa(null, "Maria", "Maître", "", "200.00", "", ""));
        assertThat(equipeRepository.count()).isEqualTo(antes + 2);

        // Mesmo nome (sem acento, outra caixa) em outro evento: reaproveita a pessoa e mantém o telefone.
        PessoaEquipeResponseDTO joaoDomingo = equipeService.criar(domingo.id(), pessoa(null, "joao  silva", "Bartender", "17:00", "180.00", "", ""));
        assertThat(joaoDomingo.equipeId()).isEqualTo(joao.equipeId());
        assertThat(joaoDomingo.nome()).isEqualTo("João Silva");
        assertThat(joaoDomingo.contato()).isEqualTo("11 98888-0000");
        assertThat(joaoDomingo.funcao()).isEqualTo("Bartender");
        // Pela pessoa escolhida (equipeId): o nome do corpo não muda o cadastro.
        PessoaEquipeResponseDTO maria = equipeService.escala(sabado.id()).stream().filter(x -> x.nome().equals("Maria")).findFirst().orElseThrow();
        assertThat(equipeService.criar(domingo.id(), pessoa(maria.equipeId(), "Outro nome", "Copeira", "", "100.00", "", "")).nome()).isEqualTo("Maria");
        assertThat(equipeRepository.count()).isEqualTo(antes + 2);

        assertThatThrownBy(() -> equipeService.criar(sabado.id(), pessoa(null, "JOÃO SILVA", "Garçom", "", "0", "", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> equipeService.criar(sabado.id(), pessoa(999_999L, "X", "Garçom", "", "0", "", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> equipeService.atualizar(sabado.id(), joao.id(), pessoa(null, "Maria", "Garçom", "", "150.00", "", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> equipeService.apagar(domingo.id(), joao.id()))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));

        // A ficha do evento traz a escala (sem horário primeiro) e a etapa "equipe" detectada.
        EventoDetalheResponseDTO det = eventoService.detalharEvento(sabado.id());
        assertThat(det.equipe()).extracting(PessoaEquipeResponseDTO::nome).containsExactly("Maria", "João Silva");
        assertThat(etapa(det, "equipe")).satisfies(x -> {
            assertThat(x.feito() && x.auto()).isTrue();
            assertThat(x.info()).isEqualTo("2 pessoas");
        });

        // Sem categoria de mão de obra não lança; com ela, cada diária vira conta a pagar no dia do evento.
        assertThatThrownBy(() -> equipeService.lancar(sabado.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        categoria("Mão de obra temporária", "custo_direto");
        LancarEquipeResponseDTO lancou = equipeService.lancar(sabado.id());
        assertThat(lancou.lancados()).isEqualTo(2);
        assertThat(lancou.vencimento()).isEqualTo(DATA_EVENTO.toString());
        assertThat(equipeService.lancar(sabado.id()).lancados()).isZero();
        PessoaEquipeResponseDTO lancado = equipeService.escala(sabado.id()).stream().filter(x -> x.id().equals(joao.id())).findFirst().orElseThrow();
        assertThat(lancado.lancamentoStatus()).isEqualTo("previsto");
        var conta = lancamentoRepository.findById(lancado.lancamentoId()).orElseThrow();
        assertThat(conta.getOrigem()).isEqualTo("equipe");
        assertThat(conta.getTipo()).isEqualTo("despesa");
        assertThat(conta.getVencimento()).isEqualTo(DATA_EVENTO);
        assertThat(conta.getFornecedor()).isEqualTo("João Silva");
        assertThat(conta.getFormaPag()).isEqualTo("pix");

        // Editar a diária ainda não paga acompanha a conta a pagar; nome e telefone mudam no cadastro (vale no outro evento).
        equipeService.atualizar(sabado.id(), joao.id(), pessoa(null, "João P. Silva", "Garçom", "18:00", "160.00", "11 97777-0000", "joao@pix"));
        conta = lancamentoRepository.findById(lancado.lancamentoId()).orElseThrow();
        assertThat(conta.getValor()).isEqualByComparingTo("160.00");
        assertThat(conta.getDescricao()).isEqualTo("Equipe · João P. Silva (Garçom) · Casamento Bia");
        assertThat(eventoService.detalharEvento(domingo.id()).equipe()).anySatisfy(x -> {
            assertThat(x.nome()).isEqualTo("João P. Silva");
            assertThat(x.contato()).isEqualTo("11 97777-0000");
        });

        // Paga, a diária trava o valor e a pessoa não sai da escala.
        caixaService.pagarLancamento(lancado.lancamentoId());
        assertThat(eventoService.detalharEvento(sabado.id()).equipe()).anySatisfy(x -> assertThat(x.pago()).isTrue());
        assertThatThrownBy(() -> equipeService.atualizar(sabado.id(), joao.id(), pessoa(null, "João P. Silva", "Garçom", "", "170.00", "", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> equipeService.apagar(sabado.id(), joao.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));

        // Tirar da escala leva a conta a pagar não paga junto; a pessoa continua no cadastro.
        Long contaMaria = equipeService.escala(sabado.id()).stream().filter(x -> x.nome().equals("Maria")).findFirst().orElseThrow().lancamentoId();
        equipeService.apagar(sabado.id(), maria.id());
        assertThat(lancamentoRepository.findById(contaMaria)).isEmpty();
        assertThat(equipeRepository.findById(maria.equipeId())).isPresent();

        // Excluir um evento sem histórico tira a escala dele, mas não as pessoas.
        eventoService.apagarEvento(domingo.id());
        assertThat(equipeRepository.count()).isEqualTo(antes + 2);

        JsonNode json1 = json.valueToTree(eventoService.detalharEvento(sabado.id()));
        assertThat(json1.get("equipe").get(0).get("equipeId").asLong()).isEqualTo(joao.equipeId());
        assertThat(json1.get("equipe").get(0).get("lancamentoStatus").asString()).isEqualTo("realizado");
    }

    @Test
    void equipe_checklistEscalaEmLoteComOsPadroesDoCadastroESincronizaQuemSai() {
        EventoResponseDTO festa = evento("Festa Lote");
        EventoResponseDTO outra = evento("Outra Festa");
        // Cadastra pelo formulário (pessoa nova): a função e a diária viram o padrão do cadastro.
        PessoaEquipeResponseDTO ana = equipeService.criar(outra.id(), pessoa(null, "Ana Lote", "Copeira", "", "120.00", "11 9000-0001", ""));
        PessoaEquipeResponseDTO beto = equipeService.criar(outra.id(), pessoa(null, "Beto Lote", "Bartender", "", "200.00", "", "beto@pix"));
        PessoaEquipeResponseDTO caio = equipeService.criar(outra.id(), pessoa(null, "Caio Lote", "Garçom", "", "150.00", "", ""));

        List<EquipeResponseDTO> cadastro = equipeCadastro.listar();
        assertThat(cadastro).extracting(EquipeResponseDTO::nome).containsSubsequence("Ana Lote", "Beto Lote", "Caio Lote");
        EquipeResponseDTO betoCad = cadastro.stream().filter(x -> x.id().equals(beto.equipeId())).findFirst().orElseThrow();
        assertThat(betoCad.funcaoPadrao()).isEqualTo("Bartender");
        assertThat(betoCad.diariaPadrao()).isEqualTo(200);
        assertThat(betoCad.pix()).isEqualTo("beto@pix");

        // Um pedido só escala os três, com a função e a diária padrão; ids repetidos contam uma vez.
        AssociarEquipeResponseDTO r = equipeService.associar(festa.id(), List.of(ana.equipeId(), beto.equipeId(), caio.equipeId(), ana.equipeId()));
        assertThat(r.adicionados()).isEqualTo(3);
        assertThat(r.removidos()).isZero();
        assertThat(equipeService.escala(festa.id())).anySatisfy(x -> {
            assertThat(x.equipeId()).isEqualTo(beto.equipeId());
            assertThat(x.funcao()).isEqualTo("Bartender");
            assertThat(x.valor()).isEqualTo(200);
            assertThat(x.chegada()).isEmpty();
        });
        // Mesma lista de novo: nada muda.
        assertThat(equipeService.associar(festa.id(), List.of(ana.equipeId(), beto.equipeId(), caio.equipeId())))
            .isEqualTo(new AssociarEquipeResponseDTO(0, 0));

        // Diárias lançadas; a de Ana é paga. Desmarcar Ana barra o lote inteiro.
        categoria("Equipe de salão", "custo_direto");
        equipeService.lancar(festa.id());
        Long contaAna = equipeService.escala(festa.id()).stream().filter(x -> x.equipeId().equals(ana.equipeId())).findFirst().orElseThrow().lancamentoId();
        Long contaCaio = equipeService.escala(festa.id()).stream().filter(x -> x.equipeId().equals(caio.equipeId())).findFirst().orElseThrow().lancamentoId();
        caixaService.pagarLancamento(contaAna);
        assertThatThrownBy(() -> equipeService.associar(festa.id(), List.of(beto.equipeId())))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThat(equipeService.escala(festa.id())).hasSize(3);

        // Desmarcar Caio tira ele e a conta a pagar não paga; a escala da outra festa não muda.
        AssociarEquipeResponseDTO sem = equipeService.associar(festa.id(), List.of(ana.equipeId(), beto.equipeId()));
        assertThat(sem).isEqualTo(new AssociarEquipeResponseDTO(0, 1));
        assertThat(equipeService.escala(festa.id())).extracting(PessoaEquipeResponseDTO::equipeId).containsExactlyInAnyOrder(ana.equipeId(), beto.equipeId());
        assertThat(lancamentoRepository.findById(contaCaio)).isEmpty();
        assertThat(equipeService.escala(outra.id())).hasSize(3);

        assertThatThrownBy(() -> equipeService.associar(festa.id(), List.of(999_999L)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> equipeService.associar(999_999L, List.of()))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void equipe_editarCadastroMudaSoOInformadoEOsPadroesValemParaAsProximasEscalas() {
        EventoResponseDTO jantar = evento("Jantar Edição");
        EventoResponseDTO almoco = evento("Almoço Edição");
        PessoaEquipeResponseDTO dani = equipeService.criar(jantar.id(), pessoa(null, "Dani Edição", "Garçom", "18:00", "150.00", "11 9000-0009", "dani@pix"));
        equipeService.criar(jantar.id(), pessoa(null, "Edu Edição", "Copeira", "", "100.00", "", ""));
        categoria("Mão de obra", "custo_direto");
        equipeService.lancar(jantar.id());
        Long conta = equipeService.escala(jantar.id()).stream().filter(x -> x.equipeId().equals(dani.equipeId())).findFirst().orElseThrow().lancamentoId();

        // Só o que vier muda: função e diária padrão novas, telefone e Pix mantidos.
        EquipeResponseDTO r = equipeCadastro.ajustar(dani.equipeId(), new AjustarEquipeRequestDTO(null, "Maître", new BigDecimal("220.00"), null, null));
        assertThat(r.funcaoPadrao()).isEqualTo("Maître");
        assertThat(r.diariaPadrao()).isEqualTo(220);
        assertThat(r.contato()).isEqualTo("11 9000-0009");
        assertThat(r.pix()).isEqualTo("dani@pix");
        // A escala existente mantém o combinado; a próxima usa o padrão novo.
        assertThat(equipeService.escala(jantar.id())).anySatisfy(x -> {
            assertThat(x.equipeId()).isEqualTo(dani.equipeId());
            assertThat(x.funcao()).isEqualTo("Garçom");
            assertThat(x.valor()).isEqualTo(150);
        });
        equipeService.associar(almoco.id(), List.of(dani.equipeId()));
        assertThat(equipeService.escala(almoco.id()).get(0)).satisfies(x -> {
            assertThat(x.funcao()).isEqualTo("Maître");
            assertThat(x.valor()).isEqualTo(220);
        });

        // Nome novo: vale em todas as escalas e a conta a pagar não paga acompanha.
        equipeCadastro.ajustar(dani.equipeId(), new AjustarEquipeRequestDTO(" Daniela Edição ", null, null, "", null));
        assertThat(equipeService.escala(almoco.id()).get(0).nome()).isEqualTo("Daniela Edição");
        var l = lancamentoRepository.findById(conta).orElseThrow();
        assertThat(l.getFornecedor()).isEqualTo("Daniela Edição");
        assertThat(l.getDescricao()).isEqualTo("Equipe · Daniela Edição (Garçom) · Jantar Edição");
        assertThat(equipeCadastro.listar()).anySatisfy(x -> {
            assertThat(x.id()).isEqualTo(dani.equipeId());
            assertThat(x.contato()).isEmpty();
        });

        // Paga, a conta fica como foi registrada.
        caixaService.pagarLancamento(conta);
        equipeCadastro.ajustar(dani.equipeId(), new AjustarEquipeRequestDTO("Dani E.", null, null, null, null));
        assertThat(lancamentoRepository.findById(conta).orElseThrow().getFornecedor()).isEqualTo("Daniela Edição");

        assertThatThrownBy(() -> equipeCadastro.ajustar(dani.equipeId(), new AjustarEquipeRequestDTO("edu edicao", null, null, null, null)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> equipeCadastro.ajustar(999_999L, new AjustarEquipeRequestDTO("X", null, null, null, null)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static EtapaEventoDTO etapa(EventoDetalheResponseDTO d, String k) {
        return d.etapas().stream().filter(x -> x.etapa().equals(k)).findFirst().orElseThrow();
    }

    @Test
    void respostas_saemNoFormatoQueAsTelasConsomem() {
        InsumoResponseDTO sal = insumo("Sal", "2.00");
        ficha("Salmoura", sal.id());
        EventoResponseDTO evento = evento("Jantar");

        PreparacaoListaItemDTO linha = fichaService.listarFichas("salm").get(0);
        JsonNode ficha = json.valueToTree(linha);
        // Os DTOs "de lista" estendem o registro base: os campos saem no mesmo nível.
        assertThat(ficha.get("nome").asString()).isEqualTo("Salmoura");
        assertThat(ficha.get("itens").get(0).get("insumoId").asLong()).isEqualTo(sal.id());
        assertThat(ficha.get("custo").get("total").asDouble()).isEqualTo(4);
        assertThat(ficha.get("fichaStatus").asString()).isEqualTo("Completa");
        assertThat(ficha.has("preparacao")).isFalse();

        JsonNode detalhe = json.valueToTree(eventoService.detalharEvento(evento.id()));
        assertThat(detalhe.get("cardapio").get("N").asInt()).isEqualTo(50);
        assertThat(detalhe.get("cardapio").get("nDefinir").asInt()).isZero();
        assertThat(detalhe.get("evento").get("dataEvento").asString()).isEqualTo(DATA_EVENTO.toString());
        assertThat(detalhe.get("evento").get("contrato").isNull()).isTrue();
        assertThat(detalhe.get("cliente").get("contato").asString()).isEqualTo("11 99999-0000");

        JsonNode opcoes = json.valueToTree(eventoService.opcoes("evento"));
        assertThat(opcoes.get("clientes").get(0).get(1).asString()).startsWith("Cliente");
        assertThat(opcoes.has("preparacoes")).isFalse();
        assertThat(json.valueToTree(eventoService.opcoes("cardapio")).has("clientes")).isFalse();

        JsonNode painel = json.valueToTree(painelService.resumo());
        assertThat(painel.get("kpis").has("aReceber")).isTrue();
        assertThat(painel.get("proximos").get(0).has("clienteNome")).isTrue();
        assertThat(painel.get("alertas").isArray()).isTrue();

        JsonNode backup = json.valueToTree(backupService.exportar());
        assertThat(backup.get("app").asString()).isEqualTo("Anne Gestão");
        assertThat(backup.get("colecoes").get("eventos").get(String.valueOf(evento.id())).has("id")).isFalse();
        assertThat(backupService.analisar(backupService.exportar()).linhas()).allSatisfy(l -> {
            assertThat(l.novos()).isZero();
            assertThat(l.diferentes()).isZero();
        });
        assertThat(backupService.resumo().total()).isGreaterThanOrEqualTo(4);
        assertThat(backupService.planilha()).startsWith((byte) 'P', (byte) 'K');
    }

    private List<OrdemProducaoDTO> ordensDo(Long eventoId) {
        return cozinhaService.retornarTudo().ordens().stream().filter(o -> o.eventoId().equals(eventoId)).toList();
    }

    private OrdemProducaoDTO ordem(Long eventoId, String k) {
        return ordensDo(eventoId).stream().filter(o -> o.k().equals(k)).findFirst().orElseThrow();
    }

    private LiberacaoExpedicaoDTO liberacaoDo(Long eventoId) {
        return cozinhaService.retornarTudo().liberacoes().stream().filter(l -> l.eventoId().equals(eventoId)).findFirst().orElseThrow();
    }

    private static AcaoOrdemRequestDTO acao(Long eventoId, String k, String quem) {
        return new AcaoOrdemRequestDTO(eventoId, k, quem, null, null, null, null, null, null);
    }

    @Test
    void ordens_mapaPlanoExecucaoConferenciaComBaixaUnicaELiberacao() {
        CategoriaResponseDTO receita = categoria("Eventos corporativos", "receita");
        InsumoResponseDTO frango = insumo("Frango", "20.00");
        PreparacaoResponseDTO coxinha = ficha("Coxinha", frango.id());
        EventoResponseDTO evento = evento("Convenção");
        Long id = evento.id();
        ItemCardapioResponseDTO item = itemCardapioService.criarItem(new ItemCardapioRequestDTO(id, coxinha.id(), 1.0, null, ""));
        eventoService.registrarContrato(id, new ContratoRequestDTO("", LocalDate.now().toString(), 50, BigDecimal.ZERO, new BigDecimal("900.00"), BigDecimal.ZERO,
            receita.id(), 1, LocalDate.now().toString(), 0));
        String km = "m_" + item.id();
        String kf = "f_" + item.id();

        // Sem mapa: nenhuma ordem, setores padrão, e nada a planejar.
        CozinhaResponseDTO antes = cozinhaService.retornarTudo();
        assertThat(antes.ordens()).noneMatch(o -> o.eventoId().equals(id));
        assertThat(antes.setores()).extracting(SetorProducaoDTO::id).containsExactly("quente", "fria", "confeitaria", "montagem", "expedicao");
        assertThatThrownBy(() -> ordemService.planejar(id, new PlanoOrdemRequestDTO(km, 3, "", "fria", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        ordemService.gerarMapa(id);
        assertThatThrownBy(() -> ordemService.gerarMapa(id)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        CozinhaResponseDTO tela = cozinhaService.retornarTudo();
        assertThat(tela.projecoes()).filteredOn(p -> p.eventoId().equals(id)).singleElement().satisfies(p -> {
            assertThat(p.producao()).isNotNull();
            assertThat(p.producao().liberado()).isEmpty();
        });
        assertThat(ordensDo(id)).extracting(OrdemProducaoDTO::k, OrdemProducaoDTO::dn, OrdemProducaoDTO::status)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(km, 5, "pendente"), org.assertj.core.groups.Tuple.tuple(kf, 0, "pendente"),
                org.assertj.core.groups.Tuple.tuple("x_exp", 0, "pendente"));
        assertThat(liberacaoDo(id).situacao()).containsExactly("bad", "Bloqueado");

        // Plano da gestão.
        ordemService.planejar(id, new PlanoOrdemRequestDTO(km, 2, "09:30", "fria", "Carla"));
        assertThat(ordem(id, km)).extracting(OrdemProducaoDTO::dn, OrdemProducaoDTO::dnPadrao, OrdemProducaoDTO::hora, OrdemProducaoDTO::setor, OrdemProducaoDTO::resp)
            .containsExactly(2, false, "09:30", "fria", "Carla");
        assertThatThrownBy(() -> ordemService.planejar(id, new PlanoOrdemRequestDTO(km, 2, "", "inexistente", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> ordemService.planejar(id, new PlanoOrdemRequestDTO("m_999999", 2, "", "fria", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        // Execução: iniciar, concluir (quantidade obrigatória), conferir.
        assertThatThrownBy(() -> ordemService.agir("concluir", acao(id, km, "Ana"))).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        ordemService.agir("iniciar", acao(id, km, "Ana"));
        assertThat(ordem(id, km)).extracting(OrdemProducaoDTO::status, OrdemProducaoDTO::resp).containsExactly("em_producao", "Ana");
        assertThatThrownBy(() -> ordemService.agir("concluir", acao(id, km, "Ana"))).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.BAD_REQUEST));
        ordemService.agir("concluir", new AcaoOrdemRequestDTO(id, km, "Ana", null, 50.0, 1.0, "Massa rendeu bem", null, null));
        assertThat(ordem(id, km).status()).isEqualTo("em_conferencia");
        // Ainda sem baixa: só a ordem conferida baixa o estoque.
        assertThat(estoqueService.detalharItem(frango.id()).saldo().fisico()).isZero();
        ordemService.agir("conferir", acao(id, km, "Chef"));
        OrdemProducaoDTO conferida = ordem(id, km);
        assertThat(conferida.status()).isEqualTo("concluida");
        assertThat(conferida.exec().qtd()).isEqualTo(50);
        assertThat(conferida.exec().lote()).isNotBlank();
        assertThat(conferida.exec().hist()).extracting(h -> h.quem()).containsExactly("Ana", "Ana", "Chef");
        assertThat(conferida.etiqueta()).contains("Lote: " + conferida.exec().lote(), "Conferido por: Chef");
        // O checklist recebe a produção do prato, mas a baixa sai uma vez só, pela ordem: 5 receitas × 2 kg.
        assertThat(cozinhaService.retornarTudo().execucoes()).filteredOn(x -> x.chave().equals("i_" + item.id())).singleElement()
            .satisfies(x -> assertThat(x.produzido()).isEqualTo(50));
        assertThat(estoqueService.detalharItem(frango.id()).saldo().fisico()).isEqualTo(-10);
        assertThat(eventoService.detalharEvento(id).travado()).contains("ordens de produção conferidas");

        // Bloqueio com motivo e volta para onde estava.
        ordemService.agir("bloquear", new AcaoOrdemRequestDTO(id, kf, "Bia", null, null, null, null, "insumo", "Sem bandejas"));
        assertThat(ordem(id, kf)).extracting(o -> o.status(), o -> o.exec().motivo(), o -> o.exec().motivoTxt())
            .containsExactly("bloqueada", "Falta de insumo", "Sem bandejas");
        ordemService.agir("desbloquear", acao(id, kf, ""));
        assertThat(ordem(id, kf).status()).isEqualTo("pendente");

        // Liberação: com pendências, só justificada; depois de liberada, travada até desfazer.
        ordemService.salvarLiberacao(id, new SalvarLiberacaoRequestDTO(Map.of("veiculo", true), "16:00"));
        assertThat(liberacaoDo(id)).extracting(LiberacaoExpedicaoDTO::horaSaida, l -> l.ck().get("veiculo")).containsExactly("16:00", true);
        assertThatThrownBy(() -> ordemService.autorizarLiberacao(id, new AutorizarLiberacaoRequestDTO("Chef", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        ordemService.autorizarLiberacao(id, new AutorizarLiberacaoRequestDTO("Chef", "Checklist conferido no local"));
        assertThat(liberacaoDo(id)).extracting(LiberacaoExpedicaoDTO::situacao, LiberacaoExpedicaoDTO::pendencias)
            .containsExactly(List.of("warn", "Liberado com pendências"), "Checklist conferido no local");
        assertThat(ordem(id, "x_exp").status()).isEqualTo("concluida");
        assertThatThrownBy(() -> ordemService.salvarLiberacao(id, new SalvarLiberacaoRequestDTO(null, "17:00")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        ordemService.desfazerLiberacao(id);
        assertThat(liberacaoDo(id).autorizadoEm()).isNull();

        // Liberar para a equipe.
        ordemService.liberarMapa(id, new LiberarMapaRequestDTO(true));
        assertThat(cozinhaService.retornarTudo().projecoes()).filteredOn(p -> p.eventoId().equals(id)).singleElement()
            .satisfies(p -> assertThat(p.producao().liberado()).isNotEmpty());

        // Setores: renomear, criar e desativar; setor inativo some do plano e das opções da ficha.
        ordemService.salvarSetores(new SalvarSetoresRequestDTO(List.of(new SalvarSetoresRequestDTO.Setor("fria", "Garde manger", true),
            new SalvarSetoresRequestDTO.Setor("", "Padaria", true), new SalvarSetoresRequestDTO.Setor("confeitaria", "Confeitaria", false))));
        List<SetorProducaoDTO> setores = cozinhaService.retornarTudo().setores();
        assertThat(setores).extracting(SetorProducaoDTO::nome).containsExactly("Cozinha quente", "Garde manger", "Confeitaria", "Montagem e finalização",
            "Expedição e logística", "Padaria");
        assertThat(setores).filteredOn(x -> x.id().equals("confeitaria")).extracting(SetorProducaoDTO::ativo).containsExactly(false);
        assertThat(fichaService.opcoes().setores()).extracting(x -> x.get(1)).contains("Garde manger", "Padaria").doesNotContain("Confeitaria");
        assertThatThrownBy(() -> ordemService.planejar(id, new PlanoOrdemRequestDTO(kf, 0, "", "confeitaria", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> ordemService.salvarSetores(new SalvarSetoresRequestDTO(List.of(new SalvarSetoresRequestDTO.Setor("nao-existe", "X", true)))))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void ficha_gravaOsCamposDeProducaoEValidaOSetor() {
        InsumoResponseDTO farinha = insumo("Farinha", "5.00");
        PreparacaoResponseDTO pao = fichaService.criarFicha(new PreparacaoRequestDTO("Pão de mel", "sobremesa", "", null, null, "confeitaria", 3, 45, "Forno",
            "Pote fechado", true, 10.0, "un", 1.0, List.of(new PreparacaoRequestDTO.Item(farinha.id(), null, 1.0, null)), null, null, null, null, null));
        assertThat(pao).extracting(PreparacaoResponseDTO::setor, PreparacaoResponseDTO::dnProducao, PreparacaoResponseDTO::tempoMin,
            PreparacaoResponseDTO::equipamentos, PreparacaoResponseDTO::armazenamento, PreparacaoResponseDTO::compradoPronto)
            .containsExactly("confeitaria", 3, 45, "Forno", "Pote fechado", true);
        assertThatThrownBy(() -> fichaService.criarFicha(new PreparacaoRequestDTO("Bolo", "sobremesa", "", null, null, "inexistente", null, null, "", "", null,
            10.0, "un", 1.0, List.of(), null, null, null, null, null))).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void ocorrencias_registraResolveSaiNaTelaDaCozinhaESomemComOEvento() {
        CategoriaResponseDTO receita = categoria("Eventos de formatura", "receita");
        EventoResponseDTO evento = evento("Formatura");
        Long id = evento.id();
        eventoService.registrarContrato(id, new ContratoRequestDTO("", LocalDate.now().toString(), 50, BigDecimal.ZERO, new BigDecimal("700.00"), BigDecimal.ZERO,
            receita.id(), 1, LocalDate.now().toString(), 0));
        EventoResponseDTO lead = evento("Orçamento em aberto");

        OcorrenciaDTO criada = cozinhaService.criarOcorrencia(new CriarOcorrenciaRequestDTO(id, "Falta de insumo", "  Carla ", " Faltou creme de leite. ", null));
        assertThat(criada).extracting(OcorrenciaDTO::eventoId, OcorrenciaDTO::tipo, OcorrenciaDTO::quem, OcorrenciaDTO::texto, OcorrenciaDTO::acao,
            OcorrenciaDTO::resolvida).containsExactly(id, "Falta de insumo", "Carla", "Faltou creme de leite.", "", false);
        assertThat(criada.quando()).isNotBlank();
        OcorrenciaDTO segunda = cozinhaService.criarOcorrencia(new CriarOcorrenciaRequestDTO(id, "Atraso", "Pedro", "Fornecedor atrasou.", "Comprado no mercado."));
        // Evento fora da cozinha (lead) aceita a ocorrência, mas ela não aparece na tela da cozinha.
        cozinhaService.criarOcorrencia(new CriarOcorrenciaRequestDTO(lead.id(), "Outro", "Ana", "Cliente pediu outra data.", ""));
        assertThatThrownBy(() -> cozinhaService.criarOcorrencia(new CriarOcorrenciaRequestDTO(999999L, "Outro", "Ana", "Teste", "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));

        CozinhaResponseDTO tela = cozinhaService.retornarTudo();
        assertThat(tela.ocorrencias()).extracting(OcorrenciaDTO::eventoId).doesNotContain(lead.id());
        assertThat(tela.ocorrencias()).filteredOn(o -> o.eventoId().equals(id)).extracting(OcorrenciaDTO::id)
            .containsExactlyInAnyOrder(criada.id(), segunda.id());
        JsonNode oc = json.valueToTree(tela).get("ocorrencias").get(0);
        assertThat(oc.has("quando")).isTrue();
        assertThat(oc.get("resolvida").isBoolean()).isTrue();

        assertThat(cozinhaService.resolverOcorrencia(criada.id(), true).resolvida()).isTrue();
        assertThat(cozinhaService.retornarTudo().ocorrencias()).filteredOn(o -> o.id().equals(criada.id())).singleElement()
            .extracting(OcorrenciaDTO::resolvida).isEqualTo(true);
        assertThat(cozinhaService.resolverOcorrencia(criada.id(), false).resolvida()).isFalse();
        assertThatThrownBy(() -> cozinhaService.resolverOcorrencia(999999L, true)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));

        eventoService.apagarEvento(id);
        assertThat(ocorrenciaRepository.findByEventoId(id)).isEmpty();
        assertThat(ocorrenciaRepository.findByEventoId(lead.id())).hasSize(1);
    }

    private PlanoProducaoResponseDTO plano(String titulo, String unidade) {
        String hoje = LocalDate.now().toString();
        return planoService.criar(new SalvarPlanoRequestDTO(titulo, unidade, null, hoje, LocalDate.now().plusDays(6).toString(), " Semana cheia "));
    }

    private double fisico(Long insumoId) {
        return estoqueService.detalharItem(insumoId).saldo().fisico();
    }

    @Test
    void planosProducao_itemConcluidoViraLoteComBaixaUnicaEDesfazerApagaOLote() {
        String hoje = LocalDate.now().toString();
        InsumoResponseDTO farinha = insumo("Farinha de trigo", "4.00");
        // Ficha de teste: rende 10 un e leva 2 kg de farinha por receita.
        PreparacaoResponseDTO massa = ficha("Massa fresca", farinha.id());
        movimentoService.criarMovimento(new MovimentoEstoqueRequestDTO("entrada", farinha.id(), hoje, 3.0, new BigDecimal("4.00"), null, "", null, null, null));

        PlanoProducaoResponseDTO plano = plano("Restaurante · semana", "restaurante");
        assertThat(plano).extracting(PlanoProducaoResponseDTO::status, PlanoProducaoResponseDTO::obs).containsExactly("planejado", "Semana cheia");
        assertThatThrownBy(() -> planoService.criar(new SalvarPlanoRequestDTO("Errado", "restaurante", null, hoje, LocalDate.now().minusDays(1).toString(), "")))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> planoService.listar("buffet")).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.BAD_REQUEST));

        PlanoProducaoResponseDTO comItem = planoService.adicionarItens(plano.id(),
            new AdicionarItensPlanoRequestDTO(List.of(new AdicionarItensPlanoRequestDTO.Item(massa.id(), 20.0))));
        String k = comItem.itens().get(0).k();
        assertThatThrownBy(() -> planoService.adicionarItens(plano.id(),
            new AdicionarItensPlanoRequestDTO(List.of(new AdicionarItensPlanoRequestDTO.Item(massa.id(), 5.0)))))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        // 20 un = 2 receitas = 4 kg de farinha; há 3 kg em estoque.
        PlanoDetalheResponseDTO det = planoService.detalhar(plano.id());
        assertThat(det.linhas()).singleElement().satisfies(l -> {
            assertThat(l).extracting(LinhaPlanoDTO::nome, LinhaPlanoDTO::unidade, LinhaPlanoDTO::receitas, LinhaPlanoDTO::fichaStatus, LinhaPlanoDTO::feito)
                .containsExactly("Massa fresca", "un", 2.0, "Completa", false);
            assertThat(l.validadeDias()).isNotNull();
            assertThat(l.lote()).isNull();
        });
        assertThat(det.insumos()).singleElement().satisfies(x -> {
            assertThat(x.rendimentoLabel()).isEqualTo("10 un");
            assertThat(x.ins()).extracting(InsumosItemPlanoDTO.Insumo::n, InsumosItemPlanoDTO.Insumo::qb, InsumosItemPlanoDTO.Insumo::qt)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Farinha de trigo", 2.0, 4.0));
        });
        assertThat(det.compras().itens()).singleElement().extracting(CompraPlanoDTO::precisa, CompraPlanoDTO::estoque, CompraPlanoDTO::falta, CompraPlanoDTO::custo)
            .containsExactly(4.0, 3.0, 1.0, 4.0);
        assertThat(det.compras().kpis()).extracting(PlanoDetalheResponseDTO.Kpis::necessarios, PlanoDetalheResponseDTO.Kpis::aComprar,
            PlanoDetalheResponseDTO.Kpis::concluidos).containsExactly(1, 1, 0);
        JsonNode tela = json.valueToTree(det);
        assertThat(tela.get("plano").get("itens").get(0).get("k").isString()).isTrue();
        assertThat(tela.get("plano").get("previsao").isObject()).isTrue();

        assertThat(planoService.ajustarItem(plano.id(), k, 10.0).itens().get(0).qtd()).isEqualTo(10);
        assertThatThrownBy(() -> planoService.ajustarItem(plano.id(), "999999", 1.0)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));

        // Concluir: lote do restaurante, validade da ficha, e a baixa vem do lote (1 receita = 2 kg).
        Map<String, Object> feito = planoService.concluirItem(plano.id(), k);
        Long loteId = (Long) feito.get("loteId");
        LoteResponseDTO lote = loteService.detalharLote(loteId).lote();
        assertThat(lote).extracting(LoteResponseDTO::qtd, LoteResponseDTO::destino, LoteResponseDTO::unidadeNegocio, LoteResponseDTO::codigo)
            .containsExactly(10.0, "restaurante", "restaurante", feito.get("codigo"));
        assertThat(lote.obs()).isEqualTo("Plano de produção: Restaurante · semana");
        PlanoDetalheResponseDTO concluido = planoService.detalhar(plano.id());
        assertThat(lote.validade()).isEqualTo(LocalDate.now().plusDays(concluido.linhas().get(0).validadeDias()).toString());
        assertThat(fisico(farinha.id())).isEqualTo(1);
        assertThat(concluido.plano().status()).isEqualTo("concluido");
        assertThat(concluido.linhas().get(0).lote()).isEqualTo(new LinhaPlanoDTO.Lote(loteId, (String) feito.get("codigo")));
        assertThat(concluido.compras().itens()).isEmpty();
        assertThat(concluido.compras().kpis().concluidos()).isEqualTo(1);

        assertThatThrownBy(() -> planoService.concluirItem(plano.id(), k)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> planoService.ajustarItem(plano.id(), k, 3.0)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> planoService.removerItem(plano.id(), k)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> loteService.apagarLote(loteId)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> fichaService.apagarFicha(massa.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
        assertThat(planoService.listar("restaurante").kpis()).extracting(PlanosProducaoResponseDTO.Kpis::abertos, PlanosProducaoResponseDTO.Kpis::concluidos,
            PlanosProducaoResponseDTO.Kpis::prePreparosProntos).containsExactly(0, 1, 1);
        assertThat(planoService.listar("chocolates").kpis().prePreparosProntos()).isZero();

        // Desfazer apaga o lote e a baixa; um plano concluído sem nada feito volta a "em produção".
        planoService.desfazerItem(plano.id(), k);
        assertThatThrownBy(() -> loteService.detalharLote(loteId)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(fisico(farinha.id())).isEqualTo(3);
        assertThat(planoService.detalhar(plano.id()).plano().status()).isEqualTo("em_producao");
        assertThatThrownBy(() -> planoService.desfazerItem(plano.id(), k)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        // Lote que já teve uso não volta.
        Long usado = (Long) planoService.concluirItem(plano.id(), k).get("loteId");
        loteService.movimentarLote(usado, new MovimentarLoteRequestDTO("uso", 1.0, hoje, "restaurante", null, ""));
        assertThatThrownBy(() -> planoService.desfazerItem(plano.id(), k)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void planosProducao_chocolatesContamOsProprioLotesECartaAtualizaOItemAberto() {
        InsumoResponseDTO cacau = insumo("Cacau", "40.00");
        PreparacaoResponseDTO bombom = ficha("Bombom", cacau.id());
        PreparacaoResponseDTO molho = ficha("Molho branco", insumo("Leite", "5.00").id());

        PlanoProducaoResponseDTO natal = plano("Chocolates · Natal", "chocolates");
        String kb = planoService.adicionarItens(natal.id(), new AdicionarItensPlanoRequestDTO(List.of(new AdicionarItensPlanoRequestDTO.Item(bombom.id(), 10.0))))
            .itens().get(0).k();
        Long loteChocolates = (Long) planoService.concluirItem(natal.id(), kb).get("loteId");
        assertThat(loteService.detalharLote(loteChocolates).lote()).extracting(LoteResponseDTO::destino, LoteResponseDTO::unidadeNegocio)
            .containsExactly("geral", "chocolates");
        assertThat(planoService.listar("chocolates").kpis().prePreparosProntos()).isEqualTo(1);
        assertThat(planoService.listar("restaurante").kpis().prePreparosProntos()).isZero();
        assertThatThrownBy(() -> planoService.calcularCarta(natal.id(), new CalcularCartaRequestDTO(Map.of("1", 1))))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        // Carta: 4 pratos x 1,5 un de molho = 6 un, no item ainda aberto da mesma ficha.
        PratoVendaResponseDTO prato = pratoVendaService.criarPrato(new PratoVendaRequestDTO("Talharim", "campo", new BigDecimal("50.00"), "", true, "",
            List.of(new PratoVendaRequestDTO.Componente(molho.id(), 1.5)), List.of()));
        PlanoProducaoResponseDTO semana = plano("Restaurante · carta", "restaurante");
        planoService.adicionarItens(semana.id(), new AdicionarItensPlanoRequestDTO(List.of(new AdicionarItensPlanoRequestDTO.Item(molho.id(), 2.0))));
        assertThat(planoService.calcularCarta(semana.id(), new CalcularCartaRequestDTO(Map.of(String.valueOf(prato.id()), 4)))).containsEntry("fichas", 1);
        PlanoProducaoResponseDTO depois = planoService.detalhar(semana.id()).plano();
        assertThat(depois.itens()).singleElement().extracting(ItemPlanoDTO::qtd).isEqualTo(6.0);
        assertThat(depois.previsao()).containsEntry(String.valueOf(prato.id()), 4);
        assertThatThrownBy(() -> planoService.calcularCarta(semana.id(), new CalcularCartaRequestDTO(Map.of("999999", 3))))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        planoService.removerItem(semana.id(), depois.itens().get(0).k());
        assertThat(planoService.detalhar(semana.id()).linhas()).isEmpty();

        // Excluir o plano mantém o lote em Pré-preparos, que então pode ser excluído direto.
        planoService.apagar(natal.id());
        assertThatThrownBy(() -> planoService.detalhar(natal.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(loteService.detalharLote(loteChocolates).lote().unidadeNegocio()).isEqualTo("chocolates");
        loteService.apagarLote(loteChocolates);
    }
}
