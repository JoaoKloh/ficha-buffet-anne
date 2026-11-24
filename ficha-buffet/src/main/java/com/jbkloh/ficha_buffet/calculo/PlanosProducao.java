package com.jbkloh.ficha_buffet.calculo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jbkloh.ficha_buffet.dtos.resp.CompraPlanoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumosItemPlanoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemPlanoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LinhaPlanoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoProducaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanosProducaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;

/**
 * Planos de produção do Restaurante ANNE e da Chocolates Marie & Anne (protótipo: vProducao):
 * lista de produção, insumos por item, lista de compras dos itens a produzir, situação
 * automática do plano e as fichas que os pratos previstos da carta pedem.
 */
public final class PlanosProducao {

    /** Abaixo disto a falta é arredondamento: o estoque cobre. */
    private static final double FALTA_MINIMA = 0.0005;

    private PlanosProducao() {}

    /**
     * Situação depois de mexer nos itens: tudo feito = concluido; algo feito = em_producao; nada
     * feito mantém a situação, mas um plano concluído volta a em_producao.
     */
    public static String statusAuto(int itens, int feitos, String atual) {
        if (itens > 0 && feitos == itens) {
            return "concluido";
        }
        if (feitos > 0) {
            return "em_producao";
        }
        return "concluido".equals(atual) ? "em_producao" : Formato.vazio(atual) ? "planejado" : atual;
    }

    /* ------------------------------------ Lista ------------------------------------ */

    /** Planos da unidade (abertos primeiro, depois os mais recentes) e os KPIs da tela. */
    public static PlanosProducaoResponseDTO lista(Dados d, String unidade, List<PlanoProducaoResponseDTO> planos) {
        List<PlanoProducaoResponseDTO> ordenados = planos.stream()
            .sorted(Comparator.<PlanoProducaoResponseDTO, Boolean>comparing(p -> "concluido".equals(p.status()))
                .thenComparing(PlanoProducaoResponseDTO::dataInicio, Comparator.reverseOrder())
                .thenComparing(PlanoProducaoResponseDTO::id, Comparator.reverseOrder()))
            .toList();
        List<PlanoProducaoResponseDTO> abertos = ordenados.stream().filter(p -> !"concluido".equals(p.status())).toList();
        int itensAProduzir = abertos.stream().mapToInt(p -> (int) p.itens().stream().filter(x -> !x.feito()).count()).sum();
        boolean chocolates = "chocolates".equals(unidade);
        int prontos = (int) d.lotes.stream()
            .filter(l -> Estoque.loteAtivo(l) && chocolates == "chocolates".equals(l.unidadeNegocio())).count();
        return new PlanosProducaoResponseDTO(unidade,
            new PlanosProducaoResponseDTO.Kpis(abertos.size(), itensAProduzir, ordenados.size() - abertos.size(), prontos),
            ordenados.stream().map(p -> new PlanoListaItemDTO(p.id(), p.titulo(), p.unidade(), p.status(), p.dataInicio(), p.dataFim(), p.itens().size(),
                (int) p.itens().stream().filter(ItemPlanoDTO::feito).count())).toList());
    }

    /* ----------------------------------- Detalhe ----------------------------------- */

    /** Plano aberto: lista de produção, insumos por item e lista de compras dos itens ainda não concluídos. */
    public static PlanoDetalheResponseDTO detalhe(Dados d, PlanoProducaoResponseDTO pl, Estoque est) {
        Map<Long, LoteResponseDTO> lotes = new LinkedHashMap<>();
        d.lotes.forEach(l -> lotes.put(l.id(), l));
        List<LinhaPlanoDTO> linhas = pl.itens().stream().map(x -> linha(d, x, lotes)).toList();
        List<InsumosItemPlanoDTO> insumos = pl.itens().stream().map(x -> insumosDoItem(d, x)).filter(x -> x != null).toList();
        List<CompraPlanoDTO> compras = compras(d, pl, est);
        List<CompraPlanoDTO> comprar = compras.stream().filter(c -> c.falta() > FALTA_MINIMA).toList();
        PlanoDetalheResponseDTO.Kpis kpis = new PlanoDetalheResponseDTO.Kpis(compras.size(), comprar.size(), Formato.round2(Formato.soma(comprar, CompraPlanoDTO::custo)),
            (int) comprar.stream().filter(CompraPlanoDTO::semPreco).count(), (int) pl.itens().stream().filter(ItemPlanoDTO::feito).count());
        return new PlanoDetalheResponseDTO(pl, linhas, insumos, new PlanoDetalheResponseDTO.Compras(kpis, compras));
    }

    private static LinhaPlanoDTO linha(Dados d, ItemPlanoDTO x, Map<Long, LoteResponseDTO> lotes) {
        PreparacaoResponseDTO p = d.preparacao(x.preparacaoId());
        String u = unidadeDa(p);
        double rend = p == null ? 1 : Fichas.rend(p.rendimento());
        String categoria = p == null ? "" : Formato.texto(p.categoria());
        LoteResponseDTO lote = x.loteId() == null ? null : lotes.get(x.loteId());
        return new LinhaPlanoDTO(x.k(), x.preparacaoId(), p == null ? null : p.nome(), categoria, Dominio.rotulo(Dominio.CATEGORIAS_PREPARACAO, categoria, "Outros"),
            Fichas.pracaDe(p), Formato.r3(x.qtd()), Fichas.unidRendLabel(u, x.qtd()), x.qtd() / rend, rend,
            p == null ? "Ficha removida" : Fichas.fichaStatus(d, p), p == null ? null : Estoque.validadeDiasDe(p), x.feito(), x.feitoEm(),
            lote == null ? null : new LinhaPlanoDTO.Lote(lote.id(), lote.codigo()));
    }

    /** Insumos de um item com ficha e quantidade: por receita e total do item, agrupados por insumo e sub-receita. */
    private static InsumosItemPlanoDTO insumosDoItem(Dados d, ItemPlanoDTO x) {
        PreparacaoResponseDTO p = d.preparacao(x.preparacaoId());
        if (p == null || !(x.qtd() > 0)) {
            return null;
        }
        String u = unidadeDa(p);
        double rend = Fichas.rend(p.rendimento());
        double receitas = x.qtd() / rend;
        Map<String, double[]> porReceita = new LinkedHashMap<>();
        Map<String, String[]> modelo = new LinkedHashMap<>();
        Fichas.explode(d, p, 1, (i, q, via) -> {
            String k = (i == null ? "?" : String.valueOf(i.id())) + "|" + Formato.texto(via);
            porReceita.computeIfAbsent(k, c -> new double[1])[0] += q;
            modelo.computeIfAbsent(k, c -> new String[] { i == null ? "Insumo removido" : i.nome(), i == null ? "" : i.unidade(), Formato.texto(via) });
        });
        List<InsumosItemPlanoDTO.Insumo> ins = modelo.entrySet().stream().map(e -> {
            double qb = porReceita.get(e.getKey())[0];
            return new InsumosItemPlanoDTO.Insumo(e.getValue()[0], e.getValue()[1], e.getValue()[2], Formato.r3(qb), Formato.r3(qb * receitas));
        }).toList();
        String categoria = Formato.texto(p.categoria());
        return new InsumosItemPlanoDTO(x.k(), p.nome(), Dominio.rotulo(Dominio.CATEGORIAS_PREPARACAO, categoria, "Outros"), Fichas.pracaDe(p),
            Formato.r3(x.qtd()), Fichas.unidRendLabel(u, x.qtd()), Formato.fmtN(rend) + " " + Fichas.unidRendLabel(u, rend), x.feito(),
            Formato.texto(p.modoPreparo()), ins);
    }

    /**
     * Lista de compras dos itens ainda não concluídos: o que precisam (sem louça e utensílio, que
     * voltam), o saldo atual em estoque (que já desconta os lotes concluídos) e o que falta.
     */
    private static List<CompraPlanoDTO> compras(Dados d, PlanoProducaoResponseDTO pl, Estoque est) {
        Map<Long, double[]> precisa = new LinkedHashMap<>();
        Map<Long, List<String>> usos = new LinkedHashMap<>();
        for (ItemPlanoDTO x : pl.itens()) {
            PreparacaoResponseDTO p = d.preparacao(x.preparacaoId());
            if (p == null || !(x.qtd() > 0) || x.feito()) {
                continue;
            }
            Fichas.explode(d, p, x.qtd() / Fichas.rend(p.rendimento()), (i, q, via) -> {
                if (i == null || Dominio.reutilizavel(i.tipo())) {
                    return;
                }
                precisa.computeIfAbsent(i.id(), c -> new double[1])[0] += q;
                List<String> u = usos.computeIfAbsent(i.id(), c -> new ArrayList<>());
                if (!u.contains(p.nome())) {
                    u.add(p.nome());
                }
            });
        }
        List<CompraPlanoDTO> lista = new ArrayList<>();
        precisa.forEach((insumoId, q) -> {
            InsumoResponseDTO i = d.insumo(insumoId);
            double estoque = Math.max(0, est.fisico(insumoId));
            double falta = Math.max(0, q[0] - estoque);
            lista.add(new CompraPlanoDTO(insumoId, i.nome(), i.unidade(), Formato.r3(q[0]), Formato.r3(estoque), Formato.r3(falta),
                Formato.round2(falta * i.custo()), !(i.custo() > 0), List.copyOf(usos.get(insumoId))));
        });
        lista.sort(Formato.porNome(CompraPlanoDTO::nome));
        return lista;
    }

    private static String unidadeDa(PreparacaoResponseDTO p) {
        return p == null || Formato.vazio(p.unidadeRendimento()) ? "porções" : p.unidadeRendimento();
    }

    /* ------------------------------------ Carta ------------------------------------ */

    /**
     * Fichas que os pratos previstos pedem: para cada prato ativo da carta com fichas, quantidade
     * prevista vezes a quantidade de cada ficha no prato. preparacaoId -> quantidade (na unidade da ficha).
     */
    public static Map<Long, Double> necessidadeDaCarta(Dados d, Map<String, Integer> previsao) {
        Map<Long, Double> need = new LinkedHashMap<>();
        for (PratoVendaResponseDTO pv : d.pratosVenda) {
            Integer n = previsao.get(String.valueOf(pv.id()));
            if (!pv.ativo() || pv.componentes().isEmpty() || n == null || n <= 0) {
                continue;
            }
            for (PratoVendaResponseDTO.Componente c : pv.componentes()) {
                if (d.preparacao(c.preparacaoId()) != null) {
                    need.merge(c.preparacaoId(), n * c.qtd(), Double::sum);
                }
            }
        }
        return need;
    }
}
