package com.jbkloh.ficha_buffet.calculo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO.Componente;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO.Compra;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO.Etapa;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO.Extra;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO.InsumoPrato;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO.Louca;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO.LoucaPrato;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO.Prato;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO.Tarefa;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.TarefaCozinhaDTO;
import com.jbkloh.ficha_buffet.model.json.ComponenteEnvio;
import com.jbkloh.ficha_buffet.model.json.EnvioExtra;
import com.jbkloh.ficha_buffet.model.json.EtapaPreparacao;
import com.jbkloh.ficha_buffet.model.json.PlanoCozinhaItem;

/** Projeção do evento para a cozinha e regras do checklist de envio, sem nenhum valor financeiro. */
public final class Cozinha {

    private Cozinha() {}

    private static final List<String> STATUS_NA_COZINHA = List.of("contratado", "cardapio_fechado", "em_producao", "realizado");

    /** Dias antes do evento em que cada categoria é produzida, quando a ficha não tem etapas. */
    private static final Map<String, Integer> DN_PADRAO = Dominio.DN_CATEGORIA;

    /** Tarefas de todo evento sem plano próprio: chave, descrição, praça, dias antes e produtos. */
    private static final List<Tarefa> TAREFAS_FIXAS = List.of(
        fixa("f_0", "Fechar pratos \"a definir\" e conferir a lista de compras", "Chef", 7, "Lista de compras"),
        fixa("f_1", "Fazer pedidos aos fornecedores", "Chef", 5, "Lista de compras"),
        fixa("f_2", "Receber e conferir mercadoria; dar entrada da NF, etiquetar e armazenar", "Recebimento", 3, "Todos"),
        fixa("f_3", "Separar louças e utensílios do evento", "Chef", 1, "Louças e utensílios"),
        fixa("f_4", "Conferir etiquetas, quantidades e checklist de envio", "Chef", 1, "Todos"),
        fixa("f_5", "Carregar com cadeia fria, transportar e montar a cozinha de apoio", "No evento", 0, "Todos"));

    private static Tarefa fixa(String k, String desc, String praca, int dn, String produtos) {
        return new Tarefa(k, desc, praca, dn, "Tarefa fixa", produtos, "", false);
    }

    private static final Pattern FINANCEIRO = Pattern.compile(
        "R\\$|\\bvalor|\\bsinal\\b|\\bsaldo\\b|pagamento|\\bpag[oa]s?\\b|parcela|\\bpix\\b|desconto|à vista|quitad|contrato assinado|contrato de|assinatura|degusta|abatid|\\btaxa\\b",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Linha do checklist: prato, componente, extra ou louça, com o campo que registra o envio. */
    public record Linha(String tipo, String id, String cons, boolean prod, String campoEnvio) {}

    public static boolean relevante(EventoResponseDTO e, String dia) {
        return Eventos.ativo(e) && STATUS_NA_COZINHA.contains(e.status()) && !Formato.vazio(e.dataEvento())
            && e.dataEvento().compareTo(Formato.addDays(dia, -10)) >= 0;
    }

    /** Observações do evento sem os trechos financeiros. */
    public static String observacoes(EventoResponseDTO e) {
        return Arrays.stream(Formato.texto(e.observacoes()).split("\n"))
            .map(l -> Arrays.stream(l.replaceAll("\\([^)]*R\\$[^)]*\\)", "").split(" · ")).map(String::trim)
                .filter(s -> !s.isEmpty() && !FINANCEIRO.matcher(s).find()).collect(Collectors.joining(" · ")))
            .filter(l -> !l.isEmpty()).collect(Collectors.joining("\n")).trim();
    }

    /** Arredonda quantidade do checklist: kg/L a 0,1; o resto para cima. */
    public static double arredondar(double t, String u) {
        if (!(t > 0)) {
            return 0;
        }
        if ("kg".equals(u) || "L".equals(u) || "l".equals(u)) {
            return Math.max(0.1, Math.round(t * 10) / 10.0);
        }
        return Math.ceil(t - 1e-9);
    }

    private static double quantidadeExtra(EnvioExtra x, int base, int n) {
        double v = x.v() == null ? 0 : x.v();
        if ("fix".equals(x.modo())) {
            return Formato.r3(v);
        }
        if ("pax106".equals(x.modo())) {
            return Formato.r3(Math.ceil(base * 1.06 - 1e-9) * v);
        }
        return Formato.r3(arredondar(v * n, Formato.vazio(x.u()) ? "un" : x.u()));
    }

    private static String ou(String valor, String padrao) {
        return Formato.vazio(valor) ? padrao : valor;
    }

    public static CozinhaProjecaoDTO projecao(Dados d, EventoResponseDTO e, Estoque est) {
        Eventos.Necessidades nd = Eventos.necessidades(d, e.id());
        List<Prato> pratos = new ArrayList<>();
        for (Eventos.NecessidadePrato x : nd.pratos()) {
            pratos.add(prato(d, x));
        }
        List<Compra> compras = nd.insumos().stream().map(x -> {
            InsumoResponseDTO i = x.insumo;
            return new Compra(i.id(), i.nome(), i.unidade(), ou(i.categoria(), "Outros"), Formato.r3(x.q), Formato.r3(x.qDef),
                Formato.r3(Math.max(0, est.fisico(i.id()) - est.reservado(i.id(), e.id()))), Formato.r3(i.fatorCompra() == null ? 0 : i.fatorCompra()),
                Formato.texto(i.unidadeCompra()), String.join(", ", x.usos));
        }).toList();
        List<Louca> loucas = nd.loucas().stream().map(x -> new Louca(x.insumo.id(), x.insumo.nome(), Formato.r3(x.q), String.join(", ", x.usos))).toList();

        List<PlanoCozinhaItem> plano = e.planoCozinha() != null && !e.planoCozinha().isEmpty() ? e.planoCozinha() : null;
        List<Tarefa> tarefas = new ArrayList<>();
        if (plano != null) {
            for (int ix = 0; ix < plano.size(); ix++) {
                PlanoCozinhaItem t = plano.get(ix);
                tarefas.add(new Tarefa("pl_" + ou(t.k(), String.valueOf(ix)), Formato.texto(t.desc()), ou(t.praca(), "Chef"), t.dn() == null ? -1 : t.dn(),
                    "Plano do evento", Formato.texto(t.produtos()), Formato.texto(t.resp()), false));
            }
        } else {
            tarefas.addAll(TAREFAS_FIXAS);
        }
        for (Prato x : pratos) {
            if (!"sim".equals(x.st())) {
                continue;
            }
            if (!x.etapas().isEmpty()) {
                for (int ix = 0; ix < x.etapas().size(); ix++) {
                    Etapa t = x.etapas().get(ix);
                    tarefas.add(new Tarefa("e_" + x.cid() + "_" + ix, t.desc(), ou(t.praca(), x.praca()), t.dn(), x.nome(), x.nome(), x.resp(), false));
                }
            } else if (plano == null) {
                tarefas.add(new Tarefa("p_" + x.cid(), "Produzir " + x.nome(), x.praca(), DN_PADRAO.getOrDefault(x.cat(), 1), x.nome(),
                    x.nome() + " · " + Formato.fmtQ(x.total(), x.uu()) + " " + x.ulabel(), x.resp(), true));
            }
        }
        List<Extra> extras = new ArrayList<>();
        for (EnvioExtra x : e.envioExtras()) {
            if (x != null && !Formato.vazio(x.n())) {
                extras.add(new Extra(ou(x.k(), "x" + extras.size()), ou(x.sec(), "servico"), x.n(), ou(x.u(), "un"), quantidadeExtra(x, nd.base(), nd.n()),
                    Formato.texto(x.obs())));
            }
        }
        return new CozinhaProjecaoDTO(e.id(), e.codigo(), e.nome(), e.dataEvento(), e.horaInicio(), e.local(), e.cidade(), e.status(), nd.n(), nd.base(),
            Formato.r3(nd.margem()), e.cardapioFechadoEm() != null, observacoes(e), e.equipeCozinha(), pratos, compras, loucas, tarefas, extras,
            d.mapas.get(e.id()));
    }

    private static Prato prato(Dados d, Eventos.NecessidadePrato x) {
        PreparacaoResponseDTO p = x.preparacao();
        String u = p.unidadeRendimento();
        String uu = "porções".equals(u) ? "un" : u;
        double rend = Fichas.rend(p.rendimento());

        // Insumos de uma receita, agrupados por insumo e sub-receita por onde vieram.
        Map<String, double[]> qtdPorChave = new LinkedHashMap<>();
        Map<String, InsumoPrato> modelo = new LinkedHashMap<>();
        Fichas.explode(d, p, 1, (i, q, via) -> {
            String chave = ((i == null ? "_" : i.id()) + "_" + via).replaceAll("[^A-Za-z0-9_-]", "_");
            // A referência gravada da execução tem 60 caracteres ("f_" + chave).
            String k = chave.length() > 58 ? chave.substring(0, 58) : chave;
            qtdPorChave.computeIfAbsent(k, c -> new double[1])[0] += q;
            modelo.computeIfAbsent(k, c -> {
                String local = i == null ? "" : i.local();
                String cons = "Refrigerado".equals(local) ? "refrigerado" : "Congelado".equals(local) ? "congelado" : "ambiente";
                return new InsumoPrato(k, i == null ? "Insumo removido" : i.nome(), i == null ? "" : i.unidade(), via, cons, 0, 0);
            });
        });
        List<InsumoPrato> ins = modelo.values().stream().map(m -> {
            double qb = qtdPorChave.get(m.k())[0];
            return new InsumoPrato(m.k(), m.n(), m.u(), m.via(), m.cons(), Formato.r3(qb), Formato.r3(qb * x.lotes()));
        }).toList();

        List<Etapa> etapas = p.etapas().stream()
            .map((EtapaPreparacao t) -> new Etapa(Formato.texto(t.desc()), Formato.texto(t.praca()), t.dn() == null ? -1 : t.dn())).toList();
        List<LoucaPrato> loucas = new ArrayList<>();
        for (PreparacaoResponseDTO.Louca l : p.loucas()) {
            InsumoResponseDTO i = d.insumo(l.insumoId());
            if (i != null) {
                loucas.add(new LoucaPrato(i.nome(), Formato.r3(l.qtd())));
            }
        }
        List<Componente> comp = new ArrayList<>();
        for (ComponenteEnvio k : p.componentes()) {
            if (k != null && !Formato.vazio(k.n())) {
                double q = k.q() == null ? 0 : k.q();
                String uk = ou(k.u(), "kg");
                comp.add(new Componente("c" + comp.size(), k.n(), uk, Formato.r3(q), arredondar(q * x.total(), uk), ou(k.cons(), "ambiente"),
                    Formato.texto(k.obs())));
            }
        }
        return new Prato(x.item().id(), p.nome(), Formato.texto(p.categoria()), ou(x.item().praca(), Fichas.pracaDe(p)), Formato.texto(x.item().responsavel()),
            x.status(), Formato.r3(x.item().porcao()), Fichas.fatorLabel(x.item().porcao(), p), Formato.r3(x.total()), uu, Fichas.unidRendLabel(u, x.total()),
            Formato.r3(rend), Formato.fmtN(rend) + " " + Fichas.unidRendLabel(u, rend), Formato.r3(x.lotes()), ou(p.conservacao(), "refrigerado"),
            Fichas.fichaStatus(d, p), Formato.texto(p.modoPreparo()), Formato.texto(p.obsEnvio()), etapas, ins, loucas, comp, Ordens.dnFicha(p),
            p.compradoPronto());
    }

    /** Eventos que a cozinha acompanha (contratados em diante, até 10 dias depois da data), por data. */
    public static List<CozinhaProjecaoDTO> projecoes(Dados d, String dia) {
        Estoque est = Estoque.calcular(d);
        return d.eventos.stream().filter(e -> relevante(e, dia)).map(e -> projecao(d, e, est)).sorted(Comparator.comparing(CozinhaProjecaoDTO::data)).toList();
    }

    /** Tarefas com os ajustes da cozinha (feito, responsável, dia movido). */
    public static List<TarefaCozinhaDTO> tarefas(Dados d, CozinhaProjecaoDTO pj) {
        return pj.tarefas().stream().map(t -> {
            ExecucaoResponseDTO x = d.execucao("t_" + pj.eventoId() + "__" + t.k());
            boolean movida = x != null && x.dnSet();
            Integer dn = movida ? x.dn() : t.dn() < 0 ? null : Integer.valueOf(t.dn());
            String resp = x != null && !Formato.vazio(x.resp()) ? x.resp() : t.resp();
            return new TarefaCozinhaDTO(new Tarefa(t.k(), t.desc(), t.praca(), t.dn(), t.origem(), t.produtos(), resp, t.padrao()), pj.eventoId(),
                pj.codigo(), pj.nome(), pj.data(), dn, dn == null || Formato.vazio(pj.data()) ? null : Formato.addDays(pj.data(), -dn),
                x != null && x.feito(), movida);
        }).toList();
    }

    /* ---------------------------------- Checklist ---------------------------------- */

    /** Temperatura fora da faixa da conservação; null = não informada. */
    public static Boolean tempFora(String cons, Double t) {
        if (t == null || !Double.isFinite(t)) {
            return null;
        }
        return switch (Formato.texto(cons)) {
            case "refrigerado" -> t > 4 || t < -1;
            case "congelado" -> t > -18;
            case "quente" -> t < 60;
            default -> false;
        };
    }

    public static List<Linha> linhas(CozinhaProjecaoDTO pj) {
        Long ev = pj.eventoId();
        List<Linha> linhas = new ArrayList<>();
        for (Prato p : pj.pratos()) {
            if (!"sim".equals(p.st())) {
                continue;
            }
            linhas.add(new Linha("prato", "i_" + p.cid(), p.conserv(), true, "enviadoQ"));
            if (!p.comp().isEmpty()) {
                p.comp().forEach(k -> linhas.add(new Linha("comp", "c_" + p.cid() + "__" + k.k(), k.cons(), true, "enviadoQ")));
            } else {
                p.ins().forEach(i -> linhas.add(new Linha("comp", "c_" + p.cid() + "__f_" + i.k(), ou(i.cons(), "ambiente"), true, "enviadoQ")));
            }
        }
        pj.extras().forEach(x -> linhas.add(new Linha("extra", "x_" + ev + "__" + x.k(), "ambiente", false, "enviadoQ")));
        pj.loucas().forEach(l -> linhas.add(new Linha("louca", "l_" + ev + "__" + l.id(), "ambiente", false, "saiu")));
        return linhas;
    }

    public static boolean fria(Linha r) {
        return !Formato.vazio(r.cons()) && !"ambiente".equals(r.cons());
    }

    /** Quantidade enviada da linha (nula se ainda não saiu). */
    public static Double enviado(Linha r, ExecucaoResponseDTO x) {
        Double v = "saiu".equals(r.campoEnvio()) ? x.saiu() : x.enviadoQ();
        if (v != null) {
            return v;
        }
        return "prato".equals(r.tipo()) && x.enviado() && x.produzido() != null && x.produzido() != 0 ? x.produzido() : null;
    }

    /** Condições para a linha sair: produção, assinaturas e, na cadeia fria, temperatura de saída na faixa. */
    public static boolean podeSair(Linha r, ExecucaoResponseDTO x) {
        if (!r.prod()) {
            return !Formato.vazio(x.separadoPor());
        }
        return x.produzido() != null && x.produzido() > 0 && !Formato.vazio(x.executadoPor()) && !Formato.vazio(x.separadoPor())
            && (!fria(r) || Boolean.FALSE.equals(tempFora(r.cons(), x.tempSaida())));
    }

    /** Louça ainda não devolvida por completo. */
    public static boolean loucaFaltando(ExecucaoResponseDTO x) {
        if (x == null || x.saiu() == null || x.voltou() == null) {
            return false;
        }
        return x.saiu() - x.voltou() - (x.avaria() == null ? 0 : x.avaria()) > 0;
    }
}
