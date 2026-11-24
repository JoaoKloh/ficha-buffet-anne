package com.jbkloh.ficha_buffet.calculo;

import static com.jbkloh.ficha_buffet.calculo.Fixtures.cardapio;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.evento;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.insumo;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.item;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.prep;
import static com.jbkloh.ficha_buffet.calculo.Fixtures.producao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoOrdemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LiberacaoExpedicaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MapaProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OrdemProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.model.json.ComponenteEnvio;

/** Ordens de produção geradas do cardápio e das fichas, situação, liberação e baixa no estoque (porta do Anne Gestão). */
class OrdensTest {

    private static final String DIA_EVENTO = "2026-12-12";
    private static final MapaProducaoDTO MAPA = new MapaProducaoDTO("2026-12-01T10:00:00Z", "", "", "");

    /** Ficha com os campos de produção. */
    private static PreparacaoResponseDTO comProducao(PreparacaoResponseDTO p, String setor, Integer dn, Integer tempoMin, boolean pronto) {
        return new PreparacaoResponseDTO(p.id(), p.nome(), p.categoria(), p.praca(), p.conservacao(), p.validadeDias(), setor, dn, tempoMin, "Fritadeira", "",
            pronto, p.rendimento(), p.unidadeRendimento(), p.porcaoPadrao(), p.itens(), p.loucas(), p.etapas(), p.componentes(), p.obsEnvio(),
            "Modelar\n\nFritar", p.atualizadoEm());
    }

    /**
     * Coxinha (Salgados, D-5 padrão, 30 min por receita) com massa (sub-receita) e molho; ceviche comprado pronto;
     * sobremesa "a definir" fora das ordens. 100 convidados de produção.
     */
    private static Dados base() {
        Dados d = new Dados();
        d.insumos.addAll(List.of(insumo(1, "Frango", "ingrediente", "kg", "Refrigerado", 0, 10), insumo(2, "Farinha", "ingrediente", "kg", "Seco", 0, 5),
            insumo(3, "Bandeja", "louca", "un", "Depósito de louças", 0, 0)));
        d.preparacoes.addAll(List.of(
            comProducao(prep(10, "Coxinha", "volante", 10, "un", List.of(item(1L, null, 2, 1), item(null, 20L, 0.5, 1), item(null, 21L, 0.1, 1), item(3L, null, 1, 1)),
                List.of(), List.of(), List.of(new ComponenteEnvio("Molho rosé", "kg", 0.01, "refrigerado", ""))), "", null, 30, false),
            prep(20, "Massa de coxinha", "base", 1, "kg", List.of(item(2L, null, 0.6, 1)), List.of(), List.of(), List.of()),
            prep(21, "Molho de tomate", "base", 1, "l", List.of(), List.of(), List.of(), List.of()),
            comProducao(prep(11, "Ceviche", "ilha", 1, "kg", List.of(), List.of(), List.of(), List.of()), "", null, null, true),
            prep(12, "Sobremesa a definir", "sobremesa", 10)));
        d.eventos.add(evento(1, "contratado", DIA_EVENTO, 95, 5, "", null));
        d.cardapio.addAll(List.of(cardapio(100, 1, 10, 2, "sim"), cardapio(101, 1, 11, 0.05, "sim"), cardapio(102, 1, 12, 1, "definir")));
        d.mapas.put(1L, MAPA);
        return d;
    }

    private static List<OrdemProducaoDTO> ordens(Dados d, String hoje, String agora) {
        return Ordens.ordens(d, d.evento(1L), hoje, agora);
    }

    private static OrdemProducaoDTO ordem(List<OrdemProducaoDTO> os, String k) {
        return os.stream().filter(o -> o.k().equals(k)).findFirst().orElseThrow();
    }

    private static Ordens.Execucao execucao(String k, String status, Double qtd, String conferidoEm, String lote) {
        return new Ordens.Execucao(1L, k, status, null, "",
            new ExecucaoOrdemDTO("Ana", null, "Ana", "2026-12-07T12:00:00Z", qtd, 0, "", "Bia", conferidoEm, lote, "", "", List.of()));
    }

    @Test
    void semMapa_naoHaOrdens() {
        Dados d = base();
        d.mapas.clear();
        assertThat(ordens(d, "2026-12-01", "08:00")).isEmpty();
    }

    @Test
    void gerar_principalSubReceitasMolhoFinalizacaoEExpedicao_naOrdemDoMapa() {
        List<OrdemProducaoDTO> os = ordens(base(), "2026-12-01", "08:00");
        // Prazo mais longo primeiro; no mesmo dia, pela etapa e pelo nome; a sobremesa "a definir" não gera ordem.
        assertThat(os).extracting(OrdemProducaoDTO::k, OrdemProducaoDTO::etapa, OrdemProducaoDTO::dn).containsExactly(
            tuple("s_20", "pre", 6), tuple("s_21", "molho", 6), tuple("m_100", "principal", 5), tuple("m_101", "pre", 1),
            tuple("f_101", "montagem", 0), tuple("f_100", "montagem", 0), tuple("x_exp", "expedicao", 0));

        OrdemProducaoDTO coxinha = ordem(os, "m_100");
        assertThat(coxinha).extracting(OrdemProducaoDTO::setor, OrdemProducaoDTO::q, OrdemProducaoDTO::u, OrdemProducaoDTO::receitas, OrdemProducaoDTO::data,
            OrdemProducaoDTO::tempoMin, OrdemProducaoDTO::dnPadrao, OrdemProducaoDTO::status, OrdemProducaoDTO::cid)
            .containsExactly("quente", 200.0, "un", 20.0, "2026-12-07", 600, true, "pendente", 100L);
        // Insumos diretos e sub-receitas (estas com ordem própria); louça não entra.
        assertThat(coxinha.ins()).extracting(OrdemProducaoDTO.Insumo::n, OrdemProducaoDTO.Insumo::q, OrdemProducaoDTO.Insumo::u, OrdemProducaoDTO.Insumo::sub)
            .contains(tuple("Frango", 40.0, "kg", null), tuple("Massa de coxinha", 10.0, "kg", true));
        assertThat(coxinha.passos()).containsExactly("Modelar", "Fritar");
        assertThat(coxinha.comp()).extracting(OrdemProducaoDTO.Componente::n, OrdemProducaoDTO.Componente::q).containsExactly(tuple("Molho rosé", 2.0));
        assertThat(coxinha.valDias()).isEqualTo(3);
        assertThat(coxinha.armaz()).isEqualTo("Refrigerado (0 a 4 °C)");

        OrdemProducaoDTO massa = ordem(os, "s_20");
        assertThat(massa).extracting(OrdemProducaoDTO::q, OrdemProducaoDTO::receitas, OrdemProducaoDTO::origem, OrdemProducaoDTO::data)
            .containsExactly(10.0, 10.0, List.of("Coxinha"), "2026-12-06");
        assertThat(massa.ins()).extracting(OrdemProducaoDTO.Insumo::n, OrdemProducaoDTO.Insumo::q).containsExactly(tuple("Farinha", 6.0));

        OrdemProducaoDTO finalizar = ordem(os, "f_100");
        assertThat(finalizar.nome()).isEqualTo("Finalizar e montar · Coxinha");
        assertThat(finalizar.setor()).isEqualTo("montagem");
        assertThat(finalizar.passos()).contains("Finalizar, porcionar e montar conforme a ficha (componentes separados: Molho rosé)");
        assertThat(ordem(os, "x_exp").passos()).hasSize(8);
    }

    @Test
    void compradoPronto_viraRetiradaDoEstoqueNaExpedicaoUmDiaAntes() {
        OrdemProducaoDTO ceviche = ordem(ordens(base(), "2026-12-01", "08:00"), "m_101");
        assertThat(ceviche).extracting(OrdemProducaoDTO::nome, OrdemProducaoDTO::etapa, OrdemProducaoDTO::setor, OrdemProducaoDTO::dn, OrdemProducaoDTO::tempoMin)
            .containsExactly("Retirar do estoque · Ceviche (comprado pronto)", "pre", "expedicao", 1, 0);
        assertThat(ceviche.passos()).first().isEqualTo("Item comprado pronto do fornecedor: a produção não faz");
    }

    @Test
    void plano_daGestaoVenceOsPadroesDaFicha() {
        Dados d = base();
        d.planosOrdem.put(Ordens.chave(1L, "m_100"), new Ordens.Plano(1L, "m_100", 3, "10:00", "fria", "Carla"));
        OrdemProducaoDTO coxinha = ordem(ordens(d, "2026-12-01", "08:00"), "m_100");
        assertThat(coxinha).extracting(OrdemProducaoDTO::dn, OrdemProducaoDTO::dnPadrao, OrdemProducaoDTO::data, OrdemProducaoDTO::hora, OrdemProducaoDTO::setor,
            OrdemProducaoDTO::resp).containsExactly(3, false, "2026-12-09", "10:00", "fria", "Carla");
        // A sub-receita conta o prazo pelo padrão da ficha do prato, não pelo plano.
        assertThat(ordem(ordens(d, "2026-12-01", "08:00"), "s_20").dn()).isEqualTo(6);
    }

    @Test
    void atraso_depoisDoDiaOuNoDiaDepoisDoHorarioLimite_eNaoContaOQueJaFoiEntregue() {
        Dados d = base();
        d.planosOrdem.put(Ordens.chave(1L, "m_101"), new Ordens.Plano(1L, "m_101", 5, "10:00", "expedicao", ""));
        List<OrdemProducaoDTO> os = ordens(d, "2026-12-07", "11:00");
        assertThat(ordem(os, "s_20").atrasada()).isTrue();
        assertThat(ordem(os, "m_101").atrasada()).isTrue();
        assertThat(ordem(os, "m_100").atrasada()).isFalse();
        assertThat(ordem(ordens(d, "2026-12-07", "09:59"), "m_101").atrasada()).isFalse();

        d.execucoesOrdem.put(Ordens.chave(1L, "s_20"), execucao("s_20", "em_conferencia", 10.0, null, ""));
        assertThat(ordem(ordens(d, "2026-12-07", "11:00"), "s_20")).extracting(OrdemProducaoDTO::status, OrdemProducaoDTO::atrasada)
            .containsExactly("em_conferencia", false);
    }

    @Test
    void conferida_temEtiquetaDeRastreabilidade() {
        Dados d = base();
        d.execucoesOrdem.put(Ordens.chave(1L, "s_20"), execucao("s_20", "concluida", 10.0, "2026-12-06T15:00:00Z", "26001-S1234"));
        OrdemProducaoDTO massa = ordem(ordens(d, "2026-12-07", "08:00"), "s_20");
        assertThat(massa.etiqueta()).startsWith("Massa de coxinha\nEvento: 26-001 · Evento 1 · 12/12/2026\nLote: 26001-S1234 · Qtd: 10 kg");
        assertThat(massa.etiqueta()).contains("Conferido por: Bia");
        assertThat(ordem(ordens(d, "2026-12-07", "08:00"), "m_100").etiqueta()).isNull();
    }

    @Test
    void estoque_comMapaABaixaVemDasOrdensConferidasENaoDoChecklist() {
        Dados d = base();
        d.execucoes.add(producao(100, 200));
        // Sem mapa: o checklist dá baixa da receita toda, sub-receitas incluídas.
        d.mapas.clear();
        assertThat(Estoque.baixasVirtuais(d)).extracting(MovimentoResponseDTO::origem).containsOnly("checklist");

        d.mapas.put(1L, MAPA);
        d.execucoesOrdem.put(Ordens.chave(1L, "m_100"), execucao("m_100", "concluida", 100.0, "2026-12-07T12:00:00Z", "L1"));
        d.execucoesOrdem.put(Ordens.chave(1L, "s_20"), execucao("s_20", "em_conferencia", 10.0, null, ""));
        List<MovimentoResponseDTO> baixas = Estoque.baixasVirtuais(d);
        // Só os insumos diretos da coxinha conferida (10 receitas × 2 kg); a massa ainda não foi conferida, a louça não baixa.
        assertThat(baixas).extracting(MovimentoResponseDTO::insumoId, MovimentoResponseDTO::qtd, MovimentoResponseDTO::origem, MovimentoResponseDTO::eventoId)
            .containsExactly(tuple(1L, -20.0, "ordem", 1L));
        assertThat(baixas.get(0).motivo()).isEqualTo("Ordem de produção · Coxinha · lote L1");
        assertThat(Eventos.travado(d, d.evento(1L))).contains("ordens de produção conferidas");
    }

    @Test
    void liberacao_bloqueadaComOrdensAbertasEProntaComTudoConferido() {
        Dados d = base();
        CozinhaProjecaoDTO pj = Cozinha.projecao(d, d.evento(1L), Estoque.calcular(d));
        LiberacaoExpedicaoDTO l = Ordens.liberacao(d, pj, ordens(d, "2026-12-01", "08:00"));
        assertThat(l.ok()).isFalse();
        assertThat(l.situacao()).containsExactly("bad", "Bloqueado");
        assertThat(l.ordensProducao()).isEqualTo(4);
        assertThat(l.ordensAbertas()).isEqualTo(4);
        assertThat(l.faltam()).contains("4 ordem(ns) abertas", "9 conferência(s)", "horário de saída");

        // Evento sem pratos: só a expedição, que não conta; com as conferências e o horário, pronto para liberar.
        d = base();
        d.cardapio.clear();
        Map<String, Boolean> tudo = new java.util.HashMap<>();
        Dominio.ITENS_LIBERACAO.keySet().forEach(k -> tudo.put(k, true));
        d.liberacoes.put(1L, new Ordens.Liberacao(1L, tudo, "16:00", "", null, false, ""));
        pj = Cozinha.projecao(d, d.evento(1L), Estoque.calcular(d));
        l = Ordens.liberacao(d, pj, ordens(d, "2026-12-01", "08:00"));
        assertThat(l.ok()).isTrue();
        assertThat(l.situacao()).containsExactly("accent", "Pronto para liberar");

        d.liberacoes.put(1L, new Ordens.Liberacao(1L, Map.of(), "", "Chef", "2026-12-12T15:00:00Z", true, "Faltou o veículo"));
        assertThat(Ordens.liberacao(d, pj, List.of()).situacao()).containsExactly("warn", "Liberado com pendências");
    }

    @Test
    void projecao_levaOMapaEAAntecedenciaEfetivaDaFicha() {
        Dados d = base();
        CozinhaProjecaoDTO pj = Cozinha.projecao(d, d.evento(1L), Estoque.calcular(d));
        assertThat(pj.producao()).isEqualTo(MAPA);
        assertThat(pj.pratos()).extracting(CozinhaProjecaoDTO.Prato::nome, CozinhaProjecaoDTO.Prato::dnf, CozinhaProjecaoDTO.Prato::pronto)
            .contains(tuple("Coxinha", 5, false), tuple("Ceviche", 5, true));
    }
}
