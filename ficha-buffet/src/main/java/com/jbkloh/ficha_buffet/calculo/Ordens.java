package com.jbkloh.ficha_buffet.calculo;

import java.text.Collator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoOrdemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LiberacaoExpedicaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OrdemProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.model.json.ComponenteEnvio;
import com.jbkloh.ficha_buffet.model.json.EtapaPreparacao;

/**
 * Ordens de produção da cozinha, como no Anne Gestão: geradas na leitura a partir do cardápio
 * confirmado (só pratos "sim") e das fichas técnicas, por setor e etapa. Gravados ficam só o
 * mapa do evento, o plano da gestão (prazo, horário, setor, responsável) e a execução da equipe.
 * Baixa de estoque: só quando a ordem é conferida (concluída), com os insumos diretos da ficha;
 * sub-receitas têm ordem própria. Evento com mapa gerado: o "produzido" do checklist deixa de
 * dar baixa (evita baixa duplicada).
 */
public final class Ordens {

    private Ordens() {}

    /** Plano da gestão para uma ordem (hora "" = sem horário limite). */
    public record Plano(Long eventoId, String k, int dn, String hora, String setor, String resp) {}

    /** O que a equipe registrou numa ordem; resp vazio = vale o responsável do plano ou do cardápio. */
    public record Execucao(Long eventoId, String k, String status, String statusAnterior, String resp, ExecucaoOrdemDTO exec) {}

    /** Liberação para expedição como foi gravada (autorizadoEm nulo = ainda não liberada). */
    public record Liberacao(Long eventoId, Map<String, Boolean> ck, String horaSaida, String autorizadoPor, String autorizadoEm, boolean comPendencias,
            String pendencias) {}

    public static final String EXPEDICAO = "x_exp";

    private static final Pattern MOLHO = Pattern.compile(
        "molho|calda|creme|fondue|fonduta|ragu|ragù|pesto|vinagrete|maionese|redu[cç]|chutney|geleia|compota|caldo",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CHAVE_BAIXA = Pattern.compile("([ms])_(\\d+)");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final Collator COLLATOR = Collator.getInstance(Locale.of("pt", "BR"));

    private static final List<String> PASSOS_PRONTO = List.of("Item comprado pronto do fornecedor: a produção não faz",
        "Retirar do estoque a quantidade desta ordem", "Conferir validade, embalagem e temperatura", "Entregar para finalização e expedição");
    private static final List<String> PASSOS_EXPEDICAO = List.of("Separar todas as preparações do evento",
        "Identificar caixas: evento, prato, quantidade, validade", "Conferir quantidades com o checklist de envio",
        "Medir temperatura dos itens refrigerados e quentes", "Separar utensílios, equipamentos, bandejas e materiais de serviço",
        "Separar bebidas e complementos", "Conferir o veículo e carregar", "Registrar horário de saída na liberação");

    public static String chave(Long eventoId, String k) {
        return eventoId + "|" + k;
    }

    /* ------------------------------ Padrões da ficha ------------------------------ */

    /** Setor da ficha; sem setor, sai da praça e da categoria. */
    public static String setorDe(PreparacaoResponseDTO p) {
        if (p == null) {
            return "quente";
        }
        if (!Formato.vazio(p.setor())) {
            return p.setor();
        }
        String praca = Formato.texto(p.praca());
        switch (praca) {
            case "Confeitaria":
                return "confeitaria";
            case "Frios":
                return "fria";
            case "No evento":
                return "montagem";
            case "Cozinha quente", "Salgados":
                return "quente";
            default:
                break;
        }
        return switch (Formato.texto(p.categoria())) {
            case "canape", "entrada", "ilha" -> "fria";
            case "sobremesa" -> "confeitaria";
            case "bebida" -> "expedicao";
            default -> "quente";
        };
    }

    /** Antecedência padrão: pela praça (Frios D-7, Cozinha quente D-6, Salgados D-5), senão pela categoria. */
    public static int dnPadrao(String categoria, String praca) {
        if (!"base".equals(categoria) && Dominio.DN_PRACA.containsKey(praca)) {
            return Dominio.DN_PRACA.get(praca);
        }
        return Dominio.DN_CATEGORIA.getOrDefault(categoria, 1);
    }

    /** Antecedência efetiva da produção da ficha (a da ficha ou o padrão da praça). */
    public static int dnFicha(PreparacaoResponseDTO p) {
        return p.dnProducao() != null ? p.dnProducao() : dnPadrao(p.categoria(), Fichas.pracaDe(p));
    }

    static boolean ehMolho(PreparacaoResponseDTO p) {
        return p != null && MOLHO.matcher(Formato.texto(p.nome())).find();
    }

    private static String conservacao(PreparacaoResponseDTO p) {
        return Formato.vazio(p.conservacao()) ? "refrigerado" : p.conservacao();
    }

    /* ---------------------------------- Geração ---------------------------------- */

    /** Ordem em montagem (sem a execução). */
    private static final class Base {
        String k;
        Long cid;
        Long prepId;
        String nome;
        String etapa;
        String setor;
        double q;
        String u;
        double rend;
        String rendU;
        double receitas;
        int dn;
        boolean dnPadrao;
        String hora;
        String resp;
        List<String> origem;
        int tempoMin;
        String equip;
        String armaz;
        int valDias;
        List<OrdemProducaoDTO.Insumo> ins = new ArrayList<>();
        List<String> passos = new ArrayList<>();
        List<OrdemProducaoDTO.Componente> comp = new ArrayList<>();
    }

    private static final class Sub {
        final PreparacaoResponseDTO s;
        double q;
        int dn;
        final List<String> usos = new ArrayList<>();

        Sub(PreparacaoResponseDTO s) {
            this.s = s;
        }
    }

    private static Base base(Map<String, Plano> plano, String k, PreparacaoResponseDTO p, String etapa, String setor, double q, double lotes, int dn,
            List<String> origem) {
        Plano pl = plano.get(k);
        String u = p == null || Formato.vazio(p.unidadeRendimento()) ? "porções" : p.unidadeRendimento();
        Base o = new Base();
        o.k = k;
        o.prepId = p == null ? null : p.id();
        o.nome = p == null ? "" : p.nome();
        o.etapa = etapa;
        o.setor = pl != null ? pl.setor() : setor;
        o.q = Formato.r3(q);
        o.u = Fichas.unidRendLabel(u, o.q);
        o.rend = p == null ? 0 : Formato.r3(Fichas.rend(p.rendimento()));
        o.rendU = p == null ? "" : Fichas.unidRendLabel(u, o.rend);
        o.receitas = Formato.r3(lotes);
        o.dn = pl != null ? pl.dn() : dn;
        o.dnPadrao = pl == null;
        o.hora = pl != null ? pl.hora() : "";
        o.resp = pl != null ? pl.resp() : "";
        o.origem = origem;
        o.tempoMin = p != null && p.tempoMin() != null && p.tempoMin() > 0
            ? (int) Math.round(p.tempoMin() * Math.max(1, Math.ceil(lotes - 1e-9))) : 0;
        o.equip = p == null ? "" : Formato.texto(p.equipamentos());
        o.armaz = p == null ? "" : !Formato.vazio(p.armazenamento()) ? p.armazenamento()
            : Dominio.CONSERVACOES.getOrDefault(conservacao(p), "—");
        o.valDias = p == null ? 0 : Estoque.validadeDiasDe(p);
        return o;
    }

    /** Insumos diretos e sub-receitas da ficha para a quantidade de receitas. */
    private static List<OrdemProducaoDTO.Insumo> diretos(Dados d, PreparacaoResponseDTO p, double lotes) {
        List<OrdemProducaoDTO.Insumo> l = new ArrayList<>();
        for (PreparacaoResponseDTO.Item it : p.itens()) {
            double q = lotes * it.qtd() * (it.fc() > 0 ? it.fc() : 1);
            if (it.subId() != null) {
                PreparacaoResponseDTO s = d.preparacao(it.subId());
                if (s != null) {
                    String us = Formato.vazio(s.unidadeRendimento()) ? "porções" : s.unidadeRendimento();
                    l.add(new OrdemProducaoDTO.Insumo(s.nome(), Formato.r3(q), Fichas.unidRendLabel(us, q), true));
                }
            } else {
                InsumoResponseDTO i = d.insumo(it.insumoId());
                if (i != null) {
                    l.add(new OrdemProducaoDTO.Insumo(i.nome(), Math.round(q * 1e4) / 1e4, Formato.texto(i.unidade()), null));
                }
            }
        }
        return l;
    }

    /** Passos de execução: etapas da ficha ou, sem elas, as linhas do modo de preparo (até 20). */
    private static List<String> passos(PreparacaoResponseDTO p) {
        List<String> l = p.etapas().stream().map(EtapaPreparacao::desc).filter(s -> !Formato.vazio(s)).toList();
        if (l.isEmpty() && !Formato.vazio(p.modoPreparo())) {
            l = Arrays.stream(p.modoPreparo().split("\\n+")).map(String::trim).filter(s -> !s.isEmpty()).limit(20).toList();
        }
        return new ArrayList<>(l);
    }

    /** Sub-receitas usadas pelo prato (até 5 níveis), com a quantidade somada e o prazo mais cedo. */
    private static void subReceitas(Dados d, PreparacaoResponseDTO p, double lotes, int dnPai, String usado, int nivel, Map<Long, Sub> subs) {
        if (nivel > 5) {
            return;
        }
        for (PreparacaoResponseDTO.Item it : p.itens()) {
            if (it.subId() == null) {
                continue;
            }
            PreparacaoResponseDTO s = d.preparacao(it.subId());
            if (s == null) {
                continue;
            }
            double q = lotes * it.qtd() * (it.fc() > 0 ? it.fc() : 1);
            Sub m = subs.computeIfAbsent(s.id(), id -> new Sub(s));
            m.q += q;
            if (!m.usos.contains(usado)) {
                m.usos.add(usado);
            }
            m.dn = Math.max(m.dn, Math.max(dnPai + 1, dnFicha(s)));
            subReceitas(d, s, q / Fichas.rend(s.rendimento()), m.dn, usado, nivel + 1, subs);
        }
    }

    /** Ordens do evento sem a execução, na ordem do mapa: prazo mais longo primeiro, depois etapa e nome. */
    static List<Base> gerar(Dados d, Long eventoId) {
        Map<String, Plano> plano = new LinkedHashMap<>();
        d.planosOrdem.values().stream().filter(p -> p.eventoId().equals(eventoId)).forEach(p -> plano.put(p.k(), p));
        Eventos.Necessidades nd = Eventos.necessidades(d, eventoId);
        List<Base> ops = new ArrayList<>();
        Map<Long, Sub> subs = new LinkedHashMap<>();
        for (Eventos.NecessidadePrato x : nd.pratos()) {
            if (!"sim".equals(x.status())) {
                continue;
            }
            PreparacaoResponseDTO p = x.preparacao();
            ItemCardapioResponseDTO c = x.item();
            int dn = dnFicha(p);
            String km = "m_" + c.id();
            Base o = base(plano, km, p, "principal", setorDe(p), x.total(), x.lotes(), dn, List.of(p.nome()));
            o.ins = diretos(d, p, x.lotes());
            o.passos = passos(p);
            for (ComponenteEnvio k : p.componentes()) {
                if (k != null && !Formato.vazio(k.n())) {
                    String uk = Formato.vazio(k.u()) ? "kg" : k.u();
                    o.comp.add(new OrdemProducaoDTO.Componente(k.n(), Cozinha.arredondar((k.q() == null ? 0 : k.q()) * x.total(), uk), uk));
                }
            }
            o.cid = c.id();
            if (p.compradoPronto()) {
                Plano pl = plano.get(km);
                o.nome = "Retirar do estoque · " + p.nome() + " (comprado pronto)";
                o.etapa = "pre";
                o.setor = pl != null ? pl.setor() : "expedicao";
                o.tempoMin = 0;
                o.equip = "";
                o.dn = pl != null ? pl.dn() : 1;
                o.passos = new ArrayList<>(PASSOS_PRONTO);
            }
            if (!Formato.vazio(c.responsavel()) && o.resp.isEmpty()) {
                o.resp = c.responsavel();
            }
            ops.add(o);
            subReceitas(d, p, x.lotes(), dn, p.nome(), 0, subs);
            if (!"bebida".equals(p.categoria())) {
                Base f = base(plano, "f_" + c.id(), p, "montagem", "montagem", x.total(), 0, 0, List.of(p.nome()));
                f.nome = "Finalizar e montar · " + p.nome();
                f.receitas = 0;
                f.tempoMin = 0;
                f.equip = "";
                String separados = o.comp.isEmpty() ? ""
                    : " (componentes separados: " + String.join(", ", o.comp.stream().map(OrdemProducaoDTO.Componente::n).toList()) + ")";
                f.passos = new ArrayList<>(List.of("Conferir a preparação e o lote recebidos da produção",
                    "Finalizar, porcionar e montar conforme a ficha" + separados, "Organizar bandejas e acabamento", "Identificar e entregar à expedição"));
                f.cid = c.id();
                ops.add(f);
            }
        }
        for (Sub m : subs.values()) {
            PreparacaoResponseDTO s = m.s;
            double lotes = m.q / Fichas.rend(s.rendimento());
            Base o = base(plano, "s_" + s.id(), s, ehMolho(s) ? "molho" : "pre", setorDe(s), m.q, lotes, m.dn, List.copyOf(m.usos));
            o.ins = diretos(d, s, lotes);
            o.passos = passos(s);
            ops.add(o);
        }
        Base xp = base(plano, EXPEDICAO, null, "expedicao", "expedicao", 0, 0, 0, List.of("Evento todo"));
        xp.nome = "Separar, identificar e carregar o evento";
        xp.q = 0;
        xp.u = "";
        xp.armaz = "Cadeia fria nos itens refrigerados";
        xp.passos = new ArrayList<>(PASSOS_EXPEDICAO);
        ops.add(xp);
        ops.sort(Comparator.<Base>comparingInt(o -> -o.dn).thenComparingInt(o -> Dominio.ETAPAS_ORDEM.indexOf(o.etapa))
            .thenComparing((a, b) -> COLLATOR.compare(a.nome, b.nome)));
        return ops;
    }

    /** Chaves das ordens que o mapa do evento tem hoje (para validar plano e ações). */
    public static List<String> chaves(Dados d, Long eventoId) {
        return gerar(d, eventoId).stream().map(o -> o.k).toList();
    }

    /* ------------------------------ Ordens com execução ------------------------------ */

    /**
     * Ordens do evento com a execução, a data (evento - dn) e o atraso: a ordem não entregue
     * (nem concluída nem em conferência) está atrasada depois do dia, ou no dia depois do horário limite.
     * Sem mapa gerado, não há ordens.
     */
    public static List<OrdemProducaoDTO> ordens(Dados d, EventoResponseDTO e, String hoje, String agoraHm) {
        if (e == null || !d.mapas.containsKey(e.id())) {
            return List.of();
        }
        String dataEvento = Formato.texto(e.dataEvento());
        List<OrdemProducaoDTO> l = new ArrayList<>();
        for (Base o : gerar(d, e.id())) {
            Execucao x = d.execucoesOrdem.get(chave(e.id(), o.k));
            String st = x == null ? "pendente" : x.status();
            String data = dataEvento.isEmpty() ? null : Formato.addDays(dataEvento, -o.dn);
            boolean entregue = "concluida".equals(st) || "em_conferencia".equals(st);
            boolean atrasada = !entregue && data != null
                && (data.compareTo(hoje) < 0 || (data.equals(hoje) && !o.hora.isEmpty() && o.hora.compareTo(agoraHm) < 0));
            String resp = x != null && !Formato.vazio(x.resp()) ? x.resp() : o.resp;
            ExecucaoOrdemDTO exec = x == null ? ExecucaoOrdemDTO.VAZIA : x.exec();
            OrdemProducaoDTO sem = new OrdemProducaoDTO(o.k, e.id(), e.codigo(), e.nome(), dataEvento, o.cid, o.prepId, o.nome, o.etapa, o.setor,
                o.q, o.u, o.rend, o.rendU, o.receitas, o.dn, o.dnPadrao, data, o.hora, resp, o.origem, o.tempoMin, o.equip, o.armaz, o.valDias,
                List.copyOf(o.ins), List.copyOf(o.passos), List.copyOf(o.comp), st, atrasada, exec, null);
            l.add("concluida".equals(st) && !Formato.vazio(exec.lote()) ? comEtiqueta(sem, etiqueta(sem)) : sem);
        }
        return l;
    }

    private static OrdemProducaoDTO comEtiqueta(OrdemProducaoDTO o, String etiqueta) {
        return new OrdemProducaoDTO(o.k(), o.eventoId(), o.codigo(), o.eventoNome(), o.eventoData(), o.cid(), o.prepId(), o.nome(), o.etapa(), o.setor(),
            o.q(), o.u(), o.rend(), o.rendU(), o.receitas(), o.dn(), o.dnPadrao(), o.data(), o.hora(), o.resp(), o.origem(), o.tempoMin(), o.equip(),
            o.armaz(), o.valDias(), o.ins(), o.passos(), o.comp(), o.status(), o.atrasada(), o.exec(), etiqueta);
    }

    /** Etiqueta de identificação e rastreabilidade da ordem conferida. */
    static String etiqueta(OrdemProducaoDTO o) {
        ExecucaoOrdemDTO x = o.exec();
        String validade = Formato.vazio(x.conferidoEm()) ? "" : Formato.addDays(diaLocal(x.conferidoEm()), o.valDias());
        return String.join("\n", o.nome(),
            "Evento: " + o.codigo() + " · " + o.eventoNome() + " · " + Formato.fdate(o.eventoData()),
            "Lote: " + ou(x.lote(), "—") + " · Qtd: " + (x.qtd() != null ? qU(x.qtd(), o.u()) : "—"),
            "Produzido: " + diaHora(x.concluidoEm()) + " por " + ou(x.concluidoPor(), "—"),
            "Validade: " + (validade.isEmpty() ? "—" : Formato.fdate(validade)) + " · " + Formato.texto(o.armaz()),
            "Conferido por: " + ou(x.conferidoPor(), "—"));
    }

    private static String ou(String s, String padrao) {
        return Formato.vazio(s) ? padrao : s;
    }

    static String qU(double n, String u) {
        return (Formato.fmtQ(n, u) + " " + Formato.texto(u)).trim();
    }

    /** Dia (AAAA-MM-DD) de um instante ISO, no fuso do servidor. */
    public static String diaLocal(String instante) {
        return Instant.parse(instante).atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    /** "DD/MM HH:MM" de um instante ISO, no fuso do servidor; travessão sem data. */
    static String diaHora(String instante) {
        if (Formato.vazio(instante)) {
            return "—";
        }
        return LocalDateTime.ofInstant(Instant.parse(instante), ZoneId.systemDefault()).format(DIA_HORA);
    }

    /* ------------------------------ Liberação para expedição ------------------------------ */

    /** Ordens de produção propriamente ditas: sem a finalização e a expedição. */
    public static boolean deProducao(OrdemProducaoDTO o) {
        return !"expedicao".equals(o.etapa()) && !"montagem".equals(o.etapa());
    }

    /**
     * Estado da liberação: pronta quando todas as ordens de produção foram concluídas, todas as
     * linhas do checklist de envio podem sair, todas as conferências foram marcadas e há horário de saída.
     */
    public static LiberacaoExpedicaoDTO liberacao(Dados d, CozinhaProjecaoDTO pj, List<OrdemProducaoDTO> os) {
        Liberacao lib = d.liberacoes.get(pj.eventoId());
        Map<String, Boolean> ck = lib == null ? Map.of() : lib.ck();
        String horaSaida = lib == null ? "" : lib.horaSaida();
        List<OrdemProducaoDTO> prod = os.stream().filter(Ordens::deProducao).toList();
        long abertas = prod.stream().filter(o -> !"concluida".equals(o.status())).count();
        List<Cozinha.Linha> linhas = Cozinha.linhas(pj);
        long prontos = linhas.stream().filter(r -> {
            ExecucaoResponseDTO x = d.execucao(r.id());
            return x != null && Cozinha.podeSair(r, x);
        }).count();
        long conferenciasFaltando = Dominio.ITENS_LIBERACAO.keySet().stream().filter(i -> !Boolean.TRUE.equals(ck.get(i))).count();
        boolean ok = abertas == 0 && prontos == linhas.size() && conferenciasFaltando == 0 && !horaSaida.isEmpty();

        List<String> faltam = new ArrayList<>();
        if (abertas > 0) {
            faltam.add(abertas + " ordem(ns) abertas");
        }
        if (prontos < linhas.size()) {
            faltam.add((linhas.size() - prontos) + " item(ns) do checklist");
        }
        if (conferenciasFaltando > 0) {
            faltam.add(conferenciasFaltando + " conferência(s)");
        }
        if (horaSaida.isEmpty()) {
            faltam.add("horário de saída");
        }
        boolean autorizada = lib != null && lib.autorizadoEm() != null;
        List<String> situacao = autorizada ? (lib.comPendencias() ? List.of("warn", "Liberado com pendências") : List.of("good", "Liberado para expedição"))
            : ok ? List.of("accent", "Pronto para liberar") : List.of("bad", "Bloqueado");
        return new LiberacaoExpedicaoDTO(pj.eventoId(), new LinkedHashMap<>(ck), horaSaida, lib == null ? "" : lib.autorizadoPor(),
            lib == null ? null : lib.autorizadoEm(), autorizada && lib.comPendencias(), lib == null ? "" : lib.pendencias(), prod.size(), (int) abertas,
            (int) prontos, linhas.size(), ok, faltam, situacao);
    }

    /* ----------------------------------- Estoque ----------------------------------- */

    /**
     * Baixas calculadas das ordens conferidas (principal e sub-receita) de eventos com mapa:
     * insumos diretos da ficha pela quantidade produzida, sem louças e utensílios.
     */
    public static List<MovimentoResponseDTO> baixas(Dados d) {
        List<MovimentoResponseDTO> v = new ArrayList<>();
        List<Execucao> conferidas = d.execucoesOrdem.values().stream()
            .filter(x -> "concluida".equals(x.status()) && x.exec().qtd() != null && x.exec().qtd() > 0 && d.mapas.containsKey(x.eventoId()))
            .sorted(Comparator.comparing(Execucao::eventoId).thenComparing(Execucao::k)).toList();
        for (Execucao x : conferidas) {
            var m = CHAVE_BAIXA.matcher(x.k());
            if (!m.matches()) {
                continue;
            }
            long id = Long.parseLong(m.group(2));
            PreparacaoResponseDTO p;
            if ("m".equals(m.group(1))) {
                ItemCardapioResponseDTO c = d.itemCardapio(id);
                p = c == null ? null : d.preparacao(c.preparacaoId());
            } else {
                p = d.preparacao(id);
            }
            if (p == null) {
                continue;
            }
            // O que veio de lote do estoque (que já baixou os insumos) não baixa de novo.
            double efetivo = Math.max(0, x.exec().qtd() - Estoque.usoDeLoteNoEvento(d, x.eventoId(), p.id()));
            if (!(efetivo > 0)) {
                continue;
            }
            double lotes = efetivo / Fichas.rend(p.rendimento());
            Map<Long, Double> porInsumo = new LinkedHashMap<>();
            for (PreparacaoResponseDTO.Item it : p.itens()) {
                InsumoResponseDTO i = it.subId() != null ? null : d.insumo(it.insumoId());
                if (i == null || Dominio.reutilizavel(i.tipo())) {
                    continue;
                }
                porInsumo.merge(i.id(), lotes * it.qtd() * (it.fc() > 0 ? it.fc() : 1), Double::sum);
            }
            String conferidoEm = x.exec().conferidoEm();
            String data = Formato.vazio(conferidoEm) ? Formato.hoje() : diaLocal(conferidoEm);
            String motivo = "Ordem de produção · " + p.nome() + (Formato.vazio(x.exec().lote()) ? "" : " · lote " + x.exec().lote());
            porInsumo.forEach((insumoId, q) -> v.add(new MovimentoResponseDTO(0L, "baixa", insumoId, -Formato.r3(q), Formato.r3(q),
                d.insumo(insumoId).unidade(), data, motivo, x.eventoId(), null, "ordem", null, Formato.texto(conferidoEm), null, null, true)));
        }
        return v;
    }
}
