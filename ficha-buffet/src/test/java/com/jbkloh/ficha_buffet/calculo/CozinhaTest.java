package com.jbkloh.ficha_buffet.calculo;

import static com.jbkloh.ficha_buffet.calculo.Fixtures.cardapio;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.checklist;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.evento;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.insumo;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.item;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.prep;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.TarefaCozinhaDTO;
import com.jbkloh.ficha_buffet.model.json.ComponenteEnvio;
import com.jbkloh.ficha_buffet.model.json.EtapaPreparacao;

/** Projeção do evento para a cozinha e regras do checklist de envio. */
class CozinhaTest {

    private static final String HOJE = "2026-12-01";

    private static Dados base() {
        Dados d = new Dados();
        d.insumos.addAll(List.of(insumo(1, "Frango", "ingrediente", "kg", "Refrigerado", 0, 10), insumo(2, "Bandeja", "louca", "un", "Depósito de louças", 0, 0)));
        d.preparacoes.addAll(List.of(
            prep(10, "Coxinha", "volante", 10, "un", List.of(item(1L, null, 2, 1)), List.of(new PreparacaoResponseDTO.Louca(2L, 20)), List.of(), List.of()),
            prep(11, "Ceviche", "ilha", 1, "kg", List.of(), List.of(),
                List.of(new EtapaPreparacao("Curar o peixe", "Frios", 1), new EtapaPreparacao("Porcionar", "", null)),
                List.of(new ComponenteEnvio("Leite de tigre", "L", 0.02, "refrigerado", ""))),
            prep(12, "Sobremesa a definir", "sobremesa", 10)));
        d.eventos.add(evento(1, "contratado", "2026-12-12", 95, 5, "Sem glúten na mesa 3 · Sinal de R$ 5.000 pago\nPagamento via pix", null));
        d.cardapio.addAll(List.of(cardapio(100, 1, 10, 2, "sim"), cardapio(101, 1, 11, 0.05, "sim"), cardapio(102, 1, 12, 1, "definir")));
        return d;
    }

    private static CozinhaProjecaoDTO projecao(Dados d) {
        return Cozinha.projecao(d, d.evento(1L), Estoque.calcular(d));
    }

    private static CozinhaProjecaoDTO.Tarefa tarefa(CozinhaProjecaoDTO pj, String k) {
        return pj.tarefas().stream().filter(t -> t.k().equals(k)).findFirst().orElse(null);
    }

    @Test
    void projecao_produzParaConvidadosMaisMargemArredondandoParaCima() {
        CozinhaProjecaoDTO pj = projecao(base());
        assertThat(pj.base()).isEqualTo(95);
        assertThat(pj.n()).isEqualTo(100);
        CozinhaProjecaoDTO.Prato coxinha = pj.pratos().stream().filter(p -> p.nome().equals("Coxinha")).findFirst().orElseThrow();
        assertThat(coxinha.total()).isEqualTo(200);
        assertThat(coxinha.lotes()).isEqualTo(20);
        assertThat(coxinha.ins().get(0)).extracting(CozinhaProjecaoDTO.InsumoPrato::qb, CozinhaProjecaoDTO.InsumoPrato::qt, CozinhaProjecaoDTO.InsumoPrato::cons)
            .containsExactly(2.0, 40.0, "refrigerado");
        assertThat(pj.loucas()).extracting(CozinhaProjecaoDTO.Louca::n, CozinhaProjecaoDTO.Louca::q).containsExactly(org.assertj.core.groups.Tuple.tuple("Bandeja", 20.0));
    }

    @Test
    void observacoes_saemSemOQueEFinanceiro() {
        assertThat(Cozinha.observacoes(base().evento(1L))).isEqualTo("Sem glúten na mesa 3");
    }

    @Test
    void tarefas_fixasEtapasDaFichaEProduzirSoParaPratosConfirmadosSemEtapas() {
        CozinhaProjecaoDTO pj = projecao(base());
        assertThat(pj.tarefas().stream().filter(t -> t.k().startsWith("f_"))).hasSize(6);
        assertThat(tarefa(pj, "p_100")).extracting(CozinhaProjecaoDTO.Tarefa::desc, CozinhaProjecaoDTO.Tarefa::dn, CozinhaProjecaoDTO.Tarefa::padrao)
            .containsExactly("Produzir Coxinha", 1, true);
        assertThat(tarefa(pj, "e_101_0")).extracting(CozinhaProjecaoDTO.Tarefa::desc, CozinhaProjecaoDTO.Tarefa::praca, CozinhaProjecaoDTO.Tarefa::dn)
            .containsExactly("Curar o peixe", "Frios", 1);
        assertThat(tarefa(pj, "e_101_1").dn()).isEqualTo(-1);
        assertThat(tarefa(pj, "p_102")).isNull();
    }

    @Test
    void tarefas_datasDnEAjustesDaCozinha() {
        Dados d = base();
        d.execucoes.add(Fixtures.exec("t_1__p_100", "tarefa", 1, null, null, "", "", null, null, false, null, null, null, true, "Ana", true, 3));
        List<TarefaCozinhaDTO> ts = Cozinha.tarefas(d, projecao(d));
        TarefaCozinhaDTO produzir = ts.stream().filter(t -> t.tarefa().k().equals("p_100")).findFirst().orElseThrow();
        assertThat(produzir.data()).isEqualTo("2026-12-09");
        assertThat(produzir.feito()).isTrue();
        assertThat(produzir.movida()).isTrue();
        assertThat(produzir.dnEfetivo()).isEqualTo(3);
        assertThat(produzir.tarefa().resp()).isEqualTo("Ana");
        TarefaCozinhaDTO fixa = ts.stream().filter(t -> t.tarefa().k().equals("f_0")).findFirst().orElseThrow();
        assertThat(fixa.data()).isEqualTo("2026-12-05");
        assertThat(fixa.feito()).isFalse();
        TarefaCozinhaDTO aPosicionar = ts.stream().filter(t -> t.tarefa().k().equals("e_101_1")).findFirst().orElseThrow();
        assertThat(aPosicionar.data()).isNull();
        assertThat(aPosicionar.dnEfetivo()).isNull();
    }

    @Test
    void cozinhaSoAcompanhaEventosContratadosAte10DiasDepoisDaData() {
        assertThat(Cozinha.relevante(evento(1, "lead", 100), HOJE)).isFalse();
        assertThat(Cozinha.relevante(evento(1, "em_producao", "2026-11-25", 100, 0, "", null), HOJE)).isTrue();
        assertThat(Cozinha.relevante(evento(1, "realizado", "2026-11-10", 100, 0, "", null), HOJE)).isFalse();
        assertThat(Cozinha.projecoes(base(), HOJE)).extracting(CozinhaProjecaoDTO::eventoId).containsExactly(1L);
    }

    @Test
    void checklist_linhasDePratoComponentesInsumosDaFichaELoucas() {
        List<Cozinha.Linha> linhas = Cozinha.linhas(projecao(base()));
        // "A definir" fica de fora; sem componentes de envio, viajam os insumos da ficha (chave f_<insumo>_<via>).
        assertThat(linhas).extracting(Cozinha.Linha::id).containsExactlyInAnyOrder("i_101", "c_101__c0", "i_100", "c_100__f_1_", "l_1__2");
        Cozinha.Linha louca = linhas.stream().filter(r -> r.tipo().equals("louca")).findFirst().orElseThrow();
        assertThat(louca.campoEnvio()).isEqualTo("saiu");
        assertThat(louca.prod()).isFalse();
    }

    @Test
    void checklist_soSaiComProducaoDuasAssinaturasETemperaturaNaFaixa() {
        List<Cozinha.Linha> linhas = Cozinha.linhas(projecao(base()));
        Cozinha.Linha prato = linhas.stream().filter(r -> r.id().equals("i_101")).findFirst().orElseThrow(); // ceviche: refrigerado
        assertThat(Cozinha.podeSair(prato, checklist(200.0, "Ana", "Bia", null))).isFalse();
        assertThat(Cozinha.podeSair(prato, checklist(200.0, "Ana", "Bia", 3.0))).isTrue();
        assertThat(Cozinha.podeSair(prato, checklist(200.0, "Ana", "Bia", 9.0))).isFalse();
        assertThat(Cozinha.podeSair(prato, checklist(200.0, "", "Bia", 3.0))).isFalse();
        Cozinha.Linha louca = linhas.stream().filter(r -> r.tipo().equals("louca")).findFirst().orElseThrow();
        assertThat(Cozinha.podeSair(louca, checklist(null, "", "Bia", null))).isTrue();
    }

    @Test
    void faixasDeTemperaturaEArredondamentos() {
        assertThat(Cozinha.tempFora("refrigerado", 4.0)).isFalse();
        assertThat(Cozinha.tempFora("refrigerado", 4.5)).isTrue();
        assertThat(Cozinha.tempFora("congelado", -18.0)).isFalse();
        assertThat(Cozinha.tempFora("congelado", -10.0)).isTrue();
        assertThat(Cozinha.tempFora("quente", 59.0)).isTrue();
        assertThat(Cozinha.tempFora("ambiente", 30.0)).isFalse();
        assertThat(Cozinha.tempFora("quente", null)).isNull();
        assertThat(Cozinha.arredondar(1.23, "kg")).isEqualTo(1.2);
        assertThat(Cozinha.arredondar(0.01, "L")).isEqualTo(0.1);
        assertThat(Cozinha.arredondar(2.1, "un")).isEqualTo(3);
        assertThat(Cozinha.arredondar(0, "un")).isZero();
    }
}
