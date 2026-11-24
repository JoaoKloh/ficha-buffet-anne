package com.jbkloh.ficha_buffet.calculo;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jbkloh.ficha_buffet.dtos.resp.AlertaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PainelResumoResponseDTO.DespesaCategoria;
import com.jbkloh.ficha_buffet.dtos.resp.PainelResumoResponseDTO.Funil;
import com.jbkloh.ficha_buffet.dtos.resp.PainelResumoResponseDTO.Graficos;
import com.jbkloh.ficha_buffet.dtos.resp.PainelResumoResponseDTO.Kpis;
import com.jbkloh.ficha_buffet.dtos.resp.PainelResumoResponseDTO.ResultadoUnidade;
import com.jbkloh.ficha_buffet.dtos.resp.PainelResumoResponseDTO.Unidades;
import com.jbkloh.ficha_buffet.dtos.resp.ParcelaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;

/** Painel da gestão: KPIs do mês, alertas e séries dos gráficos, tudo pronto para a tela. */
public final class Painel {

    private Painel() {}

    private static Map<String, Object> b(Object texto) {
        return Map.of("b", String.valueOf(texto));
    }

    private static String s(long n) {
        return n == 1 ? "" : "s";
    }

    private static String lista(List<String> nomes, int max) {
        return String.join(", ", nomes.subList(0, Math.min(max, nomes.size()))) + (nomes.size() > max ? "…" : "");
    }

    private static Map<String, Object> paraEvento(Long id) {
        return Map.of("tipo", "evento", "id", id);
    }

    private static Map<String, Object> paraTela(String href) {
        return Map.of("tipo", "href", "href", href);
    }

    /** Todos os alertas do Painel, do mais urgente para o menos. */
    public static List<AlertaDTO> alertas(Dados d, String ultimoBackupEm, String t) {
        List<AlertaDTO> out = new ArrayList<>();

        for (EventoResponseDTO e : d.eventos) {
            if (!"contratado".equals(e.status()) || e.cardapioFechadoEm() != null || Formato.vazio(e.dataEvento())) {
                continue;
            }
            String prazo = Eventos.prazoCardapio(e);
            long dd = Formato.diffDays(prazo, t);
            if (dd > 10) {
                continue;
            }
            String txt = dd < 0 ? "prazo do cardápio passou há " + -dd + " dia" + s(-dd) : dd == 0 ? "cardápio vence hoje" : "cardápio vence em " + dd + " dia" + s(dd);
            out.add(new AlertaDTO(dd < 0 ? "bad" : "warn", dd, List.of(b(e.nome()), ": " + txt + " (" + Formato.fdate(prazo) + ")"), paraEvento(e.id())));
        }

        Estoque est = Estoque.calcular(d);
        List<String> baixos = d.insumos.stream()
            .filter(i -> !Dominio.reutilizavel(i.tipo()) && i.estoqueMinimo() > 0 && est.fisico(i.id()) < i.estoqueMinimo()).map(InsumoResponseDTO::nome).toList();
        if (!baixos.isEmpty()) {
            out.add(new AlertaDTO("warn", 5, List.of(b(baixos.size() + (baixos.size() > 1 ? " itens" : " item")), " abaixo do estoque mínimo: " + lista(baixos, 4)),
                paraTela("/estoque")));
        }

        for (ParcelaResponseDTO p : d.parcelas) {
            if (p.pagoEm() != null || p.vencimento().compareTo(t) >= 0) {
                continue;
            }
            EventoResponseDTO e = d.evento(p.eventoId());
            if (e != null && !Eventos.ativo(e)) {
                continue;
            }
            String valor = p.recebido() > 0 ? "falta " + Formato.brl(p.valor() - p.recebido()) + " de " + Formato.brl(p.valor()) : Formato.brl(p.valor());
            out.add(new AlertaDTO("bad", Formato.diffDays(p.vencimento(), t), List.of("Parcela " + p.numero() + " de ", b(e == null ? "evento" : e.nome()),
                " venceu em " + Formato.fdate(p.vencimento()) + " · " + valor), paraEvento(p.eventoId())));
        }

        List<LancamentoCaixaResponseDTO> aPagar = d.lancamentos.stream().filter(l -> !Caixa.transferencia(l) && "previsto".equals(l.status())
            && !Caixa.receita(d, l) && !Caixa.vencimento(d, l).isEmpty()).toList();
        List<LancamentoCaixaResponseDTO> vencidas = aPagar.stream().filter(l -> Caixa.vencimento(d, l).compareTo(t) < 0).toList();
        List<LancamentoCaixaResponseDTO> proximas = aPagar.stream()
            .filter(l -> Caixa.vencimento(d, l).compareTo(t) >= 0 && Caixa.vencimento(d, l).compareTo(Formato.addDays(t, 7)) <= 0)
            .sorted(Comparator.comparing(l -> Caixa.vencimento(d, l))).toList();
        Map<String, Object> contasPagar = paraTela("/caixa?aba=pagar");
        if (!vencidas.isEmpty()) {
            int n = vencidas.size();
            out.add(new AlertaDTO("bad", -1, List.of(b(n + " conta" + s(n) + " a pagar vencida" + s(n)),
                " · " + Formato.brl(Formato.soma(vencidas, Caixa::saldo)) + " · " + lista(vencidas.stream().map(LancamentoCaixaResponseDTO::descricao).toList(), 3)),
                contasPagar));
        }
        if (!proximas.isEmpty()) {
            int n = proximas.size();
            out.add(new AlertaDTO("warn", 2, List.of(b(n + " conta" + s(n) + " a pagar"), " nos próximos 7 dias · " + Formato.brl(Formato.soma(proximas, Caixa::saldo))
                + " · próxima " + Formato.fdate(Caixa.vencimento(d, proximas.get(0)))), contasPagar));
        }
        // Receitas avulsas vencidas (as parcelas de contrato já têm alerta próprio).
        List<LancamentoCaixaResponseDTO> receber = d.lancamentos.stream().filter(l -> !Caixa.transferencia(l) && "previsto".equals(l.status())
            && Caixa.receita(d, l) && l.parcelaId() == null && Caixa.eventoAtivo(d, l) && !Caixa.vencimento(d, l).isEmpty()
            && Caixa.vencimento(d, l).compareTo(t) < 0).toList();
        if (!receber.isEmpty()) {
            int n = receber.size();
            out.add(new AlertaDTO("bad", 0, List.of(b(n + " conta" + s(n) + " a receber vencida" + s(n)),
                " · " + Formato.brl(Formato.soma(receber, Caixa::saldo)) + " · " + lista(receber.stream().map(LancamentoCaixaResponseDTO::descricao).toList(), 3)),
                paraTela("/caixa?aba=receber")));
        }
        int semUnidade = Caixa.semUnidade(d).size();
        if (semUnidade > 0) {
            out.add(new AlertaDTO("info", 60, List.of(b(semUnidade + " lançamento" + s(semUnidade) + " sem unidade de negócio"),
                ": classifique para ver o resultado do Buffet, do Restaurante e da Chocolates"), paraTela("/caixa?aba=lanc&unidade=_sem")));
        }

        for (EventoResponseDTO e : d.eventos) {
            if (Formato.vazio(e.dataEvento())) {
                continue;
            }
            if ("realizado".equals(e.status()) && Formato.diffDays(t, e.dataEvento()) >= 7) {
                out.add(new AlertaDTO("info", 50, List.of(b(e.nome()), " foi realizado em " + Formato.fdate(e.dataEvento()) + " e ainda não foi acertado"),
                    paraEvento(e.id())));
            }
        }
        for (EventoResponseDTO e : d.eventos) {
            if (!Formato.vazio(e.dataEvento()) && Eventos.ativo(e) && Dominio.rank(e.status()) < Dominio.rank("realizado") && e.dataEvento().compareTo(t) < 0) {
                out.add(new AlertaDTO("warn", 40, List.of(b(e.nome()), " já passou da data (" + Formato.fdate(e.dataEvento()) + ") e está como "
                    + Dominio.rotulo(Dominio.STATUS_EVENTO, e.status(), "—")), paraEvento(e.id())));
            }
        }

        out.addAll(alertasCozinha(d, t));
        out.addAll(alertasLotes(d, t));
        out.addAll(alertaBackup(d, ultimoBackupEm, t));
        out.sort(Comparator.comparingLong(AlertaDTO::ord));
        return out;
    }

    /** Alertas da cozinha: tarefas atrasadas, temperatura fora da faixa e louças faltando no retorno. */
    static List<AlertaDTO> alertasCozinha(Dados d, String t) {
        List<AlertaDTO> out = new ArrayList<>();
        for (CozinhaProjecaoDTO pj : Cozinha.projecoes(d, t)) {
            String base = "/cozinha?evento=" + pj.eventoId() + "&aba=";
            long atrasadas = pj.data().compareTo(t) < 0 ? 0
                : Cozinha.tarefas(d, pj).stream().filter(x -> x.data() != null && x.data().compareTo(t) < 0 && !x.feito()).count();
            if (atrasadas > 0) {
                out.add(new AlertaDTO("warn", 3, List.of(b(pj.nome()), ": " + atrasadas + " tarefa" + s(atrasadas) + " de produção atrasada" + s(atrasadas)),
                    paraTela(base + "cronograma")));
            }
            long temperatura = Cozinha.linhas(pj).stream().filter(r -> {
                ExecucaoResponseDTO x = d.execucao(r.id());
                return x != null && Cozinha.fria(r)
                    && (Boolean.TRUE.equals(Cozinha.tempFora(r.cons(), x.tempSaida())) || Boolean.TRUE.equals(Cozinha.tempFora(r.cons(), x.tempChegada())));
            }).count();
            if (temperatura > 0) {
                out.add(new AlertaDTO("bad", 1, List.of(b(pj.nome()), ": " + temperatura + " item(ns) com temperatura fora da faixa no checklist"),
                    paraTela(base + "checklist")));
            }
            long loucas = pj.loucas().stream().filter(l -> Cozinha.loucaFaltando(d.execucao("l_" + pj.eventoId() + "__" + l.id()))).count();
            if (loucas > 0) {
                out.add(new AlertaDTO("bad", 2, List.of(b(pj.nome()), ": " + loucas + " louça(s) faltando no retorno"), paraTela(base + "checklist")));
            }
        }
        return out;
    }

    /** Lotes de pré-preparo vencidos ou vencendo, com saldo. */
    static List<AlertaDTO> alertasLotes(Dados d, String t) {
        List<AlertaDTO> out = new ArrayList<>();
        List<String> vencidos = new ArrayList<>();
        List<String> vencendo = new ArrayList<>();
        for (LoteResponseDTO l : d.lotes) {
            if (!Estoque.loteAtivo(l)) {
                continue;
            }
            PreparacaoResponseDTO p = d.preparacao(l.preparacaoId());
            String nome = p == null ? "lote" : p.nome();
            String situacao = Estoque.loteSituacao(l, t).get(1);
            if ("Vencido".equals(situacao)) {
                vencidos.add(nome);
            } else if ("Vence hoje".equals(situacao) || "Vence amanhã".equals(situacao)) {
                vencendo.add(nome);
            }
        }
        Map<String, Object> destino = paraTela("/estoque");
        if (!vencidos.isEmpty()) {
            int n = vencidos.size();
            out.add(new AlertaDTO("bad", -1, List.of(b(n + " lote" + s(n) + " vencido" + s(n)), " com saldo: " + lista(vencidos, 3) + ". Descarte e registre."), destino));
        }
        if (!vencendo.isEmpty()) {
            int n = vencendo.size();
            out.add(new AlertaDTO("warn", 0, List.of(b(n + " pré-preparo" + s(n)), " vence" + (n > 1 ? "m" : "") + " hoje ou amanhã: " + lista(vencendo, 3)), destino));
        }
        return out;
    }

    /** Backup completo com mais de 7 dias (ou nenhum). Sem eventos cadastrados, não cobra. */
    static List<AlertaDTO> alertaBackup(Dados d, String ultimoEm, String t) {
        if (d.eventos.isEmpty()) {
            return List.of();
        }
        Map<String, Object> destino = Map.of("tipo", "acao", "acao", "backup");
        if (ultimoEm == null) {
            return List.of(new AlertaDTO("warn", 4, List.of(b("Nenhum backup completo registrado."), " Salve o primeiro (.json)."), destino));
        }
        String dia = Instant.parse(ultimoEm).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        long dias = Formato.diffDays(t, dia);
        if (dias < 7) {
            return List.of();
        }
        return List.of(new AlertaDTO("warn", 4, List.of("Último backup completo há ", b(dias + " dias"), " (" + Formato.fdate(dia) + "). Salve um novo."), destino));
    }

    public static Kpis kpis(Dados d, String t) {
        String ym = t.substring(0, 7);
        java.util.function.Predicate<String> doMes = data -> data != null && data.startsWith(ym);
        Map<String, Caixa.DreGrupo> dre = Caixa.dreCalc(d, doMes, true);
        double faturamento = dre.get("receita").total;
        double recebido = Caixa.dreCalc(d, doMes, false, "", "caixa", true).get("receita").total;
        double resultado = Caixa.dreLinhas(dre).fim();
        List<LancamentoCaixaResponseDTO> abertos = d.lancamentos.stream()
            .filter(l -> !Caixa.transferencia(l) && "previsto".equals(l.status()) && Caixa.eventoAtivo(d, l)).toList();
        String em30 = Formato.addDays(t, 30);
        String em7 = Formato.addDays(t, 7);
        List<LancamentoCaixaResponseDTO> receber = abertos.stream().filter(l -> Caixa.receita(d, l) && Caixa.vencimento(d, l).compareTo(em30) <= 0).toList();
        List<LancamentoCaixaResponseDTO> receberVenc = receber.stream().filter(l -> Caixa.vencimento(d, l).compareTo(t) < 0).toList();
        List<LancamentoCaixaResponseDTO> pagar = abertos.stream().filter(l -> !Caixa.receita(d, l) && Caixa.vencimento(d, l).compareTo(em7) <= 0).toList();
        List<LancamentoCaixaResponseDTO> pagarVenc = pagar.stream().filter(l -> Caixa.vencimento(d, l).compareTo(t) < 0).toList();
        return new Kpis(Formato.round2(faturamento), Formato.round2(recebido), Formato.round2(Formato.soma(receber, Caixa::saldo)), receber.size(),
            receberVenc.size(), Formato.round2(Formato.soma(receberVenc, Caixa::saldo)), Formato.round2(Formato.soma(pagar, Caixa::saldo)), pagar.size(),
            pagarVenc.size(), Formato.round2(Formato.soma(pagarVenc, Caixa::saldo)), Formato.round2(resultado), faturamento != 0 ? resultado / faturamento : null);
    }

    /** Resultado do mês de cada unidade operacional, por competência, com previstos e a estrutura compartilhada rateada. */
    public static Unidades unidades(Dados d, String t) {
        String ym = t.substring(0, 7);
        java.util.function.Predicate<String> doMes = data -> data != null && data.startsWith(ym);
        List<ResultadoUnidade> linhas = Dominio.UNIDADES_OPERACIONAIS.stream().map(u -> {
            Map<String, Caixa.DreGrupo> r = Caixa.dreCalc(d, doMes, true, u, "competencia", true);
            Caixa.DreLinhas l = Caixa.dreLinhas(r);
            double cmv = r.get("custo_direto").total;
            return new ResultadoUnidade(u, Dominio.UNIDADES.get(u), Formato.round2(l.bruta()), Formato.round2(cmv), l.bruta() != 0 ? cmv / l.bruta() : null,
                Formato.round2(l.mc()), Formato.round2(l.fim()), l.bruta() != 0 ? l.fim() / l.bruta() : null);
        }).toList();
        int sem = (int) Caixa.semUnidade(d).stream().filter(l -> Caixa.competencia(d, l).startsWith(ym)).count();
        return new Unidades(linhas, sem, Caixa.rateio(d));
    }

    /** Os 6 próximos eventos ativos, com código e cliente. */
    public static List<EventoListaItemDTO> proximosEventos(Dados d, String t) {
        return d.eventos.stream().filter(e -> Eventos.ativo(e) && !Formato.vazio(e.dataEvento()) && e.dataEvento().compareTo(t) >= 0)
            .sorted(Comparator.comparing(EventoResponseDTO::dataEvento)).limit(6).map(e -> Eventos.listaItem(d, e)).toList();
    }

    public static Graficos graficos(Dados d, String t) {
        String inicio = t.substring(0, 7) + "-01";
        List<String> meses = new ArrayList<>();
        List<String> mesesEventos = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            meses.add(Formato.addMonths(inicio, i - 5).substring(0, 7));
            mesesEventos.add(Formato.addMonths(inicio, i).substring(0, 7));
        }
        // Por competência (data do evento nos lançamentos de evento), sem transferências e sem investimentos.
        List<LancamentoCaixaResponseDTO> ls = d.lancamentos.stream().filter(l -> !"cancelado".equals(l.status()) && !Caixa.transferencia(l)
            && !"investimento".equals(Caixa.grupoDe(d, l)) && !Caixa.competencia(d, l).isEmpty()).toList();
        List<Double> receitas = new ArrayList<>();
        List<Double> despesas = new ArrayList<>();
        List<Double> resultado = new ArrayList<>();
        for (String m : meses) {
            double rec = 0;
            double desp = 0;
            for (LancamentoCaixaResponseDTO l : ls) {
                if (Caixa.competencia(d, l).startsWith(m)) {
                    if (Caixa.receita(d, l)) {
                        rec += l.valor();
                    } else {
                        desp += l.valor();
                    }
                }
            }
            receitas.add(Formato.round2(rec));
            despesas.add(Formato.round2(desp));
            resultado.add(Formato.round2(Formato.round2(rec) - Formato.round2(desp)));
        }

        List<EventoResponseDTO> evs = d.eventos.stream().filter(e -> Eventos.ativo(e) && !Formato.vazio(e.dataEvento())).toList();
        List<Integer> eventos = new ArrayList<>();
        List<Integer> convidados = new ArrayList<>();
        for (String m : mesesEventos) {
            List<EventoResponseDTO> doMes = evs.stream().filter(e -> e.dataEvento().startsWith(m)).toList();
            eventos.add(doMes.size());
            convidados.add(doMes.stream().mapToInt(e -> e.convidadosPrevistos() == null ? 0 : e.convidadosPrevistos()).sum());
        }

        // Despesas por categoria dos últimos 4 meses; além de 6 categorias, as menores viram "Outras".
        String de = meses.get(2);
        String ate = meses.get(5);
        Map<String, Double> porCategoria = new LinkedHashMap<>();
        for (LancamentoCaixaResponseDTO l : ls) {
            String ym = Caixa.competencia(d, l).substring(0, 7);
            if (!Caixa.receita(d, l) && ym.compareTo(de) >= 0 && ym.compareTo(ate) <= 0) {
                var c = d.categoria(l.categoriaId());
                porCategoria.merge(c == null ? "Sem categoria" : c.nome(), l.valor(), Double::sum);
            }
        }
        List<DespesaCategoria> dc = new ArrayList<>(porCategoria.entrySet().stream().map(x -> new DespesaCategoria(x.getKey(), x.getValue()))
            .sorted(Comparator.comparingDouble(DespesaCategoria::valor).reversed()).toList());
        if (dc.size() > 6) {
            double outras = Formato.soma(dc.subList(5, dc.size()), DespesaCategoria::valor);
            dc = new ArrayList<>(dc.subList(0, 5));
            dc.add(new DespesaCategoria("Outras", outras));
        }

        List<Funil> funil = Dominio.STATUS_EVENTO.entrySet().stream()
            .map(st -> new Funil(st.getKey(), st.getValue(), (int) d.eventos.stream().filter(e -> st.getKey().equals(e.status())).count()))
            .filter(x -> x.n() > 0).toList();
        return new Graficos(meses, 5, receitas, despesas, resultado, mesesEventos, eventos, convidados, funil, d.eventos.size(),
            (int) d.eventos.stream().filter(e -> Dominio.rank(e.status()) >= Dominio.rank("contratado") && Eventos.ativo(e)).count(),
            dc.stream().map(x -> new DespesaCategoria(x.nome(), Formato.round2(x.valor()))).toList(), de);
    }
}
