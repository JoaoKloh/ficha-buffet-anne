package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Eventos;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.ClassificarUnidadeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LancamentoCaixaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PagamentoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.RateioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SaldosContasRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.TransferenciaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CaixaMesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ContasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.DreResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FluxoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ResultadoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SemUnidadeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.TransferenciasResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.CategoriaFinanceiraEntity;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.LancamentoPagamentoEntity;
import com.jbkloh.ficha_buffet.model.RegistroSistemaEntity;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;
import com.jbkloh.ficha_buffet.repositories.RegistroSistemaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Caixa e DRE: lançamentos por competência, contas a pagar e a receber,
 * pagamentos em partes, fluxo de caixa, transferências entre unidades,
 * classificação por unidade de negócio, saldos das contas e rateio.
 */
@Service
@RequiredArgsConstructor
public class CaixaService {

    private static final List<String> FILTROS = List.of("todos", "receita", "despesa", "previsto", "realizado");
    private static final List<String> FILTROS_CONTAS = List.of("abertas", "vencidas", "semana", "parciais", "quitadas");
    private static final List<String> UNIDADES_FILTRO = List.of("buffet", "restaurante", "chocolates", "compartilhado", Caixa.SEM_UNIDADE);
    private static final List<String> MESES = List.of("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro",
        "novembro", "dezembro");
    private static final double CENTAVO = 0.005;

    private final LancamentoRepository lancamentoRepository;
    private final RegistroSistemaRepository registroRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    /* ------------------------------ Regras comuns ------------------------------ */

    /** Lançamento previsto, na conta banco, com o tipo que acompanha o grupo da categoria. */
    static LancamentoEntity novoLancamento(CategoriaFinanceiraEntity categoria, EventoEntity evento, String descricao, BigDecimal valor,
            LocalDate dataCompetencia, String origem) {
        LancamentoEntity l = new LancamentoEntity();
        l.setTipo(CategoriaFinanceiraService.tipoDoGrupo(categoria.getGrupo()));
        l.setCategoria(categoria);
        l.setEvento(evento);
        l.setDescricao(descricao(descricao));
        l.setValor(valor);
        l.setDataCompetencia(dataCompetencia);
        l.setStatus("previsto");
        l.setOrigem(origem);
        l.setCriadoPor(Usuario.atual());
        return l;
    }

    /** A coluna da descrição tem 255 caracteres; as descrições montadas com o nome do evento podem passar disso. */
    static String descricao(String descricao) {
        return descricao.length() > 255 ? descricao.substring(0, 255) : descricao;
    }

    static BigDecimal pagoTotal(LancamentoEntity l) {
        return l.getPagamentos().stream().map(LancamentoPagamentoEntity::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Acrescenta um pagamento ao histórico do lançamento (não acerta a situação: veja sincronizar). */
    static LancamentoPagamentoEntity novoPagamento(LancamentoEntity l, LocalDate data, BigDecimal valor, String conta, String forma) {
        LancamentoPagamentoEntity p = new LancamentoPagamentoEntity();
        p.setLancamento(l);
        p.setData(data);
        p.setValor(valor.setScale(2, RoundingMode.HALF_UP));
        p.setConta(Formato.vazio(conta) ? (Formato.vazio(l.getConta()) ? "banco" : l.getConta()) : conta);
        p.setForma(Formato.texto(forma));
        p.setRegistradoEm(Instant.now());
        p.setRegistradoPor(Usuario.atual());
        l.getPagamentos().add(p);
        return p;
    }

    /**
     * Situação pelo que foi pago: quitado = realizado, com a data do último pagamento;
     * senão previsto. A parcela do contrato ligada acompanha.
     */
    static void sincronizar(LancamentoEntity l) {
        boolean quitado = !l.getPagamentos().isEmpty() && pagoTotal(l).doubleValue() >= l.getValor().doubleValue() - CENTAVO;
        if (quitado) {
            LancamentoPagamentoEntity ultimo = l.getPagamentos().get(l.getPagamentos().size() - 1);
            l.setStatus("realizado");
            l.setDataPagamento(ultimo.getData());
            l.setConta(ultimo.getConta());
        } else {
            l.setStatus("previsto");
            l.setDataPagamento(null);
        }
        if (l.getParcela() != null) {
            l.getParcela().setPagoEm(quitado ? l.getDataPagamento() : null);
            if (!l.getPagamentos().isEmpty()) {
                l.getParcela().setForma(Formato.texto(l.getPagamentos().get(l.getPagamentos().size() - 1).getForma()));
            } else {
                l.getParcela().setForma("");
            }
        }
    }

    static void auditar(LancamentoEntity l) {
        l.setAlteradoPor(Usuario.atual());
        l.setAlteradoEm(Instant.now());
    }

    private static String unidadeDoFiltro(String u) {
        return u != null && UNIDADES_FILTRO.contains(u) ? u : "";
    }

    private static boolean mesValido(String mes) {
        return mes != null && mes.matches("\\d{4}-(0[1-9]|1[0-2])");
    }

    /* --------------------------------- Abas --------------------------------- */

    /** Aba Lançamentos: KPIs do mês por competência, na unidade pedida, e a lista filtrada. */
    @Transactional(readOnly = true)
    public CaixaMesResponseDTO retornarMes(String mesPedido, String filtroPedido, String unidadePedida) {
        String mes = mesValido(mesPedido) ? mesPedido : Formato.hoje().substring(0, 7);
        String filtro = FILTROS.contains(filtroPedido) ? filtroPedido : "todos";
        String un = unidadeDoFiltro(unidadePedida);
        String hoje = Formato.hoje();
        Dados d = dadosService.carregar();
        List<LancamentoCaixaResponseDTO> doMes = d.lancamentos.stream()
            .filter(l -> Caixa.competencia(d, l).startsWith(mes) && !"cancelado".equals(l.status()) && Caixa.naUnidade(l, un)).toList();
        List<LancamentoCaixaResponseDTO> operacao = doMes.stream().filter(l -> !Caixa.transferencia(l)).toList();
        double receitas = 0;
        double despesas = 0;
        for (LancamentoCaixaResponseDTO l : doMes) {
            String g = Caixa.grupoDe(d, l);
            double w = un.isEmpty() ? 1 : Caixa.peso(d, l, un, false);
            if ("receita".equals(g) && !Caixa.transferencia(l)) {
                receitas += l.valor() * w;
            } else if (!"receita".equals(g) && !"investimento".equals(g)) {
                despesas += l.valor() * w;
            }
        }
        double aReceber = Formato.soma(operacao.stream().filter(l -> Caixa.receita(d, l)).toList(), Caixa::saldo);
        double aPagar = Formato.soma(operacao.stream().filter(l -> !Caixa.receita(d, l)).toList(), Caixa::saldo);
        List<LancamentoCaixaResponseDTO> lista = doMes.stream().filter(l -> switch (filtro) {
            case "receita" -> Caixa.receita(d, l);
            case "despesa" -> !Caixa.receita(d, l);
            case "previsto", "realizado" -> filtro.equals(l.status());
            default -> true;
        }).sorted(Comparator.<LancamentoCaixaResponseDTO, String>comparing(l -> Caixa.competencia(d, l)).reversed()
            .thenComparing(Formato.porNome(LancamentoCaixaResponseDTO::descricao))).toList();
        double r = Formato.round2(receitas);
        double dsp = Formato.round2(despesas);
        return new CaixaMesResponseDTO(mes, un,
            new CaixaMesResponseDTO.Kpis(r, dsp, Formato.round2(aReceber), Formato.round2(aPagar), Formato.round2(r - dsp), r != 0 ? (r - dsp) / r : null,
                doMes.size()),
            lista.stream().map(l -> Caixa.listaItem(d, l, hoje)).toList(),
            Caixa.categoriasOrdenadas(d).stream()
                .map(c -> new CaixaMesResponseDTO.CategoriaComTotal(c, (int) d.lancamentos.stream().filter(l -> c.id().equals(l.categoriaId())).count())).toList(),
            d.eventos.stream().sorted(Comparator.comparing(EventoResponseDTO::dataEvento).reversed())
                .map(e -> new CaixaMesResponseDTO.EventoRef(e.id(), e.nome(), e.dataEvento())).toList(),
            Caixa.semUnidade(d).size());
    }

    /** Contas a pagar (tipo "pagar") ou a receber ("receber"), pelo vencimento. Receitas de eventos perdidos ou cancelados ficam de fora. */
    @Transactional(readOnly = true)
    public ContasResponseDTO contas(String tipoPedido, String filtroPedido, String unidadePedida) {
        boolean rec = "receber".equals(tipoPedido);
        String filtro = FILTROS_CONTAS.contains(filtroPedido) ? filtroPedido : "abertas";
        String un = unidadeDoFiltro(unidadePedida);
        String t = Formato.hoje();
        String em7 = Formato.addDays(t, 7);
        Dados d = dadosService.carregar();
        List<LancamentoCaixaResponseDTO> base = d.lancamentos.stream().filter(l -> !Caixa.transferencia(l) && !"cancelado".equals(l.status())
            && Caixa.receita(d, l) == rec && Caixa.naUnidade(l, un) && Caixa.eventoAtivo(d, l)).toList();
        List<LancamentoCaixaResponseDTO> ab = base.stream().filter(l -> !"realizado".equals(l.status())).toList();
        List<LancamentoCaixaResponseDTO> vd = ab.stream().filter(l -> !Caixa.vencimento(d, l).isEmpty() && Caixa.vencimento(d, l).compareTo(t) < 0).toList();
        List<LancamentoCaixaResponseDTO> sem = ab.stream().filter(l -> {
            String v = Caixa.vencimento(d, l);
            return v.compareTo(t) >= 0 && v.compareTo(em7) <= 0;
        }).toList();
        List<LancamentoCaixaResponseDTO> par = ab.stream().filter(l -> Caixa.pago(l) > 0).toList();
        String de60 = Formato.addDays(t, -60);
        List<LancamentoCaixaResponseDTO> ls = switch (filtro) {
            case "vencidas" -> vd;
            case "semana" -> sem;
            case "parciais" -> par;
            case "quitadas" -> base.stream().filter(l -> "realizado".equals(l.status()) && Caixa.ultimoPagamento(l).compareTo(de60) >= 0).toList();
            default -> ab;
        };
        Comparator<LancamentoCaixaResponseDTO> ordem = "quitadas".equals(filtro)
            ? Comparator.<LancamentoCaixaResponseDTO, String>comparing(Caixa::ultimoPagamento).reversed()
            : Comparator.comparing(l -> Caixa.vencimento(d, l).isEmpty() ? "9" : Caixa.vencimento(d, l));
        return new ContasResponseDTO(rec ? "receber" : "pagar", filtro, un, total(ab), total(vd), total(sem),
            new ContasResponseDTO.Parcial(Formato.round2(Formato.soma(par, Caixa::pago)), Formato.round2(Formato.soma(par, Caixa::saldo)), par.size()),
            ls.stream().sorted(ordem).map(l -> Caixa.listaItem(d, l, t)).toList(), Caixa.semUnidade(d).size());
    }

    private static ContasResponseDTO.Total total(List<LancamentoCaixaResponseDTO> ls) {
        return new ContasResponseDTO.Total(Formato.round2(Formato.soma(ls, Caixa::saldo)), ls.size());
    }

    /**
     * Fluxo de caixa: o saldo em conta é de todas as unidades (o banco é um só);
     * o filtro de unidade muda só as entradas e saídas previstas.
     */
    @Transactional(readOnly = true)
    public FluxoCaixaResponseDTO fluxo(String unidadePedida) {
        String un = unidadeDoFiltro(unidadePedida);
        String t = Formato.hoje();
        String ym0 = t.substring(0, 7);
        Dados d = dadosService.carregar();
        List<FluxoCaixaResponseDTO.Conta> contas = new ArrayList<>();
        boolean tem = false;
        double tot = 0;
        String saldosData = null;
        for (Map.Entry<String, String> c : Dominio.CONTAS.entrySet()) {
            Double s = Caixa.saldoConta(d, c.getKey());
            Dados.SaldoInformado b = d.saldos.get(c.getKey());
            contas.add(new FluxoCaixaResponseDTO.Conta(c.getKey(), c.getValue(), s, b == null ? null : b.valor(), b == null ? null : b.data()));
            if (s != null) {
                tem = true;
                tot += s;
            }
            if (b != null && saldosData == null) {
                saldosData = b.data();
            }
        }
        tot = Formato.round2(tot);
        List<LancamentoCaixaResponseDTO> ab = d.lancamentos.stream().filter(l -> !Caixa.transferencia(l) && "previsto".equals(l.status())
            && Caixa.naUnidade(l, un) && Caixa.eventoAtivo(d, l)).toList();
        Predicate<LancamentoCaixaResponseDTO> ent = l -> Caixa.receita(d, l);
        List<LancamentoCaixaResponseDTO> e30 = entre(d, ab.stream().filter(ent).toList(), t, Formato.addDays(t, 30));
        List<LancamentoCaixaResponseDTO> s30 = entre(d, ab.stream().filter(ent.negate()).toList(), t, Formato.addDays(t, 30));
        List<LancamentoCaixaResponseDTO> venc = ab.stream().filter(l -> Caixa.vencimento(d, l).compareTo(t) < 0).toList();
        double recMes = 0;
        double pagMes = 0;
        for (LancamentoCaixaResponseDTO l : d.lancamentos) {
            if (!Caixa.naUnidade(l, un)) {
                continue;
            }
            boolean r = ent.test(l);
            for (LancamentoCaixaResponseDTO.Pagamento p : Caixa.pagos(l)) {
                if (Formato.texto(p.data()).startsWith(ym0)) {
                    if (r) {
                        recMes += p.valor();
                    } else {
                        pagMes += p.valor();
                    }
                }
            }
        }
        double entradas30 = Formato.round2(Formato.soma(e30, Caixa::saldo));
        double saidas30 = Formato.round2(Formato.soma(s30, Caixa::saldo));
        double p30 = Formato.round2(tot + entradas30 - saidas30 - Formato.soma(venc.stream().filter(ent.negate()).toList(), Caixa::saldo)
            + Formato.soma(venc.stream().filter(ent).toList(), Caixa::saldo));

        List<FluxoCaixaResponseDTO.Linha> linhas = new ArrayList<>();
        double acc = tot;
        Map<String, List<LancamentoCaixaResponseDTO>> periodos = new LinkedHashMap<>();
        periodos.put("Vencidos em aberto", venc);
        for (int i = 0; i < 6; i++) {
            String m = Formato.addMonths(ym0 + "-01", i).substring(0, 7);
            String ini = i == 0 ? t : m + "-01";
            String fim = Formato.addDays(Formato.addMonths(m + "-01", 1), -1);
            String rot = MESES.get(Integer.parseInt(m.substring(5, 7)) - 1);
            periodos.put(Character.toUpperCase(rot.charAt(0)) + rot.substring(1) + " " + m.substring(0, 4) + (i == 0 ? " (de hoje em diante)" : ""),
                entre(d, ab, ini, fim));
        }
        String depois = Formato.addMonths(ym0 + "-01", 6);
        periodos.put("Depois", ab.stream().filter(l -> Caixa.vencimento(d, l).compareTo(depois) >= 0).toList());
        for (Map.Entry<String, List<LancamentoCaixaResponseDTO>> p : periodos.entrySet()) {
            double e = Formato.round2(Formato.soma(p.getValue().stream().filter(ent).toList(), Caixa::saldo));
            double s = Formato.round2(Formato.soma(p.getValue().stream().filter(ent.negate()).toList(), Caixa::saldo));
            double liq = Formato.round2(e - s);
            acc = Formato.round2(acc + liq);
            linhas.add(new FluxoCaixaResponseDTO.Linha(p.getKey(), e, s, liq, tem ? acc : null, "Vencidos em aberto".equals(p.getKey())));
        }
        return new FluxoCaixaResponseDTO(un, ym0, contas, tem, tot, Formato.round2(recMes), Formato.round2(pagMes), entradas30, saidas30, tem ? p30 : null,
            linhas, saldosData, Caixa.semUnidade(d).size());
    }

    private static List<LancamentoCaixaResponseDTO> entre(Dados d, List<LancamentoCaixaResponseDTO> ls, String ini, String fim) {
        return ls.stream().filter(l -> {
            String v = Caixa.vencimento(d, l);
            return v.compareTo(ini) >= 0 && v.compareTo(fim) <= 0;
        }).toList();
    }

    /** Transferências entre unidades: o que cada unidade recebeu e enviou no mês, e a lista (até 300). */
    @Transactional(readOnly = true)
    public TransferenciasResponseDTO transferencias(String unidadePedida) {
        String un = unidadeDoFiltro(unidadePedida);
        String t = Formato.hoje();
        String ym = t.substring(0, 7);
        Dados d = dadosService.carregar();
        List<LancamentoCaixaResponseDTO> ls = d.lancamentos.stream()
            .filter(l -> Caixa.transferencia(l) && !"cancelado".equals(l.status()) && Caixa.naUnidade(l, un))
            .sorted(Comparator.comparing((LancamentoCaixaResponseDTO l) -> Formato.texto(l.dataCompetencia())).reversed()).toList();
        List<LancamentoCaixaResponseDTO> mes = ls.stream().filter(l -> Formato.texto(l.dataCompetencia()).startsWith(ym)).toList();
        List<TransferenciasResponseDTO.PorUnidade> por = Dominio.UNIDADES_OPERACIONAIS.stream().map(u -> {
            double env = Formato.round2(Formato.soma(mes.stream().filter(l -> u.equals(l.unidadeOrigem())).toList(), LancamentoCaixaResponseDTO::valor));
            double rec = Formato.round2(Formato.soma(mes.stream().filter(l -> u.equals(l.unidadeDestino())).toList(), LancamentoCaixaResponseDTO::valor));
            return new TransferenciasResponseDTO.PorUnidade(u, rec, env, Formato.round2(rec - env));
        }).toList();
        return new TransferenciasResponseDTO(un, ym, por, ls.size(), mes.size(), ls.stream().limit(300).map(l -> Caixa.listaItem(d, l, t)).toList(),
            Caixa.semUnidade(d).size());
    }

    /** Lançamentos ainda sem unidade, dos mais recentes aos mais antigos (até 300). */
    @Transactional(readOnly = true)
    public SemUnidadeResponseDTO semUnidade() {
        String t = Formato.hoje();
        Dados d = dadosService.carregar();
        List<LancamentoCaixaResponseDTO> ls = Caixa.semUnidade(d).stream()
            .sorted(Comparator.<LancamentoCaixaResponseDTO, String>comparing(l -> Caixa.competencia(d, l)).reversed()).toList();
        return new SemUnidadeResponseDTO(ls.size(), ls.stream().limit(300).map(l -> Caixa.listaItem(d, l, t)).toList());
    }

    /* ---------------------------------- DRE ---------------------------------- */

    /**
     * DRE do mês ("01".."12") ou do ano ("ano"), por unidade ("" consolidado, "_sem",
     * uma unidade ou "_cmp" para comparar as unidades), por competência ou por caixa.
     */
    @Transactional(readOnly = true)
    public DreResponseDTO dre(String anoPedido, String periodoPedido, boolean incluirPrevistosPedido, String unidadePedida, String regimePedido,
            boolean ratearPedido) {
        String hoje = Formato.hoje();
        String ano = anoPedido != null && anoPedido.matches("\\d{4}") ? anoPedido : hoje.substring(0, 4);
        String periodo = periodoPedido != null && periodoPedido.matches("ano|0[1-9]|1[0-2]") ? periodoPedido : hoje.substring(5, 7);
        String un = "_cmp".equals(unidadePedida) ? "_cmp" : unidadeDoFiltro(unidadePedida);
        String regime = "caixa".equals(regimePedido) ? "caixa" : "competencia";
        boolean cmp = "_cmp".equals(un);
        boolean incluir = "competencia".equals(regime) && incluirPrevistosPedido;
        boolean ratear = ratearPedido && !"compartilhado".equals(un);
        Dados d = dadosService.carregar();
        String prefixo = "ano".equals(periodo) ? ano : ano + "-" + periodo;
        Predicate<String> filtro = data -> data != null && data.startsWith(prefixo);
        String descricaoRegime = "caixa".equals(regime) ? "por caixa: o que foi efetivamente recebido e pago no período"
            : incluir ? "por competência, realizado + a receber e a pagar" : "por competência, só o que já foi recebido e pago";

        TreeSet<String> anos = new TreeSet<>(Comparator.reverseOrder());
        d.lancamentos.forEach(l -> {
            String c = Caixa.competencia(d, l);
            if (c.length() >= 4) {
                anos.add(c.substring(0, 4));
            }
        });
        anos.add(hoje.substring(0, 4));
        anos.add(ano);
        String titulo = "ano".equals(periodo) ? "Ano de " + ano
            : Character.toUpperCase(MESES.get(Integer.parseInt(periodo) - 1).charAt(0)) + MESES.get(Integer.parseInt(periodo) - 1).substring(1) + " " + ano;
        List<String> invCats = d.categorias.stream().filter(c -> c.nome().toLowerCase().contains("investiment") && !"investimento".equals(c.grupo()))
            .map(CategoriaResponseDTO::nome).toList();
        Map<String, Double> rateio = Caixa.rateio(d);
        int semUnidade = Caixa.semUnidade(d).size();

        if (cmp) {
            List<String> cols = new ArrayList<>(Dominio.UNIDADES_OPERACIONAIS);
            if (!ratearPedido) {
                cols.add("compartilhado");
            }
            List<Map<String, Caixa.DreGrupo>> rs = new ArrayList<>();
            cols.forEach(u -> rs.add(Caixa.dreCalc(d, filtro, incluir, u, regime, ratearPedido)));
            Map<String, Caixa.DreGrupo> semR = Caixa.dreCalc(d, filtro, incluir, Caixa.SEM_UNIDADE, regime, false);
            if (semR.values().stream().anyMatch(g -> Math.abs(g.total) > CENTAVO)) {
                cols.add(Caixa.SEM_UNIDADE);
                rs.add(semR);
            }
            cols.add("_cons");
            rs.add(Caixa.dreCalc(d, filtro, incluir, "", regime, true));
            List<Caixa.DreLinhas> ls = rs.stream().map(Caixa::dreLinhas).toList();
            List<DreResponseDTO.Coluna> colunas = cols.stream().map(u -> new DreResponseDTO.Coluna(u,
                "_cons".equals(u) ? "Consolidado" : Caixa.SEM_UNIDADE.equals(u) ? "Sem unidade" : Dominio.UNIDADES_CURTO.get(u))).toList();
            List<DreResponseDTO.LinhaComparativa> linhas = new ArrayList<>();
            linhas.add(cmpLinha("sum", "Receita bruta", ls, Caixa.DreLinhas::bruta));
            linhas.add(cmpGrupo("(−) Deduções", rs, "deducao"));
            linhas.add(cmpLinha("sum", "Receita líquida", ls, Caixa.DreLinhas::liq));
            linhas.add(cmpGrupo("(−) Custos diretos", rs, "custo_direto"));
            linhas.add(cmpLinha("sum", "Lucro bruto", ls, Caixa.DreLinhas::lb));
            linhas.add(cmpGrupo("(−) Despesas variáveis", rs, "despesa_variavel"));
            linhas.add(cmpLinha("sum", "Margem de contribuição", ls, Caixa.DreLinhas::mc));
            linhas.add(cmpGrupo("(−) Despesas fixas", rs, "despesa_fixa"));
            linhas.add(cmpLinha("sum", "Resultado operacional", ls, Caixa.DreLinhas::op));
            linhas.add(cmpGrupo("(−) Depreciação, financeiro e IR", rs, "depreciacao_financeiro_ir"));
            linhas.add(cmpLinha("final", "Resultado líquido", ls, Caixa.DreLinhas::fim));
            List<Double> margens = ls.stream().map(x -> x.bruta() != 0 ? x.fim() / x.bruta() : null).toList();
            List<Double> inv = rs.stream().map(r -> Formato.round2(r.get("investimento").total)).toList();
            return new DreResponseDTO(ano, periodo, incluir, titulo + " · comparativo das unidades", new ArrayList<>(anos), List.of(), List.of(), un, regime,
                ratearPedido, descricaoRegime, rateio, Caixa.rateioDefinido(d), semUnidade,
                new DreResponseDTO.Comparativo(colunas, linhas, margens, inv.stream().anyMatch(v -> Math.abs(v) > CENTAVO) ? inv : null), invCats,
                ls.get(ls.size() - 1).fim());
        }

        Map<String, Caixa.DreGrupo> r = Caixa.dreCalc(d, filtro, incluir, un, regime, ratear);
        Caixa.DreLinhas totais = Caixa.dreLinhas(r);
        List<DreResponseDTO.Linha> linhas = new ArrayList<>();
        linhas.add(linha("sum", "Receita bruta", totais.bruta(), totais));
        linhas.addAll(categorias(d, r.get("receita"), totais));
        grupo(linhas, d, r, "deducao", "(−) Deduções", totais);
        linhas.add(linha("sum", "Receita líquida", totais.liq(), totais));
        grupo(linhas, d, r, "custo_direto", "(−) Custos diretos", totais);
        linhas.add(linha("sum", "Lucro bruto", totais.lb(), totais));
        grupo(linhas, d, r, "despesa_variavel", "(−) Despesas variáveis", totais);
        linhas.add(linha("sum", "Margem de contribuição", totais.mc(), totais));
        grupo(linhas, d, r, "despesa_fixa", "(−) Despesas fixas", totais);
        linhas.add(linha("sum", "Resultado operacional", totais.op(), totais));
        grupo(linhas, d, r, "depreciacao_financeiro_ir", "(−) Depreciação, financeiro e IR", totais);
        linhas.add(linha("final", "Resultado líquido", totais.fim(), totais));
        if (Math.abs(r.get("investimento").total) > CENTAVO) {
            linhas.add(linha("fora", "Investimentos no período", r.get("investimento").total, totais));
            linhas.addAll(categorias(d, r.get("investimento"), totais));
        }

        List<DreResponseDTO.ResultadoEvento> porEvento = List.of();
        if (un.isEmpty() || "buffet".equals(un)) {
            porEvento = d.eventos.stream().filter(e -> Formato.texto(e.dataEvento()).startsWith(prefixo))
                .sorted(Comparator.comparing(EventoResponseDTO::dataEvento))
                .map(e -> {
                    ResultadoEventoDTO x = Eventos.resultado(d, e.id(), !incluir);
                    return new DreResponseDTO.ResultadoEvento(e.id(), e.nome(), e.dataEvento(), x.rec(), x.desp(), x.res(), x.mg());
                }).filter(x -> x.rec() != 0 || x.desp() != 0).toList();
        }
        String nomeUn = un.isEmpty() ? "Consolidado" : Caixa.SEM_UNIDADE.equals(un) ? "Sem unidade" : Dominio.UNIDADES.get(un);
        return new DreResponseDTO(ano, periodo, incluir, titulo + " · " + nomeUn, new ArrayList<>(anos), linhas, porEvento, un, regime, ratear,
            descricaoRegime, rateio, Caixa.rateioDefinido(d), semUnidade, null, invCats, totais.fim());
    }

    private static DreResponseDTO.LinhaComparativa cmpLinha(String tipo, String rotulo, List<Caixa.DreLinhas> ls,
            java.util.function.ToDoubleFunction<Caixa.DreLinhas> f) {
        return new DreResponseDTO.LinhaComparativa(tipo, rotulo, ls.stream().map(x -> Formato.round2(f.applyAsDouble(x))).toList());
    }

    private static DreResponseDTO.LinhaComparativa cmpGrupo(String rotulo, List<Map<String, Caixa.DreGrupo>> rs, String grupo) {
        return new DreResponseDTO.LinhaComparativa("grupo", rotulo, rs.stream().map(r -> Formato.round2(-r.get(grupo).total)).toList());
    }

    private static DreResponseDTO.Linha linha(String tipo, String rotulo, double valor, Caixa.DreLinhas totais) {
        return new DreResponseDTO.Linha(tipo, rotulo, valor, totais.bruta() != 0 ? valor / totais.bruta() : null);
    }

    private static void grupo(List<DreResponseDTO.Linha> linhas, Dados d, Map<String, Caixa.DreGrupo> r, String grupo, String rotulo, Caixa.DreLinhas totais) {
        linhas.add(linha("grupo", rotulo, -r.get(grupo).total, totais));
        linhas.addAll(categorias(d, r.get(grupo), totais));
    }

    /** Categorias do grupo, da maior para a menor. */
    private static List<DreResponseDTO.Linha> categorias(Dados d, Caixa.DreGrupo g, Caixa.DreLinhas totais) {
        return g.categorias.entrySet().stream().filter(x -> Math.abs(x.getValue()) > CENTAVO)
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()).map(x -> {
                CategoriaResponseDTO c = d.categoria(x.getKey());
                return linha("cat", c == null ? "Categoria removida" : c.nome(), x.getValue(), totais);
            }).toList();
    }

    /* ------------------------------- Lançamentos ------------------------------- */

    @Transactional(readOnly = true)
    public LancamentoListaItemDTO detalharLancamento(Long id) {
        Dados d = dadosService.carregar();
        return d.lancamentos.stream().filter(l -> l.id().equals(id)).findFirst().map(l -> Caixa.listaItem(d, l, Formato.hoje()))
            .orElseThrow(() -> new AppException("O lançamento de ID " + id + " não existe.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public LancamentoCaixaResponseDTO criarLancamento(LancamentoCaixaRequestDTO req) {
        LancamentoEntity lancamento = new LancamentoEntity();
        lancamento.setOrigem("manual");
        lancamento.setCriadoPor(Usuario.atual());
        aplicar(req, lancamento);
        return GestaoMapper.lancamento(lancamentoRepository.save(lancamento));
    }

    @Transactional
    public LancamentoCaixaResponseDTO atualizarLancamento(Long id, LancamentoCaixaRequestDTO req) {
        LancamentoEntity lancamento = buscar(id);
        if ("transferencia".equals(lancamento.getTipo())) {
            throw regra("Transferência se edita em Caixa › Transferências.");
        }
        aplicar(req, lancamento);
        auditar(lancamento);
        return GestaoMapper.lancamento(lancamento);
    }

    /**
     * Pago ou recebido: completa o que falta com um pagamento na data informada.
     * Em aberto: se o que já foi pago cobre o valor, continua quitado.
     */
    private void aplicar(LancamentoCaixaRequestDTO req, LancamentoEntity lancamento) {
        CategoriaFinanceiraEntity categoria = cadastro.categoriaDoCorpo(req.categoriaId(), "Escolha a categoria.");
        LocalDate pagamento = Formato.dataOpcional(req.dataPagamento(), "data de pagamento");
        BigDecimal valor = req.valor().setScale(2, RoundingMode.HALF_UP);
        double ja = pagoTotal(lancamento).doubleValue();
        if (ja > valor.doubleValue() + CENTAVO) {
            throw regra("Já foi " + (CategoriaFinanceiraService.tipoDoGrupo(categoria.getGrupo()).equals("receita") ? "recebido " : "pago ") + Formato.brl(ja)
                + ", mais que o novo valor.");
        }
        lancamento.setTipo(CategoriaFinanceiraService.tipoDoGrupo(categoria.getGrupo()));
        lancamento.setCategoria(categoria);
        lancamento.setEvento(cadastro.eventoDoCorpo(req.eventoId()));
        lancamento.setDescricao(req.descricao().trim());
        lancamento.setValor(valor);
        lancamento.setDataCompetencia(Formato.data(req.dataCompetencia(), "competência"));
        lancamento.setVencimento(Formato.dataOpcional(req.vencimento(), "vencimento"));
        lancamento.setConta(req.conta() == null ? "banco" : req.conta());
        lancamento.setUnidade(req.unidade());
        lancamento.setFornecedor(Formato.vazio(req.fornecedor()) ? null : req.fornecedor().trim());
        if ("realizado".equals(req.status()) && ja < valor.doubleValue() - CENTAVO) {
            novoPagamento(lancamento, pagamento != null ? pagamento : LocalDate.now(), valor.subtract(BigDecimal.valueOf(ja)), lancamento.getConta(),
                lancamento.getFormaPag());
        }
        sincronizar(lancamento);
        if ("realizado".equals(lancamento.getStatus()) && pagamento != null && "realizado".equals(req.status())) {
            lancamento.setDataPagamento(pagamento);
        }
    }

    /** Paga ou recebe o saldo inteiro hoje (atalho das parcelas de nota fiscal). */
    @Transactional
    public void pagarLancamento(Long id) {
        LancamentoEntity l = buscar(id);
        validarPagavel(l);
        double saldo = l.getValor().doubleValue() - pagoTotal(l).doubleValue();
        if (saldo <= CENTAVO) {
            return;
        }
        novoPagamento(l, LocalDate.now(), BigDecimal.valueOf(saldo), l.getConta(), l.getFormaPag());
        sincronizar(l);
        auditar(l);
    }

    /** Registra um pagamento ou recebimento, inteiro ou de uma parte, sem passar do saldo. */
    @Transactional
    public LancamentoCaixaResponseDTO registrarPagamento(Long id, PagamentoRequestDTO req) {
        LancamentoEntity l = buscar(id);
        validarPagavel(l);
        double saldo = Formato.round2(l.getValor().doubleValue() - pagoTotal(l).doubleValue());
        if (saldo <= CENTAVO) {
            throw regra("Este lançamento já está quitado.");
        }
        if (req.valor().doubleValue() > saldo + CENTAVO) {
            throw regra("O valor passa do saldo (" + Formato.brl(saldo) + ").");
        }
        novoPagamento(l, Formato.data(req.data(), "data"), req.valor(), req.conta(), req.forma());
        sincronizar(l);
        auditar(l);
        return GestaoMapper.lancamento(l);
    }

    /** Desfaz o último pagamento: a conta volta a ficar em aberto com o saldo correspondente. */
    @Transactional
    public LancamentoCaixaResponseDTO desfazerPagamento(Long id) {
        LancamentoEntity l = buscar(id);
        if (l.getPagamentos().isEmpty()) {
            throw regra("Não há pagamento para desfazer.");
        }
        l.getPagamentos().remove(l.getPagamentos().size() - 1);
        sincronizar(l);
        auditar(l);
        return GestaoMapper.lancamento(l);
    }

    private static void validarPagavel(LancamentoEntity l) {
        if ("transferencia".equals(l.getTipo())) {
            throw regra("Transferência entre unidades não tem pagamento: ela não mexe no caixa.");
        }
        if ("cancelado".equals(l.getStatus())) {
            throw regra("Lançamento cancelado não recebe pagamento.");
        }
    }

    @Transactional
    public void apagarLancamento(Long id) {
        lancamentoRepository.delete(buscar(id));
    }

    /** Classifica os lançamentos indicados numa unidade. Transferências não têm unidade própria. */
    @Transactional
    public int classificar(ClassificarUnidadeRequestDTO req) {
        List<LancamentoEntity> ls = lancamentoRepository.findAllById(req.ids());
        if (ls.size() != req.ids().stream().distinct().count()) {
            throw new AppException("Algum lançamento indicado não existe mais. Recarregue a página.", HttpStatus.NOT_FOUND);
        }
        for (LancamentoEntity l : ls) {
            if ("transferencia".equals(l.getTipo())) {
                throw regra("Transferência entre unidades não se classifica.");
            }
            l.setUnidade(req.unidade());
            auditar(l);
        }
        return ls.size();
    }

    /* ------------------------------ Transferências ------------------------------ */

    @Transactional
    public LancamentoCaixaResponseDTO criarTransferencia(TransferenciaRequestDTO req) {
        LancamentoEntity l = new LancamentoEntity();
        l.setCriadoPor(Usuario.atual());
        aplicarTransferencia(req, l);
        return GestaoMapper.lancamento(lancamentoRepository.save(l));
    }

    @Transactional
    public LancamentoCaixaResponseDTO atualizarTransferencia(Long id, TransferenciaRequestDTO req) {
        LancamentoEntity l = buscar(id);
        if (!"transferencia".equals(l.getTipo())) {
            throw new AppException("A transferência de ID " + id + " não existe.", HttpStatus.NOT_FOUND);
        }
        aplicarTransferencia(req, l);
        auditar(l);
        return GestaoMapper.lancamento(l);
    }

    private void aplicarTransferencia(TransferenciaRequestDTO req, LancamentoEntity l) {
        if (req.unidadeOrigem().equals(req.unidadeDestino())) {
            throw regra("Origem e destino precisam ser unidades diferentes.");
        }
        var insumo = req.insumoId() == null ? null : cadastro.ingredienteDoCorpo(req.insumoId(), "Item do cadastro inválido.");
        String nome = !Formato.vazio(req.item()) ? req.item().trim() : insumo != null ? insumo.getNome() : "";
        if (nome.isEmpty()) {
            throw regra("Diga o que foi transferido.");
        }
        CategoriaFinanceiraEntity categoria = cadastro.categoriaDoCorpo(req.categoriaId(), "Escolha a linha de custo.");
        if (!List.of("custo_direto", "despesa_variavel", "despesa_fixa").contains(categoria.getGrupo())) {
            throw regra("A linha de custo da transferência deve ser um custo direto ou uma despesa.");
        }
        l.setTipo("transferencia");
        l.setOrigem("transferencia");
        l.setStatus("realizado");
        l.setCategoria(categoria);
        l.setUnidadeOrigem(req.unidadeOrigem());
        l.setUnidadeDestino(req.unidadeDestino());
        l.setUnidade(null);
        l.setInsumo(insumo);
        l.setItem(nome);
        l.setQtd(req.qtd() == null || req.qtd() == 0 ? null : req.qtd());
        l.setUnid(Formato.texto(req.unid()).trim());
        l.setValor(req.valor().setScale(2, RoundingMode.HALF_UP));
        l.setDataCompetencia(Formato.data(req.data(), "data"));
        l.setDataPagamento(null);
        l.setDescricao(descricao("Transferência " + Dominio.UNIDADES_CURTO.get(req.unidadeOrigem()) + " → " + Dominio.UNIDADES_CURTO.get(req.unidadeDestino())
            + " · " + nome));
        l.setResponsavel(Formato.texto(req.responsavel()).trim());
        l.setObs(Formato.texto(req.obs()).trim());
        l.getPagamentos().clear();
    }

    /* ------------------------------ Configurações ------------------------------ */

    /** Saldo de cada conta no fim do dia informado; os pagamentos registrados depois dele são somados. */
    @Transactional
    public void salvarSaldos(SaldosContasRequestDTO req) {
        Formato.data(req.data(), "data");
        Map<String, Object> contas = new LinkedHashMap<>();
        req.contas().forEach((conta, valor) -> {
            if (valor != null) {
                contas.put(conta, Map.of("valor", valor.setScale(2, RoundingMode.HALF_UP).doubleValue(), "data", req.data()));
            }
        });
        gravarRegistro("saldos", Map.of("contas", contas, "por", Usuario.atual()));
    }

    /** Percentuais do rateio da estrutura compartilhada; precisam somar 100. */
    @Transactional
    public void salvarRateio(RateioRequestDTO req) {
        double t = req.buffet() + req.restaurante() + req.chocolates();
        if (Math.abs(t - 100) > 0.5) {
            throw regra("Os percentuais somam " + Formato.fmtN(Formato.round2(t)) + "%: ajuste para 100%.");
        }
        Map<String, Object> rateio = new LinkedHashMap<>();
        rateio.put("buffet", Formato.round2(req.buffet()));
        rateio.put("restaurante", Formato.round2(req.restaurante()));
        rateio.put("chocolates", Formato.round2(req.chocolates()));
        gravarRegistro("unidades", Map.of("rateio", rateio, "por", Usuario.atual()));
    }

    private void gravarRegistro(String chave, Map<String, Object> dados) {
        RegistroSistemaEntity registro = registroRepository.findById(chave).orElseGet(() -> new RegistroSistemaEntity(chave));
        registro.setDados(new LinkedHashMap<>(dados));
        registro.setAtualizadoEm(Instant.now());
        registroRepository.save(registro);
    }

    private static AppException regra(String mensagem) {
        return new AppException(mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private LancamentoEntity buscar(Long id) {
        return lancamentoRepository.findById(id).orElseThrow(() -> new AppException("O lançamento de ID " + id + " não existe.", HttpStatus.NOT_FOUND));
    }
}
