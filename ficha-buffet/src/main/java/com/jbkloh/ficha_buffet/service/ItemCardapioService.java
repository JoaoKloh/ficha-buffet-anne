package com.jbkloh.ficha_buffet.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Fichas;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.calculo.ImportacaoCardapio;
import com.jbkloh.ficha_buffet.dtos.req.ImportarCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ItemCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PatchItemCardapioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ImportarCardapioAnaliseResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.ItemCardapioEntity;
import com.jbkloh.ficha_buffet.model.PreparacaoEntity;
import com.jbkloh.ficha_buffet.repositories.ExecucaoCozinhaRepository;
import com.jbkloh.ficha_buffet.repositories.ItemCardapioRepository;
import com.jbkloh.ficha_buffet.repositories.PreparacaoRepository;

import lombok.RequiredArgsConstructor;

/** Cardápio do evento: pratos, situação de cada um, cópia de outro evento e importação por texto. */
@Service
@RequiredArgsConstructor
public class ItemCardapioService {

    private final ItemCardapioRepository itemCardapioRepository;
    private final PreparacaoRepository preparacaoRepository;
    private final ExecucaoCozinhaRepository execucaoRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    @Transactional
    public ItemCardapioResponseDTO criarItem(ItemCardapioRequestDTO req) {
        EventoEntity evento = cadastro.evento(req.eventoId());
        if (itemCardapioRepository.existsByEventoIdAndPreparacaoId(req.eventoId(), req.preparacaoId())) {
            throw new AppException("Este prato já está no cardápio.", HttpStatus.CONFLICT);
        }
        ItemCardapioEntity item = new ItemCardapioEntity();
        item.setEvento(evento);
        aplicar(req, item);
        return GestaoMapper.itemCardapio(itemCardapioRepository.save(item));
    }

    @Transactional
    public ItemCardapioResponseDTO atualizarItem(Long id, ItemCardapioRequestDTO req) {
        ItemCardapioEntity item = buscar(id);
        aplicar(req, item);
        return GestaoMapper.itemCardapio(item);
    }

    @Transactional
    public ItemCardapioResponseDTO ajustarItem(Long id, PatchItemCardapioRequestDTO req) {
        ItemCardapioEntity item = buscar(id);
        if (req.status() != null) {
            item.setStatus(req.status());
        }
        if (req.porcao() != null) {
            item.setPorcao(req.porcao());
        }
        return GestaoMapper.itemCardapio(item);
    }

    @Transactional
    public void apagarItem(Long id) {
        ItemCardapioEntity item = buscar(id);
        execucaoRepository.deleteAll(execucaoRepository.findByItemCardapioId(id));
        itemCardapioRepository.delete(item);
    }

    /** Copia do evento de origem os pratos que ainda não estão no cardápio; devolve quantos entraram. */
    @Transactional
    public int copiarCardapio(Long eventoId, Long origemEventoId) {
        EventoEntity evento = cadastro.evento(eventoId);
        cadastro.evento(origemEventoId);
        Set<Long> jaNoCardapio = fichasNoCardapio(eventoId);
        int copiados = 0;
        for (ItemCardapioEntity origem : itemCardapioRepository.findByEventoId(origemEventoId)) {
            if (jaNoCardapio.add(origem.getPreparacao().getId())) {
                itemCardapioRepository.save(novoItem(evento, origem.getPreparacao(), origem.getPorcao()));
                copiados++;
            }
        }
        return copiados;
    }

    /**
     * Adiciona de uma vez as fichas marcadas, com o fator por convidado da ficha e a praça acompanhando a ficha.
     * Fichas que já estão no cardápio (ou repetidas no pedido) são ignoradas; devolve quantos pratos entraram.
     */
    @Transactional
    public int adicionarPratos(Long eventoId, List<Long> preparacaoIds) {
        EventoEntity evento = cadastro.evento(eventoId);
        // Confere todas antes de gravar: uma ficha inexistente não deixa o cardápio pela metade.
        List<PreparacaoEntity> fichas = preparacaoIds.stream().distinct().map(cadastro::preparacaoDoCorpo).toList();
        Set<Long> jaNoCardapio = fichasNoCardapio(eventoId);
        int adicionados = 0;
        for (PreparacaoEntity ficha : fichas) {
            if (jaNoCardapio.add(ficha.getId())) {
                itemCardapioRepository.save(novoItem(evento, ficha, ficha.getPorcaoPadrao()));
                adicionados++;
            }
        }
        return adicionados;
    }

    /** Confere o texto colado contra as fichas, sem gravar. */
    @Transactional(readOnly = true)
    public ImportarCardapioAnaliseResponseDTO analisarImportacao(Long eventoId, String texto) {
        cadastro.evento(eventoId);
        return ImportacaoCardapio.analisar(dadosService.carregar(), eventoId, texto);
    }

    /** Grava as linhas confirmadas; linhas sem ficha viram fichas incompletas. Devolve quantos pratos entraram. */
    @Transactional
    public int confirmarImportacao(Long eventoId, ImportarCardapioRequestDTO req) {
        EventoEntity evento = cadastro.evento(eventoId);
        Set<Long> jaNoCardapio = fichasNoCardapio(eventoId);
        int adicionados = 0;
        for (ImportarCardapioRequestDTO.Linha linha : req.linhas()) {
            PreparacaoEntity ficha;
            if (linha.preparacaoId() != null) {
                ficha = cadastro.preparacaoDoCorpo(linha.preparacaoId());
            } else if (linha.criarCategoria() != null && !linha.texto().isBlank()) {
                ficha = preparacaoRepository.save(fichaIncompleta(linha.texto().trim(), linha.criarCategoria()));
            } else {
                continue;
            }
            if (jaNoCardapio.add(ficha.getId())) {
                itemCardapioRepository.save(novoItem(evento, ficha, ficha.getPorcaoPadrao()));
                adicionados++;
            }
        }
        return adicionados;
    }

    private Set<Long> fichasNoCardapio(Long eventoId) {
        Set<Long> ids = new HashSet<>();
        List<ItemCardapioEntity> itens = itemCardapioRepository.findByEventoId(eventoId);
        itens.forEach(c -> ids.add(c.getPreparacao().getId()));
        return ids;
    }

    private static ItemCardapioEntity novoItem(EventoEntity evento, PreparacaoEntity ficha, Double porcao) {
        ItemCardapioEntity item = new ItemCardapioEntity();
        item.setEvento(evento);
        item.setPreparacao(ficha);
        item.setPorcao(porcao != null && porcao > 0 ? porcao : 1.0);
        return item;
    }

    private static PreparacaoEntity fichaIncompleta(String nome, String categoria) {
        PreparacaoEntity ficha = new PreparacaoEntity();
        ficha.setNome(nome.substring(0, 1).toUpperCase() + nome.substring(1));
        ficha.setCategoria(categoria);
        ficha.setPraca(Fichas.pracaPadrao(categoria));
        ficha.setRendimento(1.0);
        ficha.setModoPreparo("Criada pela importação de cardápio. Complete a ficha.");
        ficha.setAtualizadoEm(LocalDate.now());
        return ficha;
    }

    private void aplicar(ItemCardapioRequestDTO req, ItemCardapioEntity item) {
        PreparacaoEntity ficha = cadastro.preparacaoDoCorpo(req.preparacaoId());
        String pracaDaFicha = Formato.vazio(ficha.getPraca()) ? Fichas.pracaPadrao(ficha.getCategoria()) : ficha.getPraca();
        item.setPreparacao(ficha);
        item.setPorcao(req.porcao());
        // Só guarda a praça quando difere da ficha: nula acompanha a ficha se ela mudar.
        item.setPraca(req.praca() != null && !req.praca().equals(pracaDaFicha) ? req.praca() : null);
        item.setResponsavel(Formato.texto(req.responsavel()).trim());
    }

    private ItemCardapioEntity buscar(Long id) {
        return itemCardapioRepository.findById(id)
            .orElseThrow(() -> new AppException("O prato do cardápio de id " + id + " não existe.", HttpStatus.NOT_FOUND));
    }
}
