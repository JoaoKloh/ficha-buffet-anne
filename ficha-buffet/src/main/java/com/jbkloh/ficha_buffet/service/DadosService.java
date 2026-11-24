package com.jbkloh.ficha_buffet.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Ordens;
import com.jbkloh.ficha_buffet.dtos.resp.SetorProducaoDTO;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.MovimentoEstoqueEntity;
import com.jbkloh.ficha_buffet.model.RegistroSistemaEntity;
import com.jbkloh.ficha_buffet.repositories.CategoriaFinanceiraRepository;
import com.jbkloh.ficha_buffet.repositories.ClienteRepository;
import com.jbkloh.ficha_buffet.repositories.EventoRepository;
import com.jbkloh.ficha_buffet.repositories.ExecucaoCozinhaRepository;
import com.jbkloh.ficha_buffet.repositories.ExtraEventoRepository;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.ItemCardapioRepository;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;
import com.jbkloh.ficha_buffet.repositories.LiberacaoEventoRepository;
import com.jbkloh.ficha_buffet.repositories.LotePreparoRepository;
import com.jbkloh.ficha_buffet.repositories.MovimentoEstoqueRepository;
import com.jbkloh.ficha_buffet.repositories.NotaFiscalRepository;
import com.jbkloh.ficha_buffet.repositories.OrdemExecucaoRepository;
import com.jbkloh.ficha_buffet.repositories.OrdemPlanoRepository;
import com.jbkloh.ficha_buffet.repositories.ParcelaRepository;
import com.jbkloh.ficha_buffet.repositories.PratoVendaRepository;
import com.jbkloh.ficha_buffet.repositories.PreparacaoRepository;
import com.jbkloh.ficha_buffet.repositories.ProducaoEventoRepository;
import com.jbkloh.ficha_buffet.repositories.RegistroSistemaRepository;
import com.jbkloh.ficha_buffet.repositories.SetorProducaoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Carrega o retrato completo do cadastro para os cálculos das telas (custo de
 * fichas, necessidades do cardápio, saldo de estoque, DRE). O volume de um
 * buffet cabe em memória; as coleções das entidades vêm em lotes
 * (hibernate.default_batch_fetch_size).
 */
@Service
@RequiredArgsConstructor
public class DadosService {

    private static final Sort POR_ID = Sort.by("id");

    private final ClienteRepository clienteRepository;
    private final EventoRepository eventoRepository;
    private final ParcelaRepository parcelaRepository;
    private final ExtraEventoRepository extraRepository;
    private final LancamentoRepository lancamentoRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;
    private final IngredientesRepository ingredientesRepository;
    private final PreparacaoRepository preparacaoRepository;
    private final ItemCardapioRepository itemCardapioRepository;
    private final MovimentoEstoqueRepository movimentoRepository;
    private final NotaFiscalRepository notaFiscalRepository;
    private final PratoVendaRepository pratoVendaRepository;
    private final LotePreparoRepository loteRepository;
    private final ExecucaoCozinhaRepository execucaoRepository;
    private final RegistroSistemaRepository registroRepository;
    private final ProducaoEventoRepository producaoEventoRepository;
    private final OrdemPlanoRepository ordemPlanoRepository;
    private final OrdemExecucaoRepository ordemExecucaoRepository;
    private final LiberacaoEventoRepository liberacaoRepository;
    private final SetorProducaoRepository setorRepository;

    /** Cadastros que podem ser carregados sozinhos, para as opções dos formulários. */
    public enum Parte { CATEGORIAS, CLIENTES, INSUMOS, PREPARACOES, EVENTOS, CARDAPIO }

    /**
     * Só os cadastros pedidos: as demais coleções ficam vazias. Serve às opções dos
     * formulários, que não precisam de lançamentos, movimentos nem notas.
     */
    @Transactional(readOnly = true)
    public Dados carregar(Set<Parte> partes) {
        Dados d = new Dados();
        if (partes.contains(Parte.CATEGORIAS)) {
            categoriaRepository.findAll(POR_ID).forEach(c -> d.categorias.add(GestaoMapper.categoria(c)));
        }
        if (partes.contains(Parte.CLIENTES)) {
            clienteRepository.findAll(POR_ID).forEach(c -> d.clientes.add(GestaoMapper.cliente(c)));
        }
        if (partes.contains(Parte.INSUMOS)) {
            ingredientesRepository.findAll(POR_ID).forEach(i -> d.insumos.add(GestaoMapper.insumo(i)));
        }
        if (partes.contains(Parte.PREPARACOES)) {
            preparacaoRepository.findAll(POR_ID).forEach(p -> d.preparacoes.add(GestaoMapper.preparacao(p)));
        }
        if (partes.contains(Parte.EVENTOS)) {
            eventoRepository.findAll(POR_ID).forEach(e -> d.eventos.add(GestaoMapper.evento(e)));
        }
        if (partes.contains(Parte.CARDAPIO)) {
            itemCardapioRepository.findAll(POR_ID).forEach(c -> d.cardapio.add(GestaoMapper.itemCardapio(c)));
        }
        return d;
    }

    @Transactional(readOnly = true)
    public Dados carregar() {
        Dados d = carregar(EnumSet.allOf(Parte.class));

        // Parcelas, extras e notas guardam o vínculo do lado do lançamento.
        Map<Long, Long> lancamentoDaParcela = new HashMap<>();
        Map<Long, Double> recebidoDaParcela = new HashMap<>();
        Map<Long, Long> lancamentoDoExtra = new HashMap<>();
        Map<Long, List<Long>> lancamentosDaNota = new HashMap<>();
        for (LancamentoEntity l : lancamentoRepository.findAll(POR_ID)) {
            d.lancamentos.add(GestaoMapper.lancamento(l));
            if (l.getParcela() != null) {
                lancamentoDaParcela.put(l.getParcela().getId(), l.getId());
                recebidoDaParcela.put(l.getParcela().getId(), Caixa.pago(d.lancamentos.get(d.lancamentos.size() - 1)));
            }
            if (l.getExtra() != null) {
                lancamentoDoExtra.put(l.getExtra().getId(), l.getId());
            }
            if (l.getNotaFiscal() != null) {
                lancamentosDaNota.computeIfAbsent(l.getNotaFiscal().getId(), k -> new ArrayList<>()).add(l.getId());
            }
        }
        parcelaRepository.findAll(POR_ID).forEach(p -> d.parcelas.add(GestaoMapper.parcela(p, lancamentoDaParcela.get(p.getId()),
            recebidoDaParcela.getOrDefault(p.getId(), 0.0))));
        extraRepository.findAll(POR_ID).forEach(x -> d.extras.add(GestaoMapper.extra(x, lancamentoDoExtra.get(x.getId()))));

        Map<Long, List<Long>> movimentosDaNota = new HashMap<>();
        for (MovimentoEstoqueEntity m : movimentoRepository.findAll(POR_ID)) {
            d.movimentos.add(GestaoMapper.movimento(m));
            if (m.getNotaFiscal() != null) {
                movimentosDaNota.computeIfAbsent(m.getNotaFiscal().getId(), k -> new ArrayList<>()).add(m.getId());
            }
        }
        notaFiscalRepository.findAll(POR_ID).forEach(n -> d.nfs.add(GestaoMapper.notaFiscal(n, movimentosDaNota.getOrDefault(n.getId(), List.of()),
            lancamentosDaNota.getOrDefault(n.getId(), List.of()))));
        pratoVendaRepository.findAll(POR_ID).forEach(p -> d.pratosVenda.add(GestaoMapper.pratoVenda(p)));
        loteRepository.findAll(POR_ID).forEach(l -> d.lotes.add(GestaoMapper.lote(l)));
        execucaoRepository.findAll(POR_ID).forEach(x -> d.execucoes.add(GestaoMapper.execucao(x)));
        carregarOrdens(d);
        carregarRegistros(d);
        return d;
    }

    /** Mapas de produção, plano e execução das ordens, liberações e setores da cozinha. */
    private void carregarOrdens(Dados d) {
        producaoEventoRepository.findAll(POR_ID).forEach(m -> d.mapas.put(m.getEvento().getId(), GestaoMapper.mapaProducao(m)));
        ordemPlanoRepository.findAll(POR_ID).forEach(p -> {
            Ordens.Plano x = GestaoMapper.ordemPlano(p);
            d.planosOrdem.put(Ordens.chave(x.eventoId(), x.k()), x);
        });
        ordemExecucaoRepository.findAll(POR_ID).forEach(e -> {
            Ordens.Execucao x = GestaoMapper.ordemExecucao(e);
            d.execucoesOrdem.put(Ordens.chave(x.eventoId(), x.k()), x);
        });
        liberacaoRepository.findAll(POR_ID).forEach(l -> d.liberacoes.put(l.getEvento().getId(), GestaoMapper.liberacao(l)));
        d.setores.addAll(setores());
    }

    /** Setores da cozinha na ordem gravada; sem nenhum gravado, os padrões. */
    @Transactional(readOnly = true)
    public List<SetorProducaoDTO> setores() {
        List<SetorProducaoDTO> l = setorRepository.findAll(Sort.by("ordem", "id")).stream().map(GestaoMapper::setor).toList();
        return l.isEmpty() ? Dominio.SETORES_PADRAO.entrySet().stream().map(e -> new SetorProducaoDTO(e.getKey(), e.getValue(), true)).toList() : l;
    }

    /** Rateio da estrutura compartilhada e saldos informados das contas (sistema_registros). */
    @SuppressWarnings("unchecked")
    private void carregarRegistros(Dados d) {
        registroRepository.findById("unidades").map(RegistroSistemaEntity::getDados).ifPresent(r -> {
            if (r.get("rateio") instanceof Map<?, ?> m) {
                ((Map<String, Object>) m).forEach((u, v) -> {
                    if (v instanceof Number n) {
                        d.rateioGravado.put(u, n.doubleValue());
                    }
                });
            }
        });
        registroRepository.findById("saldos").map(RegistroSistemaEntity::getDados).ifPresent(r -> {
            if (r.get("contas") instanceof Map<?, ?> m) {
                ((Map<String, Object>) m).forEach((conta, v) -> {
                    if (v instanceof Map<?, ?> b && b.get("valor") instanceof Number n && b.get("data") instanceof String data) {
                        d.saldos.put(conta, new Dados.SaldoInformado(n.doubleValue(), data));
                    }
                });
            }
        });
    }
}
