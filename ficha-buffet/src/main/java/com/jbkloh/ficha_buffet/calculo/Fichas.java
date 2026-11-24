package com.jbkloh.ficha_buffet.calculo;

import java.util.List;

import com.jbkloh.ficha_buffet.dtos.resp.CmvDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CustoPreparacaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;

/** Custo e explosão de fichas técnicas com sub-receitas (até 6 níveis) e CMV dos pratos da carta. */
public final class Fichas {

    private Fichas() {}

    private static final CustoPreparacaoDTO SEM_CUSTO = new CustoPreparacaoDTO(0, 0, 0);

    /** Recebe cada insumo da explosão: o insumo (nulo se removido), a quantidade e a sub-receita por onde veio. */
    @FunctionalInterface
    public interface Saida {
        void aceitar(InsumoResponseDTO insumo, double qtd, String via);
    }

    public static CustoPreparacaoDTO custoPrep(Dados d, PreparacaoResponseDTO p) {
        return p == null ? SEM_CUSTO : custoPrep(d, p.itens(), p.rendimento(), 0);
    }

    public static CustoPreparacaoDTO custoPrep(Dados d, List<PreparacaoResponseDTO.Item> itens, double rendimento) {
        return custoPrep(d, itens, rendimento, 0);
    }

    private static CustoPreparacaoDTO custoPrep(Dados d, List<PreparacaoResponseDTO.Item> itens, double rendimento, int nivel) {
        if (nivel > 6) {
            return SEM_CUSTO;
        }
        double custo = 0;
        int faltando = 0;
        for (PreparacaoResponseDTO.Item it : itens) {
            double q = it.qtd() * fc(it);
            if (it.subId() != null) {
                PreparacaoResponseDTO sub = d.preparacao(it.subId());
                if (sub == null) {
                    faltando++;
                    continue;
                }
                CustoPreparacaoDTO cs = custoPrep(d, sub.itens(), sub.rendimento(), nivel + 1);
                custo += q * cs.porUnid();
                faltando += cs.faltando();
                continue;
            }
            InsumoResponseDTO i = d.insumo(it.insumoId());
            if (i == null || !(i.custo() > 0)) {
                faltando++;
                continue;
            }
            custo += q * i.custo();
        }
        return new CustoPreparacaoDTO(custo, custo / rend(rendimento), faltando);
    }

    /** Distribui `lotes` receitas de p em insumos. */
    public static void explode(Dados d, PreparacaoResponseDTO p, double lotes, Saida saida) {
        explode(d, p, lotes, saida, "", 0);
    }

    private static void explode(Dados d, PreparacaoResponseDTO p, double lotes, Saida saida, String via, int nivel) {
        if (p == null || nivel > 6) {
            return;
        }
        for (PreparacaoResponseDTO.Item it : p.itens()) {
            double q = lotes * it.qtd() * fc(it);
            if (it.subId() != null) {
                PreparacaoResponseDTO sub = d.preparacao(it.subId());
                if (sub != null) {
                    explode(d, sub, q / rend(sub.rendimento()), saida, sub.nome(), nivel + 1);
                }
                continue;
            }
            saida.aceitar(d.insumo(it.insumoId()), q, via);
        }
    }

    private static double fc(PreparacaoResponseDTO.Item it) {
        return it.fc() != 0 ? it.fc() : 1;
    }

    public static double rend(double rendimento) {
        return rendimento != 0 ? rendimento : 1;
    }

    public static String fichaStatus(Dados d, PreparacaoResponseDTO p) {
        if (p.itens().isEmpty()) {
            return "Incompleta";
        }
        return custoPrep(d, p).faltando() > 0 ? "Sem preço" : "Completa";
    }

    public static String pracaPadrao(String categoria) {
        return Dominio.PRACA_PADRAO.getOrDefault(categoria, "Cozinha quente");
    }

    public static String pracaDe(PreparacaoResponseDTO p) {
        if (p == null) {
            return "Cozinha quente";
        }
        return Formato.vazio(p.praca()) ? pracaPadrao(p.categoria()) : p.praca();
    }

    /** "porções" vira "porção"/"porções"; o resto é a própria unidade. */
    public static String unidRendLabel(String unidade, double n) {
        return "porções".equals(unidade) ? (n == 1 ? "porção" : "porções") : unidade;
    }

    public static String fatorUnid(PreparacaoResponseDTO p) {
        String u = p == null ? "porções" : p.unidadeRendimento();
        return "porções".equals(u) ? "porção" : u;
    }

    public static String fatorLabel(double porcao, PreparacaoResponseDTO p) {
        String u = p == null ? "porções" : p.unidadeRendimento();
        if ("kg".equals(u)) {
            return Formato.fmtN(Math.round(porcao * 1000)) + " g por convidado";
        }
        if ("l".equals(u)) {
            return Formato.fmtN(Math.round(porcao * 1000)) + " ml por convidado";
        }
        return Formato.fmtN(porcao) + " " + unidRendLabel(u, porcao) + " por convidado";
    }

    /** Custo dos ingredientes de um prato da carta e CMV pelo preço de venda. */
    public static CmvDTO cmv(Dados d, List<PratoVendaResponseDTO.Componente> componentes, List<PratoVendaResponseDTO.Extra> extras, double preco) {
        double custo = 0;
        int faltando = 0;
        for (PratoVendaResponseDTO.Componente x : componentes) {
            PreparacaoResponseDTO p = d.preparacao(x.preparacaoId());
            if (p == null) {
                faltando++;
                continue;
            }
            CustoPreparacaoDTO cp = custoPrep(d, p);
            custo += cp.porUnid() * x.qtd();
            faltando += cp.faltando();
        }
        for (PratoVendaResponseDTO.Extra x : extras) {
            InsumoResponseDTO i = d.insumo(x.insumoId());
            if (i == null || !(i.custo() > 0)) {
                faltando++;
                continue;
            }
            custo += x.qtd() * i.custo();
        }
        return new CmvDTO(custo, faltando, preco > 0 ? custo / preco : null);
    }

    public static CmvDTO cmv(Dados d, PratoVendaResponseDTO pv) {
        return cmv(d, pv.componentes(), pv.extras(), pv.preco());
    }
}
