package com.jbkloh.ficha_buffet.calculo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jbkloh.ficha_buffet.dtos.resp.EstoqueComprasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueConferenciaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoquePerdasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueValidadesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PerdaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PontoConferenciaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SugestaoCompraDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ValidadeItemDTO;

/**
 * Estoque integrado: sugestão de compras, validades, perdas do mês e conferência.
 * Tudo calculado sobre o retrato dos dados; só o "pedido feito" das compras vem de fora.
 */
public final class EstoqueIntegrado {

    private EstoqueIntegrado() {}

    public static final String SEM_FORNECEDOR = "Sem fornecedor definido";
    /** Janela da aba Validades: vencidos e o que vence nos próximos dias. */
    public static final int DIAS_VALIDADE = 7;

    public static final String GRUPO_PERDA_INSUMO = "Perda de insumo";
    public static final String GRUPO_AVARIA = "Avaria de louça e utensílio";
    public static final String GRUPO_DESCARTE_LOTE = "Descarte de pré-preparo";

    /** Pedido feito de um item: dia (AAAA-MM-DD, no fuso do servidor) e instante ISO. */
    public record Pedido(String dia, String em) {}

    /** Louças e utensílios não se consomem: ficam fora de compras, validades e perdas de insumo. */
    static boolean consumivel(InsumoResponseDTO i) {
        return i != null && !Dominio.reutilizavel(i.tipo());
    }

    /** Valor de um movimento: quantidade × preço pago no movimento ou, sem ele, o preço atual do item. */
    static double valorMov(Dados d, MovimentoResponseDTO m) {
        double custo = m.custoUnit() != null && m.custoUnit() > 0 ? m.custoUnit() : custoAtual(d, m.insumoId());
        return Math.abs(m.qtd()) * custo;
    }

    private static double custoAtual(Dados d, Long insumoId) {
        InsumoResponseDTO i = d.insumo(insumoId);
        return i == null ? 0 : i.custo();
    }

    private static double custoPorUnidadeDoLote(Dados d, LoteResponseDTO l) {
        PreparacaoResponseDTO p = d.preparacao(l.preparacaoId());
        return p == null ? 0 : Fichas.custoPrep(d, p).porUnid();
    }

    /* -------------------------------- Compras -------------------------------- */

    /**
     * Sugestão de compras: reservas dos eventos até o fim do horizonte + mínimo − saldo físico
     * (o saldo negativo conta como zero), arredondado para cima em embalagens de compra.
     * O item com pedido feito continua na lista, marcado, até entrar uma entrada dele com data
     * igual ou posterior à do pedido.
     */
    public static EstoqueComprasResponseDTO compras(Dados d, int horizonte, Map<Long, Pedido> pedidos, String hoje) {
        Estoque est = Estoque.calcular(d);
        String limite = Formato.addDays(hoje, horizonte);
        Map<Long, String> ultimaEntrada = new HashMap<>();
        for (MovimentoResponseDTO m : d.movimentos) {
            if ("entrada".equals(m.tipo()) && !Formato.vazio(m.data())) {
                ultimaEntrada.merge(m.insumoId(), m.data(), (a, b) -> a.compareTo(b) >= 0 ? a : b);
            }
        }
        List<SugestaoCompraDTO> itens = new ArrayList<>();
        for (InsumoResponseDTO i : d.insumos) {
            if (!consumivel(i)) {
                continue;
            }
            double reservado = 0;
            List<String> eventos = new ArrayList<>();
            for (Map.Entry<Long, Double> r : est.reservasDe(i.id()).entrySet()) {
                EventoResponseDTO e = d.evento(r.getKey());
                if (e != null && !Formato.vazio(e.dataEvento()) && e.dataEvento().compareTo(limite) <= 0) {
                    reservado += r.getValue();
                    eventos.add(e.codigo());
                }
            }
            double minimo = i.estoqueMinimo();
            double saldo = est.fisico(i.id());
            double falta = Formato.r3(reservado + minimo - Math.max(0, saldo));
            if (!(falta > 0.0005)) {
                continue;
            }
            double fator = i.fatorCompra() == null ? 0 : i.fatorCompra();
            int embalagens = fator > 0 ? (int) Math.ceil(falta / fator - 1e-9) : 0;
            double comprar = embalagens > 0 ? Formato.r3(embalagens * fator) : falta;
            Pedido p = pedidos.get(i.id());
            boolean pedido = p != null && ultimaEntrada.getOrDefault(i.id(), "").compareTo(p.dia()) < 0;
            String fornecedor = Formato.vazio(i.fornecedor()) ? SEM_FORNECEDOR : i.fornecedor().trim();
            itens.add(new SugestaoCompraDTO(i.id(), i.nome(), i.unidade(), Formato.texto(i.unidadeCompra()), fornecedor, saldo, Formato.r3(reservado),
                minimo, falta, embalagens, comprar, Formato.round2(comprar * i.custo()), !(i.custo() > 0), eventos, pedido, pedido ? p.em() : null));
        }
        itens.sort(Comparator.<SugestaoCompraDTO, Boolean>comparing(x -> SEM_FORNECEDOR.equals(x.fornecedor()))
            .thenComparing(Formato.porNome(SugestaoCompraDTO::fornecedor)).thenComparing(Formato.porNome(SugestaoCompraDTO::nome)));

        Map<String, List<SugestaoCompraDTO>> porFornecedor = new LinkedHashMap<>();
        itens.forEach(x -> porFornecedor.computeIfAbsent(x.fornecedor(), k -> new ArrayList<>()).add(x));
        List<EstoqueComprasResponseDTO.Fornecedor> fornecedores = porFornecedor.entrySet().stream()
            .map(f -> new EstoqueComprasResponseDTO.Fornecedor(f.getKey(),
                Formato.round2(Formato.soma(f.getValue().stream().filter(x -> !x.pedido()).toList(), SugestaoCompraDTO::valor)), f.getValue()))
            .toList();

        List<SugestaoCompraDTO> pendentes = itens.stream().filter(x -> !x.pedido()).toList();
        int eventosNoPeriodo = (int) d.eventos.stream().filter(e -> Estoque.STATUS_QUE_RESERVAM.contains(e.status()) && !Formato.vazio(e.dataEvento())
            && e.dataEvento().compareTo(hoje) >= 0 && e.dataEvento().compareTo(limite) <= 0).count();
        return new EstoqueComprasResponseDTO(horizonte, new EstoqueComprasResponseDTO.Kpis(pendentes.size(),
            (int) pendentes.stream().map(SugestaoCompraDTO::fornecedor).distinct().count(), Formato.round2(Formato.soma(pendentes, SugestaoCompraDTO::valor)),
            pendentes.stream().anyMatch(SugestaoCompraDTO::semPreco), itens.size() - pendentes.size(), eventosNoPeriodo), fornecedores);
    }

    /* -------------------------------- Validades -------------------------------- */

    /**
     * Vencidos e vencendo até hoje + 7 dias. Insumos pela validade informada na entrada: o que
     * entrou primeiro é considerado usado primeiro, então o saldo atual é coberto pelas entradas
     * (e sobras) mais recentes. Pré-preparos pela validade do lote com saldo.
     */
    public static EstoqueValidadesResponseDTO validades(Dados d, String hoje) {
        Estoque est = Estoque.calcular(d);
        String limite = Formato.addDays(hoje, DIAS_VALIDADE);
        Map<Long, List<MovimentoResponseDTO>> entradas = new HashMap<>();
        for (MovimentoResponseDTO m : d.movimentos) {
            if (m.qtd() > 0 && ("entrada".equals(m.tipo()) || "sobra".equals(m.tipo()))) {
                entradas.computeIfAbsent(m.insumoId(), k -> new ArrayList<>()).add(m);
            }
        }
        Comparator<MovimentoResponseDTO> maisNovas = Comparator.comparing((MovimentoResponseDTO m) -> Formato.texto(m.data()))
            .thenComparing(m -> Formato.texto(m.criadoEm())).reversed();

        List<ValidadeItemDTO> itens = new ArrayList<>();
        for (InsumoResponseDTO i : d.insumos) {
            double saldo = est.fisico(i.id());
            if (!consumivel(i) || !(saldo > 0)) {
                continue;
            }
            for (MovimentoResponseDTO m : entradas.getOrDefault(i.id(), List.of()).stream().sorted(maisNovas).toList()) {
                if (!(saldo > 0)) {
                    break;
                }
                double parte = Math.min(saldo, m.qtd());
                saldo -= parte;
                if (!Formato.vazio(m.validade()) && m.validade().compareTo(limite) <= 0) {
                    double qtd = Formato.r3(parte);
                    String detalhe = Ordens.qU(qtd, i.unidade()) + " · entrada de " + Formato.fdate(m.data())
                        + (Formato.vazio(m.motivo()) ? "" : " · " + m.motivo());
                    itens.add(new ValidadeItemDTO("insumo", m.validade(), (int) Formato.diffDays(m.validade(), hoje), i.nome(), detalhe, qtd, i.unidade(),
                        Formato.round2(qtd * i.custo()), i.id(), null));
                }
            }
        }
        for (LoteResponseDTO l : d.lotes) {
            if (!Estoque.loteAtivo(l) || Formato.vazio(l.validade()) || l.validade().compareTo(limite) > 0) {
                continue;
            }
            PreparacaoResponseDTO p = d.preparacao(l.preparacaoId());
            double saldo = Formato.r3(l.saldo());
            double valor = Formato.round2(custoPorUnidadeDoLote(d, l) * saldo);
            String detalhe = "Lote " + Formato.texto(l.codigo()) + " · " + Formato.fmtN(saldo) + " " + Fichas.unidRendLabel(l.unidade(), saldo) + " · "
                + Formato.brl(valor);
            itens.add(new ValidadeItemDTO("lote", l.validade(), (int) Formato.diffDays(l.validade(), hoje), p == null ? "Pré-preparo" : p.nome(), detalhe,
                saldo, l.unidade(), valor, null, l.id()));
        }
        itens.sort(Comparator.comparing(ValidadeItemDTO::validade));

        int vencidos = (int) itens.stream().filter(x -> x.validade().compareTo(hoje) < 0).count();
        int semValidade = (int) d.insumos.stream().filter(i -> consumivel(i) && est.fisico(i.id()) > 0
            && d.movimentos.stream().noneMatch(m -> i.id().equals(m.insumoId()) && !Formato.vazio(m.validade()))).count();
        return new EstoqueValidadesResponseDTO(new EstoqueValidadesResponseDTO.Kpis(vencidos, itens.size() - vencidos,
            Formato.round2(Formato.soma(itens, ValidadeItemDTO::valor)), semValidade), itens);
    }

    /* ---------------------------------- Perdas --------------------------------- */

    /**
     * Perdas do mês (AAAA-MM): movimentos de perda (louça e utensílio contam como avaria) e
     * descartes de lote de pré-preparo, pelo custo atual da ficha. Os KPIs comparam com as
     * entradas do mês e mostram as sobras que voltaram.
     */
    public static EstoquePerdasResponseDTO perdas(Dados d, String mes) {
        List<PerdaDTO> perdas = new ArrayList<>();
        for (MovimentoResponseDTO m : d.movimentos) {
            if (!"perda".equals(m.tipo()) || !Formato.texto(m.data()).startsWith(mes)) {
                continue;
            }
            InsumoResponseDTO i = d.insumo(m.insumoId());
            perdas.add(new PerdaDTO(m.data(), i == null ? "Item removido" : i.nome(), Formato.r3(Math.abs(m.qtd())), i == null ? "" : i.unidade(),
                Formato.round2(valorMov(d, m)), Formato.texto(m.motivo()), i != null && Dominio.reutilizavel(i.tipo()) ? GRUPO_AVARIA : GRUPO_PERDA_INSUMO,
                d.nomeEvento(m.eventoId()), m.insumoId(), null));
        }
        for (LoteResponseDTO l : d.lotes) {
            PreparacaoResponseDTO p = d.preparacao(l.preparacaoId());
            for (LoteResponseDTO.Historico h : l.historico()) {
                if (!"descarte".equals(h.tipo()) || !Formato.texto(h.data()).startsWith(mes)) {
                    continue;
                }
                perdas.add(new PerdaDTO(h.data(), (p == null ? "Pré-preparo" : p.nome()) + " · lote " + Formato.texto(l.codigo()), Formato.r3(h.qtd()),
                    Fichas.unidRendLabel(l.unidade(), 2), Formato.round2(custoPorUnidadeDoLote(d, l) * h.qtd()),
                    Dominio.rotulo(Dominio.MOTIVOS_DESCARTE_LOTE, h.para(), ""), GRUPO_DESCARTE_LOTE, d.nomeEvento(l.eventoId()), null, l.id()));
            }
        }
        perdas.sort(Comparator.comparing((PerdaDTO x) -> Formato.texto(x.data())).reversed());

        List<MovimentoResponseDTO> sobras = d.movimentos.stream().filter(m -> "sobra".equals(m.tipo()) && Formato.texto(m.data()).startsWith(mes)).toList();
        double compras = Formato.round2(Formato.soma(d.movimentos.stream().filter(m -> "entrada".equals(m.tipo()) && Formato.texto(m.data()).startsWith(mes))
            .toList(), m -> valorMov(d, m)));
        double total = Formato.round2(Formato.soma(perdas, PerdaDTO::valor));

        Map<String, Double> porGrupo = new LinkedHashMap<>();
        Map<String, Double> porMotivo = new LinkedHashMap<>();
        for (PerdaDTO x : perdas) {
            porGrupo.merge(x.grupo(), x.valor(), Double::sum);
            porMotivo.merge(x.motivo().isEmpty() ? "Sem motivo" : x.motivo(), x.valor(), Double::sum);
        }
        String maiorCausa = null;
        double maior = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> c : porMotivo.entrySet()) {
            if (c.getValue() > maior) {
                maior = c.getValue();
                maiorCausa = c.getKey();
            }
        }
        return new EstoquePerdasResponseDTO(mes, new EstoquePerdasResponseDTO.Kpis(total, perdas.size(), compras, compras > 0 ? total / compras : null,
            Formato.round2(Formato.soma(sobras, m -> valorMov(d, m))), sobras.size(), maiorCausa),
            porGrupo.entrySet().stream().map(g -> new EstoquePerdasResponseDTO.PorGrupo(g.getKey(), Formato.round2(g.getValue()))).toList(), perdas);
    }

    /* -------------------------------- Conferência -------------------------------- */

    /** Rótulo da origem de um movimento, gravado ou calculado (o mesmo da lista de movimentos do frontend). */
    static String origem(MovimentoResponseDTO m) {
        if (Boolean.TRUE.equals(m.virtual())) {
            return "ordem".equals(m.origem()) ? "Ordem de produção" : m.eventoId() != null ? "Consumo em evento (produção)" : "Produção de pré-preparo";
        }
        switch (Formato.texto(m.tipo())) {
            case "consumo":
                return "Consumo · " + Dominio.rotulo(Dominio.UNIDADES_CURTO, m.unidadeUso(), "unidade não informada");
            case "sobra":
                return "Sobra de evento";
            case "devolucao":
                return "Devolução ao fornecedor";
            case "perda":
                if (!"checklist".equals(m.origem())) {
                    return "Perda ou descarte";
                }
                break;
            default:
                break;
        }
        return Dominio.ORIGENS_MOVIMENTO.getOrDefault(Formato.texto(m.origem()), "Origem não informada");
    }

    /**
     * Regras de integridade: toda movimentação tem origem e cada operação baixa o estoque uma vez só.
     * Pontos: lote do estoque usado em evento sem dizer qual (baixa dupla possível), movimentos manuais
     * iguais, nota lançada sem uma entrada por item, saldo negativo e movimento sem origem.
     */
    public static EstoqueConferenciaResponseDTO conferencia(Dados d) {
        List<MovimentoResponseDTO> todos = new ArrayList<>(d.movimentos);
        todos.addAll(Estoque.baixasVirtuais(d));
        Map<String, Integer> porOrigem = new LinkedHashMap<>();
        todos.forEach(m -> porOrigem.merge(origem(m), 1, Integer::sum));

        List<PontoConferenciaDTO> pontos = new ArrayList<>();
        for (LoteResponseDTO l : d.lotes) {
            if (!l.baixaInsumos()) {
                continue;
            }
            PreparacaoResponseDTO p = d.preparacao(l.preparacaoId());
            for (LoteResponseDTO.Historico h : l.historico()) {
                if ("uso".equals(h.tipo()) && "evento".equals(h.para()) && h.eventoId() == null) {
                    pontos.add(new PontoConferenciaDTO("warn", "Lote " + Formato.texto(l.codigo()) + " (" + (p == null ? "" : p.nome()) + ") usado em evento em "
                        + Formato.fdate(h.data()) + " sem dizer qual evento. Se a ordem ou o checklist do evento também registrou a produção, os insumos baixaram duas vezes.",
                        null, l.id(), null));
                }
            }
        }

        Map<String, List<MovimentoResponseDTO>> iguais = new LinkedHashMap<>();
        d.movimentos.stream().filter(m -> "manual".equals(m.origem()))
            .forEach(m -> iguais.computeIfAbsent(m.insumoId() + "|" + m.tipo() + "|" + m.qtd() + "|" + m.data(), k -> new ArrayList<>()).add(m));
        iguais.values().stream().filter(g -> g.size() > 1).forEach(g -> {
            MovimentoResponseDTO m = g.get(0);
            InsumoResponseDTO i = d.insumo(m.insumoId());
            pontos.add(new PontoConferenciaDTO("warn", g.size() + " movimentos iguais de " + (i == null ? "item" : i.nome()) + " ("
                + Dominio.rotulo(Dominio.TIPOS_MOVIMENTO, m.tipo(), "") + ", " + Ordens.qU(Math.abs(m.qtd()), i == null ? "" : i.unidade()) + ", "
                + Formato.fdate(m.data()) + "). Confira se não foi lançado duas vezes.", m.insumoId(), null, null));
        });

        Map<Long, Integer> entradasDaNota = new HashMap<>();
        d.movimentos.stream().filter(m -> m.nfId() != null).forEach(m -> entradasDaNota.merge(m.nfId(), 1, Integer::sum));
        for (NotaFiscalResponseDTO n : d.nfs) {
            if (!"lancada".equals(n.status())) {
                continue;
            }
            int movimentos = entradasDaNota.getOrDefault(n.id(), 0);
            int itens = n.itens() == null ? 0 : n.itens().size();
            if (movimentos != itens) {
                pontos.add(new PontoConferenciaDTO("bad", "NF " + (Formato.vazio(n.numero()) ? "s/n" : n.numero()) + " · " + Formato.texto(n.fornecedor()) + ": "
                    + itens + " itens e " + movimentos + " entradas no estoque.", null, null, n.id()));
            }
        }

        Estoque est = Estoque.calcular(d);
        for (InsumoResponseDTO i : d.insumos) {
            double saldo = est.fisico(i.id());
            if (saldo < 0) {
                pontos.add(new PontoConferenciaDTO("bad", i.nome() + " com saldo negativo (−" + Ordens.qU(-saldo, i.unidade())
                    + "): falta entrada ou a contagem está errada.", i.id(), null, null));
            }
        }

        List<String> comOrigemPeloTipo = List.of("entrada", "perda", "sobra", "devolucao", "ajuste", "inventario", "consumo");
        long semOrigem = d.movimentos.stream().filter(m -> Formato.vazio(m.origem()) && !comOrigemPeloTipo.contains(m.tipo())).count();
        if (semOrigem > 0) {
            pontos.add(new PontoConferenciaDTO("warn", semOrigem + " movimento(s) sem origem identificada.", null, null, null));
        }

        return new EstoqueConferenciaResponseDTO(porOrigem.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(o -> new EstoqueConferenciaResponseDTO.PorOrigem(o.getKey(), o.getValue())).toList(), pontos);
    }
}
