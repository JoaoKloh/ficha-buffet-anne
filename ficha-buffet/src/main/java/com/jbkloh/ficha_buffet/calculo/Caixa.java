package com.jbkloh.ficha_buffet.calculo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO.Pagamento;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ParcelaResponseDTO;

/**
 * Regras do caixa e da DRE.
 *
 * Unidades de negócio: cada lançamento pertence ao Buffet, ao Restaurante, à
 * Chocolates ou à Estrutura compartilhada, que é rateada entre as três pelo
 * percentual do rateio. Lançamentos sem unidade gravada contam como Buffet se
 * forem de evento e como Restaurante se forem da conta do restaurante; os
 * demais ficam "sem unidade" até serem classificados.
 *
 * Pagamentos: um lançamento pode ser pago ou recebido em partes; está realizado
 * quando a soma dos pagamentos alcança o valor.
 *
 * Transferências entre unidades (tipo "transferencia") não mexem no caixa nem
 * no consolidado: só tiram custo da unidade de origem e põem na de destino.
 */
public final class Caixa {

    private Caixa() {}

    public static final String SEM_UNIDADE = "_sem";

    /** Soma de um grupo da DRE e de cada categoria dele (id da categoria -> valor). */
    public static final class DreGrupo {
        public double total;
        public final Map<Long, Double> categorias = new LinkedHashMap<>();
    }

    public record DreLinhas(double bruta, double liq, double lb, double mc, double op, double fim) {}

    /** Situação de um lançamento para as listas: tom da pílula e rótulo. */
    public record Situacao(String tom, String rotulo) {}

    /* ------------------------------ Classificação ------------------------------ */

    /** Grupo da DRE de um lançamento: pela categoria; sem categoria, pelo tipo. */
    public static String grupoDe(Dados d, LancamentoCaixaResponseDTO l) {
        CategoriaResponseDTO c = d.categoria(l.categoriaId());
        if (c != null) {
            return c.grupo();
        }
        return "receita".equals(l.tipo()) ? "receita" : "despesa_fixa";
    }

    public static boolean receita(Dados d, LancamentoCaixaResponseDTO l) {
        return "receita".equals(grupoDe(d, l));
    }

    public static boolean transferencia(LancamentoCaixaResponseDTO l) {
        return "transferencia".equals(l.tipo());
    }

    /** Unidade do lançamento; "" quando ainda não tem. */
    public static String unidadeDe(LancamentoCaixaResponseDTO l) {
        if (!Formato.vazio(l.unidade())) {
            return l.unidade();
        }
        if (l.eventoId() != null) {
            return "buffet";
        }
        return "banco_restaurante".equals(l.conta()) ? "restaurante" : "";
    }

    /** Lançamentos (não transferências, não cancelados) que ainda não têm unidade. */
    public static List<LancamentoCaixaResponseDTO> semUnidade(Dados d) {
        return d.lancamentos.stream().filter(l -> !transferencia(l) && !"cancelado".equals(l.status()) && unidadeDe(l).isEmpty()).toList();
    }

    /** Percentual (0 a 100) de cada unidade operacional no rateio; sem rateio gravado, partes iguais. */
    public static Map<String, Double> rateio(Dados d) {
        Map<String, Double> o = new LinkedHashMap<>();
        double t = 0;
        for (String u : Dominio.UNIDADES_OPERACIONAIS) {
            double v = Math.max(0, d.rateioGravado.getOrDefault(u, 0.0));
            o.put(u, v);
            t += v;
        }
        double total = t;
        Dominio.UNIDADES_OPERACIONAIS.forEach(u -> o.put(u, total > 0 ? o.get(u) * 100 / total : 100.0 / Dominio.UNIDADES_OPERACIONAIS.size()));
        return o;
    }

    public static boolean rateioDefinido(Dados d) {
        return !d.rateioGravado.isEmpty();
    }

    /**
     * Peso do lançamento na unidade pedida: "" = consolidado (1), "_sem" = só os sem
     * unidade. A estrutura compartilhada entra nas operacionais pelo rateio quando `ratear`.
     * Transferência: +1 no destino, −1 na origem, 0 no consolidado.
     */
    public static double peso(Dados d, LancamentoCaixaResponseDTO l, String u, boolean ratear) {
        if (transferencia(l)) {
            if (Formato.vazio(u) || SEM_UNIDADE.equals(u)) {
                return 0;
            }
            return (u.equals(l.unidadeDestino()) ? 1 : 0) - (u.equals(l.unidadeOrigem()) ? 1 : 0);
        }
        if (Formato.vazio(u)) {
            return 1;
        }
        String lu = unidadeDe(l);
        if (SEM_UNIDADE.equals(u)) {
            return lu.isEmpty() ? 1 : 0;
        }
        if (lu.equals(u)) {
            return 1;
        }
        if (ratear && "compartilhado".equals(lu) && Dominio.UNIDADES_OPERACIONAIS.contains(u)) {
            return rateio(d).get(u) / 100;
        }
        return 0;
    }

    /** O lançamento aparece no filtro de unidade da lista (transferência: na origem ou no destino). */
    public static boolean naUnidade(LancamentoCaixaResponseDTO l, String u) {
        if (Formato.vazio(u)) {
            return true;
        }
        if (transferencia(l)) {
            return u.equals(l.unidadeOrigem()) || u.equals(l.unidadeDestino());
        }
        String lu = unidadeDe(l);
        return SEM_UNIDADE.equals(u) ? lu.isEmpty() : lu.equals(u);
    }

    /* -------------------------------- Pagamentos -------------------------------- */

    /**
     * Pagamentos do lançamento. Um lançamento realizado sem histórico (registro anterior
     * aos pagamentos em partes) conta como pago de uma vez na data de pagamento.
     */
    public static List<Pagamento> pagos(LancamentoCaixaResponseDTO l) {
        if (transferencia(l) || "cancelado".equals(l.status())) {
            return List.of();
        }
        if (l.pagamentos() != null && !l.pagamentos().isEmpty()) {
            return l.pagamentos();
        }
        if ("realizado".equals(l.status())) {
            String data = !Formato.vazio(l.dataPagamento()) ? l.dataPagamento() : l.dataCompetencia();
            return List.of(new Pagamento(null, data, l.valor(), Formato.vazio(l.conta()) ? "banco" : l.conta(), "", null, ""));
        }
        return List.of();
    }

    public static double pago(LancamentoCaixaResponseDTO l) {
        return Formato.round2(Formato.soma(pagos(l), Pagamento::valor));
    }

    /** O que falta pagar ou receber; 0 para realizados, cancelados e transferências. */
    public static double saldo(LancamentoCaixaResponseDTO l) {
        if (transferencia(l) || "realizado".equals(l.status()) || "cancelado".equals(l.status())) {
            return 0;
        }
        return Math.max(0, Formato.round2(l.valor() - pago(l)));
    }

    /** Data do último pagamento ("" se nenhum). */
    public static String ultimoPagamento(LancamentoCaixaResponseDTO l) {
        return pagos(l).stream().map(p -> Formato.texto(p.data())).max(Comparator.naturalOrder()).orElse("");
    }

    /** Vencimento: o do lançamento, o da parcela do contrato ou, sem nenhum, a competência. */
    public static String vencimento(Dados d, LancamentoCaixaResponseDTO l) {
        if (!Formato.vazio(l.vencimento())) {
            return l.vencimento();
        }
        if (l.parcelaId() != null) {
            ParcelaResponseDTO p = d.parcela(l.parcelaId());
            if (p != null && !Formato.vazio(p.vencimento())) {
                return p.vencimento();
            }
        }
        return Formato.texto(l.dataCompetencia());
    }

    public static Situacao situacao(Dados d, LancamentoCaixaResponseDTO l, String hoje) {
        boolean r = receita(d, l);
        if ("realizado".equals(l.status())) {
            return new Situacao("good", r ? "Recebido" : "Pago");
        }
        String v = vencimento(d, l);
        double p = pago(l);
        if (!v.isEmpty() && v.compareTo(hoje) < 0) {
            return new Situacao("bad", p > 0 ? "Parcial · vencido" : "Vencido");
        }
        if (p > 0) {
            return new Situacao("warn", "Parcial");
        }
        return new Situacao("", "Em aberto");
    }

    /** Competência: receitas e custos de evento contam na data do evento; o resto, na competência lançada. */
    public static String competencia(Dados d, LancamentoCaixaResponseDTO l) {
        if (l.eventoId() != null) {
            EventoResponseDTO e = d.evento(l.eventoId());
            if (e != null && !Formato.vazio(e.dataEvento())) {
                return e.dataEvento();
            }
        }
        return Formato.texto(l.dataCompetencia());
    }

    /** O lançamento não é de evento perdido ou cancelado. */
    public static boolean eventoAtivo(Dados d, LancamentoCaixaResponseDTO l) {
        EventoResponseDTO e = l.eventoId() == null ? null : d.evento(l.eventoId());
        return e == null || Eventos.ativo(e);
    }

    /** Com quem é a conta: o cliente (receita de evento) ou o fornecedor. */
    public static String parte(Dados d, LancamentoCaixaResponseDTO l) {
        if (receita(d, l)) {
            EventoResponseDTO e = l.eventoId() == null ? null : d.evento(l.eventoId());
            ClienteResponseDTO c = e == null ? null : d.cliente(e.clienteId());
            return c != null ? c.nome() : e != null ? e.nome() : "";
        }
        return Formato.texto(l.fornecedor());
    }

    /** Saldo de uma conta: o informado + o que entrou − o que saiu depois da data informada. Nulo se não informado. */
    public static Double saldoConta(Dados d, String conta) {
        Dados.SaldoInformado b = d.saldos.get(conta);
        if (b == null || Formato.vazio(b.data())) {
            return null;
        }
        double s = b.valor();
        for (LancamentoCaixaResponseDTO l : d.lancamentos) {
            int sinal = receita(d, l) ? 1 : -1;
            for (Pagamento p : pagos(l)) {
                String c = !Formato.vazio(p.conta()) ? p.conta() : !Formato.vazio(l.conta()) ? l.conta() : "banco";
                if (c.equals(conta) && Formato.texto(p.data()).compareTo(b.data()) > 0) {
                    s += sinal * p.valor();
                }
            }
        }
        return Formato.round2(s);
    }

    /* ------------------------------------ DRE ----------------------------------- */

    /**
     * Soma por grupo e categoria. `filtro` recebe a data que vale no regime: a
     * competência (data do evento ou competência lançada) ou, no regime de caixa,
     * a data de cada pagamento. Na competência, `incluirPrevistos` soma o valor
     * cheio; sem ele, só o que já foi pago. `unidade`: "" consolidado, "_sem", ou a unidade.
     */
    public static Map<String, DreGrupo> dreCalc(Dados d, Predicate<String> filtro, boolean incluirPrevistos, String unidade, String regime, boolean ratear) {
        Map<String, DreGrupo> r = new LinkedHashMap<>();
        Dominio.GRUPOS_DRE.keySet().forEach(g -> r.put(g, new DreGrupo()));
        boolean caixa = "caixa".equals(regime);
        for (LancamentoCaixaResponseDTO l : d.lancamentos) {
            if ("cancelado".equals(l.status())) {
                continue;
            }
            double w = peso(d, l, unidade, ratear);
            if (w == 0) {
                continue;
            }
            DreGrupo g = r.get(grupoDe(d, l));
            if (g == null) {
                continue;
            }
            double v;
            if (transferencia(l)) {
                v = filtro.test(caixa ? Formato.texto(l.dataCompetencia()) : competencia(d, l)) ? l.valor() : 0;
            } else if (caixa) {
                v = Formato.soma(pagos(l).stream().filter(p -> filtro.test(Formato.texto(p.data()))).toList(), Pagamento::valor);
            } else if (filtro.test(competencia(d, l))) {
                v = incluirPrevistos ? l.valor() : pago(l);
            } else {
                v = 0;
            }
            if (v == 0) {
                continue;
            }
            g.total += v * w;
            g.categorias.merge(l.categoriaId(), v * w, Double::sum);
        }
        return r;
    }

    /** DRE consolidada por competência. */
    public static Map<String, DreGrupo> dreCalc(Dados d, Predicate<String> filtro, boolean incluirPrevistos) {
        return dreCalc(d, filtro, incluirPrevistos, "", "competencia", true);
    }

    public static DreLinhas dreLinhas(Map<String, DreGrupo> r) {
        double bruta = r.get("receita").total;
        double liq = bruta - r.get("deducao").total;
        double lb = liq - r.get("custo_direto").total;
        double mc = lb - r.get("despesa_variavel").total;
        double op = mc - r.get("despesa_fixa").total;
        return new DreLinhas(bruta, liq, lb, mc, op, op - r.get("depreciacao_financeiro_ir").total);
    }

    /* --------------------------------- Listas --------------------------------- */

    /** O lançamento com os nomes e valores que as listas mostram. */
    public static LancamentoListaItemDTO listaItem(Dados d, LancamentoCaixaResponseDTO l, String hoje) {
        CategoriaResponseDTO c = d.categoria(l.categoriaId());
        Situacao st = transferencia(l) ? new Situacao("", "Transferência") : situacao(d, l, hoje);
        return new LancamentoListaItemDTO(l, grupoDe(d, l), c == null ? null : c.nome(), d.nomeEvento(l.eventoId()), transferencia(l) ? "" : unidadeDe(l),
            competencia(d, l), vencimento(d, l), pago(l), saldo(l), st.tom(), st.rotulo(), parte(d, l));
    }

    /* --------------------------------- Diversos --------------------------------- */

    /** Divide um total em n parcelas iguais com 2 casas; o resto vai para a última. */
    public static List<Double> dividirParcelas(double total, int n) {
        double base = Math.floor(total / n * 100) / 100;
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(i == n - 1 ? Formato.round2(total - base * (n - 1)) : base);
        }
        return out;
    }

    /** Categorias na ordem da DRE: grupo, ordem e nome. */
    public static List<CategoriaResponseDTO> categoriasOrdenadas(Dados d) {
        List<String> grupos = List.copyOf(Dominio.GRUPOS_DRE.keySet());
        List<CategoriaResponseDTO> out = new ArrayList<>(d.categorias);
        out.sort(Comparator.<CategoriaResponseDTO>comparingInt(c -> grupos.indexOf(c.grupo()))
            .thenComparingInt(CategoriaResponseDTO::ordem).thenComparing(Formato.porNome(CategoriaResponseDTO::nome)));
        return out;
    }

    /** Categoria "Alimentos e bebidas" (custo direto), usada no resumo do cardápio e nas notas fiscais. */
    public static CategoriaResponseDTO categoriaAB(Dados d) {
        return categoriasOrdenadas(d).stream()
            .filter(c -> "custo_direto".equals(c.grupo()) && c.nome().toLowerCase().matches(".*(a&b|aliment).*")).findFirst().orElse(null);
    }

    private static final java.util.regex.Pattern CUSTO_AB = java.util.regex.Pattern.compile("\\ba b\\b|aliment|bebida|insumo|hortifrut|carne|mercearia|cmv");
    private static final java.util.regex.Pattern CUSTO_EQUIPE = java.util.regex.Pattern.compile("mao de obra|equipe|garc|freela|pessoal|salari|diaria|extra de servico|staff");
    private static final java.util.regex.Pattern CUSTO_TRANSPORTE = java.util.regex.Pattern.compile("frete|transport|logistic|combust|veicul|van|carro|pedagio");

    /** Tipo de custo do evento pelo nome da categoria (custoTipoCat do protótipo): ab, equipe, transporte ou outros, nessa ordem de prioridade. */
    public static String tipoCusto(CategoriaResponseDTO c) {
        if (c == null) {
            return "outros";
        }
        String n = Formato.normTxt(c.nome());
        return CUSTO_AB.matcher(n).find() ? "ab" : CUSTO_EQUIPE.matcher(n).find() ? "equipe" : CUSTO_TRANSPORTE.matcher(n).find() ? "transporte" : "outros";
    }

    /** Categoria das diárias da escala: a primeira de custo (fora as de receita) do tipo equipe, como no protótipo. */
    public static CategoriaResponseDTO categoriaEquipe(Dados d) {
        return categoriasOrdenadas(d).stream().filter(c -> !"receita".equals(c.grupo()) && "equipe".equals(tipoCusto(c))).findFirst().orElse(null);
    }

    /** Categoria de receita padrão para o contrato, conforme o tipo do evento. */
    public static Long receitaPadrao(Dados d, String tipoEvento) {
        List<CategoriaResponseDTO> rs = categoriasOrdenadas(d).stream().filter(c -> "receita".equals(c.grupo())).toList();
        boolean corporativo = "corporativo".equals(tipoEvento) || "coffee_break".equals(tipoEvento);
        String alvo = corporativo ? ".*corpor.*" : ".*(social|casament).*";
        return rs.stream().filter(c -> c.nome().toLowerCase().matches(alvo)).findFirst()
            .or(() -> rs.stream().filter(c -> !c.nome().toLowerCase().contains("extra")).findFirst())
            .or(() -> rs.stream().findFirst()).map(CategoriaResponseDTO::id).orElse(null);
    }
}
