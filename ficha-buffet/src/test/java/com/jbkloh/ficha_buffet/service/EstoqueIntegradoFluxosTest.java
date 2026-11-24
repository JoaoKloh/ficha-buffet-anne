package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.TestRsaKeys;
import com.jbkloh.ficha_buffet.dtos.req.CategoriaFinanceiraRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.EventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.InsumoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LotePreparoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MarcarPedidoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentarLoteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentoEstoqueRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PreparacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueComprasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SugestaoCompraDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;

/** Estoque integrado sobre o banco de teste: o que as abas gravam e o que leem de volta. */
@SpringBootTest
@Transactional
class EstoqueIntegradoFluxosTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    @Autowired private InsumoService insumoService;
    @Autowired private FichaService fichaService;
    @Autowired private EventoService eventoService;
    @Autowired private CategoriaFinanceiraService categoriaService;
    @Autowired private CaixaService caixaService;
    @Autowired private MovimentoEstoqueService movimentoService;
    @Autowired private EstoqueService estoqueService;
    @Autowired private LotePreparoService loteService;

    private static final String HOJE = LocalDate.now().toString();

    private InsumoResponseDTO insumo(String nome, String custo, double minimo) {
        return insumoService.criarItem(new InsumoRequestDTO(nome, "ingrediente", "Mercearia", "kg", "Refrigerado", "caixa", null, minimo, new BigDecimal(custo), ""));
    }

    private static MovimentoEstoqueRequestDTO movimento(String tipo, Long insumoId, double qtd, String validade, String unidadeUso, String compradoPor) {
        return new MovimentoEstoqueRequestDTO(tipo, insumoId, HOJE, qtd, null, null, "", validade, unidadeUso, compradoPor);
    }

    private static HttpStatus status(Throwable t) {
        if (!(t instanceof AppException e)) {
            throw new AssertionError("Esperava AppException, veio " + t, t);
        }
        return e.getHttpStatus();
    }

    @Test
    void entradaGuardaAValidadeEApareceNasValidades() {
        InsumoResponseDTO creme = insumo("Creme de leite", "10.00", 0);
        String vence = LocalDate.now().plusDays(2).toString();
        MovimentoResponseDTO m = movimentoService.criarMovimento(movimento("entrada", creme.id(), 3, vence, null, null));
        assertThat(m.validade()).isEqualTo(vence);
        // Validade só vale na entrada.
        assertThat(movimentoService.criarMovimento(movimento("sobra", creme.id(), 1, vence, null, null)).validade()).isNull();

        assertThat(estoqueService.validades().itens()).filteredOn(x -> creme.id().equals(x.insumoId()))
            .extracting(x -> x.validade() + ":" + x.qtd()).containsExactly(vence + ":3.0");
    }

    @Test
    void consumoDeOutraUnidade_baixaOEstoqueETransfereOCusto() {
        categoriaService.criarCategoria(new CategoriaFinanceiraRequestDTO("Alimentos e bebidas (A&B)", "custo_direto", 1));
        InsumoResponseDTO cafe = insumo("Café", "40.00", 0);
        movimentoService.criarMovimento(movimento("entrada", cafe.id(), 5, null, null, null));

        MovimentoResponseDTO m = movimentoService.criarMovimento(movimento("consumo", cafe.id(), 0.5, null, "restaurante", "buffet"));
        assertThat(m.tipo()).isEqualTo("consumo");
        assertThat(m.qtd()).isEqualTo(-0.5);
        assertThat(m.origem()).isEqualTo("consumo");
        assertThat(m.unidadeUso()).isEqualTo("restaurante");
        assertThat(estoqueService.detalharItem(cafe.id()).saldo().fisico()).isEqualTo(4.5);
        assertThat(movimentoService.listarMovimentos("saidas").movimentos()).extracting(x -> x.movimento().id()).contains(m.id());

        List<LancamentoListaItemDTO> transf = caixaService.transferencias("restaurante").transferencias();
        assertThat(transf).hasSize(1);
        assertThat(transf.get(0).lancamento().unidadeOrigem()).isEqualTo("buffet");
        assertThat(transf.get(0).lancamento().unidadeDestino()).isEqualTo("restaurante");
        assertThat(transf.get(0).lancamento().valor()).isEqualTo(20);

        // Sem "comprado por" (a mesma unidade), não há transferência.
        movimentoService.criarMovimento(movimento("consumo", cafe.id(), 0.5, null, "restaurante", null));
        assertThat(caixaService.transferencias("restaurante").transferencias()).hasSize(1);
        assertThat(estoqueService.conferencia().porOrigem()).extracting(o -> o.origem() + ":" + o.n()).contains("Consumo · Restaurante:2");
    }

    @Test
    void pedidoFeito_marcaEDesmarcaNaSugestaoDeCompras() {
        InsumoResponseDTO arroz = insumo("Arroz", "5.00", 10);
        assertThatThrownBy(() -> estoqueService.compras(10)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.BAD_REQUEST));

        SugestaoCompraDTO antes = item(estoqueService.compras(15), arroz.id());
        assertThat(antes.comprar()).isEqualTo(10);
        assertThat(antes.pedido()).isFalse();

        estoqueService.marcarPedido(new MarcarPedidoRequestDTO(arroz.id(), true));
        EstoqueComprasResponseDTO marcado = estoqueService.compras(15);
        assertThat(item(marcado, arroz.id()).pedido()).isTrue();
        assertThat(item(marcado, arroz.id()).pedidoEm()).isNotBlank();
        assertThat(marcado.kpis().pedidos()).isEqualTo(1);

        estoqueService.marcarPedido(new MarcarPedidoRequestDTO(arroz.id(), false));
        assertThat(item(estoqueService.compras(15), arroz.id()).pedido()).isFalse();
        assertThatThrownBy(() -> estoqueService.marcarPedido(new MarcarPedidoRequestDTO(999_999L, true)))
            .satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private static SugestaoCompraDTO item(EstoqueComprasResponseDTO r, Long insumoId) {
        return r.fornecedores().stream().flatMap(f -> f.itens().stream()).filter(x -> x.insumoId().equals(insumoId)).findFirst().orElseThrow();
    }

    @Test
    void usoDeLoteEmEvento_guardaOEventoESaiDaConferencia() {
        InsumoResponseDTO acucar = insumo("Açúcar", "5.00", 0);
        PreparacaoResponseDTO calda = fichaService.criarFicha(new PreparacaoRequestDTO("Calda", "volante", "", null, null, "", null, null, "", "", null, 10.0, "un", 1.0,
            List.of(new PreparacaoRequestDTO.Item(acucar.id(), null, 2.0, null)), null, null, null, null, null));
        EventoResponseDTO evento = eventoService.criarEvento(new EventoRequestDTO("Bodas", null, new EventoRequestDTO.NovoCliente("Cliente Bodas", "11 99999-0000"),
            "casamento", "lead", LocalDate.now().plusDays(5).toString(), "19:30", "Salão", "São Paulo", 50, 0.0, ""));
        LoteResponseDTO lote = loteService.criarLote(new LotePreparoRequestDTO(calda.id(), 20.0, HOJE, null, "restaurante", null, "Ana", ""));

        LoteResponseDTO usado = loteService.movimentarLote(lote.id(), new MovimentarLoteRequestDTO("uso", 5.0, HOJE, "evento", evento.id(), ""));
        assertThat(usado.historico()).extracting(LoteResponseDTO.Historico::eventoId).containsExactly(evento.id());
        // Uso no restaurante não guarda evento, mesmo que venha no corpo.
        assertThat(loteService.movimentarLote(lote.id(), new MovimentarLoteRequestDTO("uso", 1.0, HOJE, "restaurante", evento.id(), "")).historico())
            .extracting(LoteResponseDTO.Historico::eventoId).containsExactly(evento.id(), null);
        assertThat(estoqueService.conferencia().pontos()).noneMatch(p -> lote.id().equals(p.loteId()));
        // O evento com uso de lote não pode ser excluído.
        assertThatThrownBy(() -> eventoService.apagarEvento(evento.id())).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void telasSemDados_respondemEstruturaCompleta() {
        assertThat(estoqueService.compras(7).fornecedores()).isNotNull();
        assertThat(estoqueService.validades().kpis()).isNotNull();
        assertThat(estoqueService.perdas("2020-01").perdas()).isEmpty();
        assertThat(estoqueService.perdas("2020-01").porGrupo()).isEmpty();
        assertThat(estoqueService.conferencia().pontos()).isNotNull();
        assertThatThrownBy(() -> estoqueService.perdas("2020-13")).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> estoqueService.perdas(null)).satisfies(t -> assertThat(status(t)).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
