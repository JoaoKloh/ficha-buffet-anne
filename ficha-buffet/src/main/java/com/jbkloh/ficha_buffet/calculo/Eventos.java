package com.jbkloh.ficha_buffet.calculo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jbkloh.ficha_buffet.dtos.resp.CardapioEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CustoPreparacaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.DesvioCustoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EtapaEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FechamentoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.IndicadoresEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LogisticaEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OrcamentoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ProjetoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ResultadoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ValoresPorCustoDTO;

/** Regras dos eventos: convidados, necessidades do cardápio, resultado e travas de exclusão. */
public final class Eventos {

    private Eventos() {}

    public record Convidados(int base, double margem, int n) {}

    /** Insumo consolidado do cardápio: q dos pratos confirmados, qDef dos pratos a definir. */
    public static final class NecessidadeInsumo {
        public final InsumoResponseDTO insumo;
        public double q;
        public double qDef;
        public final List<String> usos = new ArrayList<>();

        NecessidadeInsumo(InsumoResponseDTO insumo) {
            this.insumo = insumo;
        }
    }

    public static final class NecessidadeLouca {
        public final InsumoResponseDTO insumo;
        public double q;
        public final List<String> usos = new ArrayList<>();

        NecessidadeLouca(InsumoResponseDTO insumo) {
            this.insumo = insumo;
        }
    }

    public record NecessidadePrato(ItemCardapioResponseDTO item, PreparacaoResponseDTO preparacao, String status, double total, double lotes,
            double custo, double custoDef) {}

    public record Necessidades(int n, int base, double margem, List<NecessidadeInsumo> insumos, List<NecessidadePrato> pratos, double custo,
            double custoDef, List<String> semFicha, int faltando, List<NecessidadeLouca> loucas) {}

    public static boolean ativo(EventoResponseDTO e) {
        return !"perdido".equals(e.status()) && !"cancelado".equals(e.status());
    }

    /** O cardápio fecha 30 dias antes do evento. */
    public static String prazoCardapio(EventoResponseDTO e) {
        return Formato.vazio(e.dataEvento()) ? null : Formato.addDays(e.dataEvento(), -30);
    }

    /** Convidados para a produção: base (contratados ou previstos) + margem, arredondado para cima. */
    public static Convidados convidados(EventoResponseDTO e) {
        int base = e.contrato() != null && e.contrato().convidadosContratados() > 0 ? e.contrato().convidadosContratados()
            : e.convidadosPrevistos() != null ? e.convidadosPrevistos() : 0;
        return new Convidados(base, e.margem(), (int) Math.ceil(base * (1 + e.margem() / 100) - 1e-9));
    }

    public static String statusItem(ItemCardapioResponseDTO c) {
        return "definir".equals(c.status()) || "nao".equals(c.status()) ? c.status() : "sim";
    }

    /** Tudo que o cardápio de um evento pede: quantidades, insumos consolidados, custo e louças. */
    public static Necessidades necessidades(Dados d, Long eventoId) {
        EventoResponseDTO e = d.evento(eventoId);
        Convidados cv = e == null ? new Convidados(0, 0, 0) : convidados(e);
        Map<Long, NecessidadeInsumo> insumos = new LinkedHashMap<>();
        Map<Long, NecessidadeLouca> loucas = new LinkedHashMap<>();
        List<NecessidadePrato> pratos = new ArrayList<>();
        List<String> semFicha = new ArrayList<>();
        double custo = 0;
        double custoDef = 0;
        int faltando = 0;
        for (ItemCardapioResponseDTO c : d.cardapioDe(eventoId)) {
            PreparacaoResponseDTO p = d.preparacao(c.preparacaoId());
            if (p == null) {
                continue;
            }
            String st = statusItem(c);
            boolean sim = "sim".equals(st);
            double total = c.porcao() * cv.n();
            double lotes = total / Fichas.rend(p.rendimento());
            CustoPreparacaoDTO pc = Fichas.custoPrep(d, p);
            if (!"nao".equals(st)) {
                if (sim) {
                    custo += pc.total() * lotes;
                    faltando += pc.faltando();
                } else {
                    custoDef += pc.total() * lotes;
                }
                Fichas.explode(d, p, lotes, (i, q, via) -> {
                    if (i == null) {
                        return;
                    }
                    NecessidadeInsumo n = insumos.computeIfAbsent(i.id(), k -> new NecessidadeInsumo(i));
                    if (sim) {
                        n.q += q;
                    } else {
                        n.qDef += q;
                    }
                    if (!n.usos.contains(p.nome())) {
                        n.usos.add(p.nome());
                    }
                });
                if (p.itens().isEmpty()) {
                    semFicha.add(p.nome());
                }
                if (sim) {
                    for (PreparacaoResponseDTO.Louca l : p.loucas()) {
                        InsumoResponseDTO i = d.insumo(l.insumoId());
                        if (i == null) {
                            continue;
                        }
                        NecessidadeLouca x = loucas.computeIfAbsent(i.id(), k -> new NecessidadeLouca(i));
                        x.q += l.qtd();
                        x.usos.add(p.nome());
                    }
                }
            }
            pratos.add(new NecessidadePrato(c, p, st, total, lotes, sim ? pc.total() * lotes : 0, "definir".equals(st) ? pc.total() * lotes : 0));
        }
        pratos.sort(Comparator.<NecessidadePrato>comparingInt(x -> Dominio.ordemCategoriaPreparacao(x.preparacao().categoria()))
            .thenComparing(Formato.porNome(x -> x.preparacao().nome())));
        return new Necessidades(cv.n(), cv.base(), cv.margem(), new ArrayList<>(insumos.values()), pratos, custo, custoDef, semFicha, faltando,
            new ArrayList<>(loucas.values()));
    }

    /** Cardápio do evento pronto para a tela, com custo e situação de cada ficha. */
    public static CardapioEventoDTO cardapioDoEvento(Dados d, EventoResponseDTO e) {
        Necessidades nd = necessidades(d, e.id());
        CategoriaResponseDTO ab = Caixa.categoriaAB(d);
        double abLancado = ab == null ? 0 : Formato.soma(lancamentosDoEvento(d, e.id()).stream().filter(l -> ab.id().equals(l.categoriaId())).toList(),
            LancamentoCaixaResponseDTO::valor);
        List<CardapioEventoDTO.Linha> linhas = nd.pratos().stream().map(x -> {
            PreparacaoResponseDTO p = x.preparacao();
            String u = p.unidadeRendimento();
            String aProduzir = "nao".equals(x.status()) ? "fora do cardápio"
                : "a produzir " + Formato.fmtQ(x.total(), "porções".equals(u) ? "un" : u) + " " + Fichas.unidRendLabel(u, x.total());
            return new CardapioEventoDTO.Linha(x.item(), p.nome(), p.categoria(), Dominio.rotulo(Dominio.CATEGORIAS_PREPARACAO, p.categoria(), "Outros"),
                Formato.vazio(x.item().praca()) ? Fichas.pracaDe(p) : x.item().praca(), x.status(), x.total(), aProduzir, Fichas.fatorUnid(p),
                Fichas.fichaStatus(d, p), x.custo(), x.custoDef(), p.porcaoPadrao(),
                Formato.fmtN(p.rendimento()) + " " + Fichas.unidRendLabel(u, p.rendimento()));
        }).toList();
        double valorContrato = e.contrato() == null ? 0 : e.contrato().valorTotal();
        return new CardapioEventoDTO(nd.n(), nd.base(), nd.margem(), e.cardapioFechadoEm() != null, linhas, nd.custo(), nd.custoDef(),
            nd.n() > 0 ? nd.custo() / nd.n() : null, valorContrato > 0 ? nd.custo() / valorContrato : null, abLancado,
            (int) nd.pratos().stream().filter(x -> "definir".equals(x.status())).count(), nd.semFicha(), nd.faltando() > 0);
    }

    public static List<LancamentoCaixaResponseDTO> lancamentosDoEvento(Dados d, Long eventoId) {
        return d.lancamentos.stream().filter(l -> eventoId.equals(l.eventoId()) && !"cancelado".equals(l.status())).toList();
    }

    /* --------------------------- Indicadores (evento como projeto) --------------------------- */

    private static final List<String> TIPOS_CUSTO = List.copyOf(Dominio.TIPOS_CUSTO.keySet());

    /**
     * Indicadores e previsto × realizado do evento (evProjeto do protótipo). Previsto: A&B pelas
     * fichas do cardápio; equipe pelo total da escala ou, sem escala, pelo orçamento do evento;
     * transporte e outros pelo orçamento do evento. Realizado: custos lançados no evento por tipo de categoria, mais a saída líquida do
     * estoque geral (baixas, perdas e sobras pelo custo atual, menos o comprado por nota para o evento)
     * em A&B e as avarias de louça e utensílios em outros.
     */
    public static IndicadoresEventoDTO indicadores(Dados d, EventoResponseDTO e, CardapioEventoDTO cardapio, double totalEscala,
            OrcamentoEventoDTO orcamento) {
        Long id = e.id();
        double contratado = 0;
        double recebido = 0;
        double aPagar = 0;
        int contasAPagar = 0;
        Map<String, Double> real = new LinkedHashMap<>();
        TIPOS_CUSTO.forEach(t -> real.put(t, 0.0));
        for (LancamentoCaixaResponseDTO l : lancamentosDoEvento(d, id)) {
            if (Caixa.transferencia(l)) {
                continue;
            }
            String grupo = Caixa.grupoDe(d, l);
            if ("receita".equals(grupo)) {
                contratado += l.valor();
                recebido += Caixa.pago(l);
            } else if (!"investimento".equals(grupo)) {
                real.merge(Caixa.tipoCusto(d.categoria(l.categoriaId())), l.valor(), Double::sum);
                if ("previsto".equals(l.status())) {
                    aPagar += Caixa.saldo(l);
                    contasAPagar++;
                }
            }
        }
        contratado = Formato.round2(contratado);
        recebido = Formato.round2(recebido);

        double consumo = 0;
        double comprado = 0;
        double avarias = 0;
        List<MovimentoResponseDTO> movimentos = new ArrayList<>(d.movimentos);
        movimentos.addAll(Estoque.baixasVirtuais(d));
        for (MovimentoResponseDTO m : movimentos) {
            InsumoResponseDTO i = id.equals(m.eventoId()) ? d.insumo(m.insumoId()) : null;
            if (i == null) {
                continue;
            }
            if ("entrada".equals(m.tipo()) && "nf".equals(m.origem())) {
                comprado += m.qtd() * (m.custoUnit() != null && m.custoUnit() != 0 ? m.custoUnit() : i.custo());
            } else if (List.of("baixa", "perda", "sobra").contains(m.tipo())) {
                if (Dominio.reutilizavel(i.tipo())) {
                    if ("perda".equals(m.tipo())) {
                        avarias += -m.qtd() * i.custo();
                    }
                } else {
                    consumo += -m.qtd() * i.custo();
                }
            }
        }
        double estoqueGeral = Math.max(0, Formato.round2(consumo - comprado));
        avarias = Math.max(0, Formato.round2(avarias));

        ValoresPorCustoDTO realizado = new ValoresPorCustoDTO(Formato.round2(real.get("ab") + estoqueGeral), Formato.round2(real.get("equipe")),
            Formato.round2(real.get("transporte")), Formato.round2(real.get("outros") + avarias));
        OrcamentoEventoDTO orc = orcamento == null ? OrcamentoEventoDTO.VAZIO : orcamento;
        double equipePrevista = totalEscala != 0 ? totalEscala : orc.equipe() == null ? 0 : orc.equipe();
        ValoresPorCustoDTO previsto = new ValoresPorCustoDTO(Formato.round2(cardapio.custo()), Formato.round2(equipePrevista),
            Formato.round2(orc.transporte()), Formato.round2(orc.outros()));
        double totalPrevisto = Formato.round2(previsto.total());
        double totalRealizado = Formato.round2(realizado.total());
        EventoResponseDTO.Contrato k = e.contrato();
        int convidados = k != null && k.convidadosContratados() > 0 ? k.convidadosContratados()
            : e.convidadosPrevistos() == null ? 0 : e.convidadosPrevistos();
        double receitaPrevista = contratado != 0 ? contratado : k != null ? k.valorTotal() : 0;
        return new IndicadoresEventoDTO(contratado, receitaPrevista, k != null && contratado > k.valorTotal() + 0.005, recebido,
            Formato.round2(contratado - recebido), previsto, realizado, totalPrevisto, totalRealizado, convidados, estoqueGeral, avarias,
            Formato.round2(aPagar), contasAPagar, Formato.round2(receitaPrevista - totalPrevisto), Formato.round2(contratado - totalRealizado),
            receitaPrevista != 0 ? previsto.ab() / receitaPrevista : null, contratado != 0 ? realizado.ab() / contratado : null);
    }

    /** Tipos de custo com realizado mais de 10% acima do previsto (ou realizado sem previsto). */
    public static List<DesvioCustoDTO> desvios(IndicadoresEventoDTO ind) {
        List<DesvioCustoDTO> out = new ArrayList<>();
        for (String t : TIPOS_CUSTO) {
            double p = ind.previsto().de(t);
            double r = ind.realizado().de(t);
            if (r > 0 && (p <= 0 || (r - p) / p > 0.1)) {
                out.add(new DesvioCustoDTO(t, p, r, p > 0 ? (r - p) / p : null));
            }
        }
        return out;
    }

    /* ------------------------------ Fechamento ------------------------------ */

    /** Etapas ainda sem marcar nem dispensar, fora o próprio fechamento (na ordem do processo). */
    public static List<String> etapasPendentes(List<EtapaEventoDTO> etapas) {
        return etapas.stream().filter(x -> !x.feito() && !x.dispensada() && !"fechamento".equals(x.etapa())).map(EtapaEventoDTO::etapa).toList();
    }

    /**
     * O que conferir antes de fechar o evento (fecharEventoSheet do protótipo): etapa do evento,
     * saldo a receber, contas a pagar, A&B sem lançamento, etapas sem marcar e desvios de custo.
     * Não impede o fechamento ("Fechar mesmo assim").
     */
    public static List<String> pendenciasFechamento(EventoResponseDTO e, IndicadoresEventoDTO ind, List<DesvioCustoDTO> desvios,
            List<EtapaEventoDTO> etapas) {
        List<String> out = new ArrayList<>();
        if (Dominio.rank(e.status()) < Dominio.rank("realizado")) {
            out.add("O evento ainda não está como Realizado.");
        }
        if (ind.aReceber() > 0.005) {
            out.add("Falta receber " + Formato.brl(ind.aReceber()) + " do cliente.");
        }
        if (ind.contasAPagar() > 0) {
            String s = ind.contasAPagar() > 1 ? "s" : "";
            out.add(ind.contasAPagar() + " conta" + s + " do evento ainda a pagar (" + Formato.brl(ind.aPagar()) + ").");
        }
        if (ind.realizado().ab() == 0 && ind.previsto().ab() != 0) {
            out.add("Nenhum custo de A&B lançado no evento: o resultado está otimista.");
        }
        List<String> pendentes = etapasPendentes(etapas);
        if (!pendentes.isEmpty()) {
            out.add("Etapas sem marcar: " + String.join(", ", pendentes.stream().map(k -> Dominio.rotulo(Dominio.ETAPAS_EVENTO, k, k)).toList()) + ".");
        }
        desvios.forEach(x -> out.add(Dominio.rotulo(Dominio.TIPOS_CUSTO, x.tipo(), x.tipo()) + ": realizado " + Formato.brl(x.realizado()) + " contra "
            + Formato.brl(x.previsto()) + " previsto."));
        return out;
    }

    /** Retrato do resultado no momento do fechamento: em e por vêm de quem fechou; CMV realizado com 3 casas. */
    public static FechamentoEventoDTO retratoFechamento(IndicadoresEventoDTO ind, List<EtapaEventoDTO> etapas, String em, String por) {
        return new FechamentoEventoDTO(em, por, ind.contratado(), ind.recebido(), ind.totalPrevisto(), ind.totalRealizado(), ind.resultadoRealizado(),
            ind.cmvRealizado() == null ? null : Formato.r3(ind.cmvRealizado()), new FechamentoEventoDTO.PorTipo(ind.previsto(), ind.realizado()),
            etapasPendentes(etapas));
    }

    /** Assinaturas do checklist de transporte (execução tr_{id}); cada uma nula enquanto ninguém assinou. */
    public static ProjetoEventoDTO.Assinaturas assinaturas(Dados d, Long eventoId) {
        ExecucaoResponseDTO tr = d.execucao("tr_" + eventoId);
        if (tr == null) {
            return new ProjetoEventoDTO.Assinaturas(null, null, null);
        }
        return new ProjetoEventoDTO.Assinaturas(assinatura(tr.cargaPor(), tr.cargaHora()), assinatura(tr.recebPor(), tr.recebHora()),
            assinatura(tr.retornoPor(), tr.retornoHora()));
    }

    private static ProjetoEventoDTO.Assinatura assinatura(String por, String hora) {
        return Formato.vazio(por) ? null : new ProjetoEventoDTO.Assinatura(por, Formato.texto(hora));
    }

    public static ResultadoEventoDTO resultado(Dados d, Long eventoId, boolean soRealizado) {
        double rec = 0;
        double desp = 0;
        for (LancamentoCaixaResponseDTO l : lancamentosDoEvento(d, eventoId)) {
            if (soRealizado && !"realizado".equals(l.status())) {
                continue;
            }
            if (Caixa.receita(d, l)) {
                rec += l.valor();
            } else {
                desp += l.valor();
            }
        }
        return new ResultadoEventoDTO(rec, desp, rec - desp, rec != 0 ? (rec - desp) / rec : null);
    }

    /** Motivos que impedem excluir o evento (histórico do caixa e do estoque); vazio se pode excluir. */
    public static String travado(Dados d, EventoResponseDTO e) {
        List<String> m = new ArrayList<>();
        long pg = d.lancamentos.stream().filter(l -> e.id().equals(l.eventoId()) && ("realizado".equals(l.status()) || Caixa.pago(l) > 0)).count();
        if (pg > 0) {
            String s = pg > 1 ? "s" : "";
            m.add(pg + " lançamento" + s + " pago" + s + " ou recebido" + s);
        }
        if (d.execucoes.stream().anyMatch(x -> e.id().equals(x.eventoId()) && produziuOuEnviou(x))) {
            m.add("produção registrada no checklist");
        }
        if (d.execucoesOrdem.values().stream().anyMatch(x -> e.id().equals(x.eventoId()) && "concluida".equals(x.status()) && x.exec().qtd() != null
            && x.exec().qtd() > 0)) {
            m.add("ordens de produção conferidas");
        }
        if (d.lotes.stream().anyMatch(l -> e.id().equals(l.eventoId()) || l.historico().stream().anyMatch(h -> e.id().equals(h.eventoId())))) {
            m.add("lotes de pré-preparo");
        }
        if (d.movimentos.stream().anyMatch(x -> e.id().equals(x.eventoId()))) {
            m.add("movimentos de estoque");
        }
        return String.join(", ", m);
    }

    private static boolean produziuOuEnviou(ExecucaoResponseDTO x) {
        return (x.produzido() != null && x.produzido() > 0) || x.enviado() || (x.enviadoQ() != null && x.enviadoQ() > 0) || x.saiu() != null;
    }

    /* ------------------------------- Etapas ------------------------------- */

    /** Etapa marcada à mão ou dispensada, como está gravada; em = quando mudou (ISO). */
    public record EtapaMarcada(boolean feito, boolean dispensada, String em) {}

    private record Auto(boolean feito, String info) {}

    /**
     * As 14 etapas do evento: cada uma é detectada pelo sistema (auto) ou marcada à mão. Uma etapa
     * crítica ainda pendente perto do evento sai como atrasada. A escala de equipe é detectada por
     * `escalados` (pessoas na escala); a logística, pelo plano com veículo (ou veículo do checklist de
     * transporte) e horário de saída; o fechamento, pela etapa Acertado, com a data do retrato.
     */
    public static List<EtapaEventoDTO> etapas(Dados d, EventoResponseDTO e, CardapioEventoDTO cardapio, Map<String, EtapaMarcada> marcadas, int escalados,
            LogisticaEventoDTO logistica, FechamentoEventoDTO fechamento) {
        Map<String, Auto> auto = etapasAuto(d, e, cardapio, escalados, logistica == null ? LogisticaEventoDTO.VAZIA : logistica, fechamento);
        Long dias = Formato.vazio(e.dataEvento()) ? null : Formato.diffDays(e.dataEvento(), Formato.hoje());
        boolean pendenteDeExecucao = ativo(e) && Dominio.rank(e.status()) < Dominio.rank("realizado");
        List<EtapaEventoDTO> out = new ArrayList<>();
        for (String k : Dominio.ETAPAS_EVENTO.keySet()) {
            Auto a = auto.getOrDefault(k, new Auto(false, ""));
            EtapaMarcada m = marcadas.getOrDefault(k, new EtapaMarcada(false, false, null));
            boolean feito = a.feito() || m.feito();
            Integer critica = Dominio.ETAPAS_CRITICAS.get(k);
            boolean atrasada = !feito && !m.dispensada() && critica != null && dias != null && dias <= critica && pendenteDeExecucao;
            out.add(new EtapaEventoDTO(k, feito, a.feito(), m.feito() && !a.feito(), m.dispensada(), a.info(), m.em(), atrasada, dias));
        }
        return out;
    }

    private static Map<String, Auto> etapasAuto(Dados d, EventoResponseDTO e, CardapioEventoDTO cardapio, int escalados, LogisticaEventoDTO logistica,
            FechamentoEventoDTO fechamento) {
        Long id = e.id();
        ExecucaoResponseDTO tr = d.execucao("tr_" + id);
        int convidados = convidados(e).base();
        long pratos = cardapio.linhas().stream().filter(l -> "sim".equals(l.status())).count();
        long nfsLancadas = d.nfs.stream().filter(n -> id.equals(n.eventoId()) && "lancada".equals(n.status())).count();
        boolean produziu = d.execucoes.stream()
            .anyMatch(x -> id.equals(x.eventoId()) && "item".equals(x.kind()) && x.produzido() != null && x.produzido() > 0);
        String carga = tr == null ? null : tr.cargaPor();
        String retorno = tr == null ? null : tr.retornoPor();

        Map<String, Auto> m = new LinkedHashMap<>();
        m.put("cadastro", new Auto(!Formato.vazio(e.nome()) && e.clienteId() != null, ""));
        boolean temLocal = !Formato.vazio(e.local()) || !Formato.vazio(e.cidade());
        List<String> falta = new ArrayList<>();
        if (Formato.vazio(e.dataEvento())) {
            falta.add("falta data");
        }
        if (!temLocal) {
            falta.add("falta local");
        }
        if (convidados <= 0) {
            falta.add("falta convidados");
        }
        m.put("dados", new Auto(falta.isEmpty(), String.join(", ", falta)));
        m.put("contrato", new Auto(e.contrato() != null && d.parcelas.stream().anyMatch(p -> id.equals(p.eventoId())), e.contrato() != null ? "" : "sem contrato"));
        m.put("cardapio", new Auto(e.cardapioFechadoEm() != null,
            e.cardapioFechadoEm() != null ? "fechado em " + Formato.fdate(e.cardapioFechadoEm()) : pratos > 0 ? pratos + " pratos, ainda aberto" : "nenhum prato"));
        m.put("fichas", new Auto(pratos > 0 && cardapio.semFicha().isEmpty() && !cardapio.faltandoPreco(),
            !cardapio.semFicha().isEmpty() ? cardapio.semFicha().size() + " sem ficha"
                : cardapio.faltandoPreco() ? "insumos sem preço" : pratos > 0 ? "custo " + Formato.brl(cardapio.custo()) : ""));
        m.put("compras", new Auto(false, nfsLancadas > 0 ? nfsLancadas + " nota(s) do evento lançada(s)" : ""));
        m.put("producao", new Auto(produziu, produziu ? "produção registrada na cozinha" : ""));
        m.put("equipe", new Auto(escalados > 0, escalados > 0 ? escalados + (escalados > 1 ? " pessoas" : " pessoa") : "ninguém escalado"));
        String veiculo = !Formato.vazio(logistica.veiculos()) ? logistica.veiculos() : tr == null ? "" : Formato.texto(tr.veiculo());
        List<String> infoLogistica = new ArrayList<>();
        if (!veiculo.isEmpty()) {
            infoLogistica.add(veiculo);
        }
        if (!Formato.vazio(logistica.saida())) {
            infoLogistica.add("saída " + logistica.saida());
        }
        m.put("logistica", new Auto(!veiculo.isEmpty() && !Formato.vazio(logistica.saida()), String.join(" · ", infoLogistica)));
        m.put("carregamento", new Auto(!Formato.vazio(carga), Formato.vazio(carga) ? "" : "carga assinada por " + carga));
        m.put("execucao", new Auto(Dominio.rank(e.status()) >= Dominio.rank("realizado") && ativo(e), ""));
        m.put("conferencia", new Auto(!Formato.vazio(retorno), Formato.vazio(retorno) ? "" : "retorno conferido por " + retorno));
        m.put("fechamento", new Auto("acertado".equals(e.status()),
            fechamento == null || Formato.vazio(fechamento.em()) ? "" : "fechado em " + Formato.fdate(Ordens.diaLocal(fechamento.em()))));
        return m;
    }

    public static EventoListaItemDTO listaItem(Dados d, EventoResponseDTO e) {
        var c = d.cliente(e.clienteId());
        return new EventoListaItemDTO(e, c == null ? null : c.nome());
    }
}
