package com.jbkloh.ficha_buffet.calculo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExtraResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MapaProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ParcelaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SetorProducaoDTO;

/**
 * Retrato de tudo que está cadastrado, já em DTOs: os cálculos cruzam várias
 * coleções (cardápio, fichas, estoque, caixa) e trabalham sobre ele, sem tocar no banco.
 * Preencha as listas antes de consultar por id: os índices são montados no primeiro uso.
 */
public final class Dados {

    public final List<ClienteResponseDTO> clientes = new ArrayList<>();
    public final List<EventoResponseDTO> eventos = new ArrayList<>();
    public final List<ParcelaResponseDTO> parcelas = new ArrayList<>();
    public final List<ExtraResponseDTO> extras = new ArrayList<>();
    public final List<LancamentoCaixaResponseDTO> lancamentos = new ArrayList<>();
    public final List<CategoriaResponseDTO> categorias = new ArrayList<>();
    public final List<InsumoResponseDTO> insumos = new ArrayList<>();
    public final List<PreparacaoResponseDTO> preparacoes = new ArrayList<>();
    public final List<ItemCardapioResponseDTO> cardapio = new ArrayList<>();
    public final List<MovimentoResponseDTO> movimentos = new ArrayList<>();
    public final List<NotaFiscalResponseDTO> nfs = new ArrayList<>();
    public final List<PratoVendaResponseDTO> pratosVenda = new ArrayList<>();
    public final List<LoteResponseDTO> lotes = new ArrayList<>();
    public final List<ExecucaoResponseDTO> execucoes = new ArrayList<>();

    /* Ordens de produção: mapa por evento, plano e execução por evento|chave, liberação por evento. */
    public final Map<Long, MapaProducaoDTO> mapas = new HashMap<>();
    public final Map<String, Ordens.Plano> planosOrdem = new HashMap<>();
    public final Map<String, Ordens.Execucao> execucoesOrdem = new HashMap<>();
    public final Map<Long, Ordens.Liberacao> liberacoes = new HashMap<>();
    /** Setores da cozinha (os padrões enquanto nenhum foi gravado). */
    public final List<SetorProducaoDTO> setores = new ArrayList<>();

    /** Percentuais do rateio da estrutura compartilhada como foram gravados; vazio = ainda não definido (partes iguais). */
    public final Map<String, Double> rateioGravado = new HashMap<>();
    /** Saldo informado de cada conta (do extrato), no fim do dia indicado. */
    public final Map<String, SaldoInformado> saldos = new HashMap<>();

    public record SaldoInformado(double valor, String data) {}

    private Map<Long, ClienteResponseDTO> clientesPorId;
    private Map<Long, EventoResponseDTO> eventosPorId;
    private Map<Long, CategoriaResponseDTO> categoriasPorId;
    private Map<Long, ParcelaResponseDTO> parcelasPorId;
    private Map<Long, InsumoResponseDTO> insumosPorId;
    private Map<Long, PreparacaoResponseDTO> preparacoesPorId;
    private Map<Long, ItemCardapioResponseDTO> cardapioPorId;
    private Map<String, ExecucaoResponseDTO> execucoesPorChave;
    private Map<Long, List<ItemCardapioResponseDTO>> cardapioPorEvento;

    public ClienteResponseDTO cliente(Long id) {
        if (clientesPorId == null) {
            clientesPorId = indice(clientes, ClienteResponseDTO::id);
        }
        return id == null ? null : clientesPorId.get(id);
    }

    public EventoResponseDTO evento(Long id) {
        if (eventosPorId == null) {
            eventosPorId = indice(eventos, EventoResponseDTO::id);
        }
        return id == null ? null : eventosPorId.get(id);
    }

    public CategoriaResponseDTO categoria(Long id) {
        if (categoriasPorId == null) {
            categoriasPorId = indice(categorias, CategoriaResponseDTO::id);
        }
        return id == null ? null : categoriasPorId.get(id);
    }

    public ParcelaResponseDTO parcela(Long id) {
        if (parcelasPorId == null) {
            parcelasPorId = indice(parcelas, ParcelaResponseDTO::id);
        }
        return id == null ? null : parcelasPorId.get(id);
    }

    public InsumoResponseDTO insumo(Long id) {
        if (insumosPorId == null) {
            insumosPorId = indice(insumos, InsumoResponseDTO::id);
        }
        return id == null ? null : insumosPorId.get(id);
    }

    public PreparacaoResponseDTO preparacao(Long id) {
        if (preparacoesPorId == null) {
            preparacoesPorId = indice(preparacoes, PreparacaoResponseDTO::id);
        }
        return id == null ? null : preparacoesPorId.get(id);
    }

    public ItemCardapioResponseDTO itemCardapio(Long id) {
        if (cardapioPorId == null) {
            cardapioPorId = indice(cardapio, ItemCardapioResponseDTO::id);
        }
        return id == null ? null : cardapioPorId.get(id);
    }

    public ExecucaoResponseDTO execucao(String chave) {
        if (execucoesPorChave == null) {
            execucoesPorChave = indice(execucoes, ExecucaoResponseDTO::chave);
        }
        return execucoesPorChave.get(chave);
    }

    public List<ItemCardapioResponseDTO> cardapioDe(Long eventoId) {
        if (cardapioPorEvento == null) {
            cardapioPorEvento = new HashMap<>();
            for (ItemCardapioResponseDTO c : cardapio) {
                cardapioPorEvento.computeIfAbsent(c.eventoId(), k -> new ArrayList<>()).add(c);
            }
        }
        return cardapioPorEvento.getOrDefault(eventoId, List.of());
    }

    public String nomeEvento(Long id) {
        EventoResponseDTO e = evento(id);
        return e == null ? null : e.nome();
    }

    private static <K, T> Map<K, T> indice(List<T> lista, Function<T, K> chave) {
        Map<K, T> m = new HashMap<>();
        for (T x : lista) {
            m.putIfAbsent(chave.apply(x), x);
        }
        return m;
    }
}
