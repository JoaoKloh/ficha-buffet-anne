package com.jbkloh.ficha_buffet.calculo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;

/**
 * Estoque calculado: o saldo físico é a soma dos movimentos gravados com as
 * baixas virtuais (produção do checklist e lotes de pré-preparo); as reservas
 * são o que os eventos contratados ainda vão consumir.
 */
public final class Estoque {

    public static final List<String> STATUS_QUE_RESERVAM = List.of("contratado", "cardapio_fechado", "em_producao");

    private final Map<Long, Double> saldos;
    /** insumo -> evento -> quantidade reservada. */
    private final Map<Long, Map<Long, Double>> reservas;

    private Estoque(Map<Long, Double> saldos, Map<Long, Map<Long, Double>> reservas) {
        this.saldos = saldos;
        this.reservas = reservas;
    }

    public static Estoque calcular(Dados d) {
        Map<Long, Double> saldos = new HashMap<>();
        for (MovimentoResponseDTO m : d.movimentos) {
            saldos.merge(m.insumoId(), m.qtd(), Double::sum);
        }
        for (MovimentoResponseDTO m : baixasVirtuais(d)) {
            saldos.merge(m.insumoId(), m.qtd(), Double::sum);
        }
        return new Estoque(saldos, reservas(d));
    }

    public double fisico(Long insumoId) {
        return Formato.r3(saldos.getOrDefault(insumoId, 0.0));
    }

    public double reservado(Long insumoId) {
        return reservado(insumoId, null);
    }

    public double reservado(Long insumoId, Long excetoEvento) {
        double s = 0;
        for (Map.Entry<Long, Double> r : reservasDe(insumoId).entrySet()) {
            if (!r.getKey().equals(excetoEvento)) {
                s += r.getValue();
            }
        }
        return Formato.r3(s);
    }

    public Map<Long, Double> reservasDe(Long insumoId) {
        return reservas.getOrDefault(insumoId, Map.of());
    }

    public double disponivel(Long insumoId) {
        return Formato.r3(fisico(insumoId) - reservado(insumoId));
    }

    /** Situação do item: [tom, rótulo]. */
    public List<String> situacao(InsumoResponseDTO i) {
        double s = fisico(i.id());
        if (Dominio.reutilizavel(i.tipo())) {
            return s < 0 ? List.of("bad", "Negativo") : s > 0 ? List.of("good", "OK") : List.of("", "Sem saldo");
        }
        if (s < 0) {
            return List.of("bad", "Negativo");
        }
        double disp = disponivel(i.id());
        if (disp < 0) {
            return List.of("bad", "Comprar");
        }
        if (i.estoqueMinimo() > 0 && disp < i.estoqueMinimo()) {
            return disp == 0 ? List.of("bad", "Sem saldo") : List.of("warn", "Abaixo do mínimo");
        }
        return s == 0 ? List.of("", "Sem saldo") : List.of("good", "OK");
    }

    /* ------------------------------- Baixas e reservas ------------------------------- */

    /** Baixas calculadas: produção registrada no checklist, lotes feitos para o restaurante ou estoque geral e ordens de produção conferidas. */
    public static List<MovimentoResponseDTO> baixasVirtuais(Dados d) {
        List<MovimentoResponseDTO> v = new ArrayList<>();
        for (ExecucaoResponseDTO x : d.execucoes) {
            // Evento com mapa de produção: a baixa vem das ordens conferidas, não do checklist.
            if (!"item".equals(x.kind()) || x.produzido() == null || !(x.produzido() > 0) || d.mapas.containsKey(x.eventoId())) {
                continue;
            }
            ItemCardapioResponseDTO c = d.itemCardapio(x.cardapioId());
            PreparacaoResponseDTO p = c == null ? null : d.preparacao(c.preparacaoId());
            if (p == null) {
                continue;
            }
            String data = !Formato.vazio(x.produzidoEm()) ? x.produzidoEm()
                : Formato.texto(x.atualizadoEm()).length() >= 10 ? x.atualizadoEm().substring(0, 10) : Formato.hoje();
            // O que veio de lote do estoque (que já baixou os insumos) não baixa de novo.
            double efetivo = Math.max(0, x.produzido() - usoDeLoteNoEvento(d, x.eventoId(), p.id()));
            if (efetivo > 0) {
                baixar(d, v, p, efetivo, data, "Produção · " + p.nome() + " (checklist)", x.eventoId(), Formato.texto(x.atualizadoEm()));
            }
        }
        for (LoteResponseDTO l : d.lotes) {
            PreparacaoResponseDTO p = d.preparacao(l.preparacaoId());
            if (l.baixaInsumos() && l.qtd() > 0 && p != null) {
                baixar(d, v, p, l.qtd(), l.produzidoEm(), "Lote " + l.codigo() + " · " + p.nome(), null, Formato.texto(l.criadoEm()));
            }
        }
        v.addAll(Ordens.baixas(d));
        return v;
    }

    /**
     * Quanto dos lotes do estoque (que já baixaram os insumos) foi usado num evento,
     * para uma preparação: a ordem e o checklist desse evento abatem essa quantidade.
     */
    public static double usoDeLoteNoEvento(Dados d, Long eventoId, Long preparacaoId) {
        double q = 0;
        for (LoteResponseDTO l : d.lotes) {
            if (!l.baixaInsumos() || !preparacaoId.equals(l.preparacaoId())) {
                continue;
            }
            for (LoteResponseDTO.Historico h : l.historico()) {
                if ("uso".equals(h.tipo()) && "evento".equals(h.para()) && eventoId.equals(h.eventoId())) {
                    q += h.qtd();
                }
            }
        }
        return q;
    }

    private static void baixar(Dados d, List<MovimentoResponseDTO> v, PreparacaoResponseDTO p, double produzido, String data, String motivo, Long eventoId,
            String criadoEm) {
        Map<Long, Double> porInsumo = new LinkedHashMap<>();
        Fichas.explode(d, p, produzido / Fichas.rend(p.rendimento()), (i, q, via) -> {
            if (i != null && !Dominio.reutilizavel(i.tipo())) {
                porInsumo.merge(i.id(), q, Double::sum);
            }
        });
        porInsumo.forEach((id, q) -> v.add(new MovimentoResponseDTO(0L, "baixa", id, -Formato.r3(q), Formato.r3(q), d.insumo(id).unidade(), data, motivo,
            eventoId, null, "checklist", null, criadoEm, null, null, true)));
    }

    /** Reservas por insumo e evento: pratos confirmados de eventos contratados e ainda não produzidos. */
    private static Map<Long, Map<Long, Double>> reservas(Dados d) {
        Map<Long, Map<Long, Double>> v = new HashMap<>();
        for (EventoResponseDTO e : d.eventos) {
            if (!STATUS_QUE_RESERVAM.contains(e.status())) {
                continue;
            }
            int n = Eventos.convidados(e).n();
            for (ItemCardapioResponseDTO c : d.cardapioDe(e.id())) {
                ExecucaoResponseDTO x = d.execucao("i_" + c.id());
                boolean produzido = x != null && x.produzido() != null && x.produzido() > 0;
                PreparacaoResponseDTO p = d.preparacao(c.preparacaoId());
                if (!"sim".equals(Eventos.statusItem(c)) || produzido || p == null) {
                    continue;
                }
                Fichas.explode(d, p, c.porcao() * n / Fichas.rend(p.rendimento()), (i, q, via) -> {
                    if (i != null) {
                        v.computeIfAbsent(i.id(), k -> new LinkedHashMap<>()).merge(e.id(), q, Double::sum);
                    }
                });
            }
        }
        return v;
    }

    /* ------------------------------ Louças e utensílios ------------------------------ */

    /** Louças fora em eventos: saiu − voltou − avaria. */
    public static double loucaFora(Dados d, Long insumoId) {
        double fora = 0;
        for (EventoResponseDTO e : d.eventos) {
            ExecucaoResponseDTO l = d.execucao("l_" + e.id() + "__" + insumoId);
            if (l == null || l.saiu() == null) {
                continue;
            }
            fora += Math.max(0, l.saiu() - (l.voltou() == null ? 0 : l.voltou()) - (l.avaria() == null ? 0 : l.avaria()));
        }
        return fora;
    }

    /** Louças que cada evento ainda em aberto pede (evento -> louças). */
    public static Map<Long, List<Eventos.NecessidadeLouca>> loucasPorEvento(Dados d) {
        Map<Long, List<Eventos.NecessidadeLouca>> v = new HashMap<>();
        for (EventoResponseDTO e : d.eventos) {
            if (!Eventos.ativo(e) || "acertado".equals(e.status())) {
                continue;
            }
            List<Eventos.NecessidadeLouca> ls = Eventos.necessidades(d, e.id()).loucas();
            if (!ls.isEmpty()) {
                v.put(e.id(), ls);
            }
        }
        return v;
    }

    /** Reservas de uma louça para os eventos de uma data, enquanto ela ainda não saiu: evento -> quantidade. */
    public static Map<Long, Double> loucaReservadaNaData(Dados d, Map<Long, List<Eventos.NecessidadeLouca>> porEvento, Long insumoId, String data) {
        Map<Long, Double> out = new LinkedHashMap<>();
        for (EventoResponseDTO e : d.eventos) {
            boolean reserva = STATUS_QUE_RESERVAM.contains(e.status()) || "realizado".equals(e.status());
            if (!data.equals(e.dataEvento()) || !reserva || !porEvento.containsKey(e.id())) {
                continue;
            }
            ExecucaoResponseDTO l = d.execucao("l_" + e.id() + "__" + insumoId);
            if (l != null && l.saiu() != null) {
                continue;
            }
            porEvento.get(e.id()).stream().filter(x -> x.insumo.id().equals(insumoId)).findFirst().ifPresent(x -> out.put(e.id(), x.q));
        }
        return out;
    }

    /* ------------------------------------ Lotes ------------------------------------ */

    /** Situação do lote: [tom, rótulo]. */
    public static List<String> loteSituacao(LoteResponseDTO l, String dia) {
        if (!loteAtivo(l)) {
            return List.of("", "Esgotado");
        }
        if (Formato.vazio(l.validade())) {
            return List.of("good", "Sem validade");
        }
        int c = l.validade().compareTo(dia);
        if (c < 0) {
            return List.of("bad", "Vencido");
        }
        if (c == 0) {
            return List.of("bad", "Vence hoje");
        }
        return l.validade().equals(Formato.addDays(dia, 1)) ? List.of("warn", "Vence amanhã") : List.of("good", "Válido");
    }

    public static boolean loteAtivo(LoteResponseDTO l) {
        return l.saldo() > 0;
    }

    public static int validadeDiasDe(PreparacaoResponseDTO p) {
        if (p == null) {
            return 3;
        }
        return p.validadeDias() != null ? p.validadeDias() : Dominio.VALIDADE_PADRAO.getOrDefault(p.conservacao(), 3);
    }
}
