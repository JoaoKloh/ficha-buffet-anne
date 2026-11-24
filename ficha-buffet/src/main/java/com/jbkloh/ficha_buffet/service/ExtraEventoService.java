package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.dtos.req.ExtraEventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExtraResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.CategoriaFinanceiraEntity;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.ExtraEventoEntity;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.repositories.ExtraEventoRepository;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;

import lombok.RequiredArgsConstructor;

/** Extras cobrados no evento. Cada um vira uma receita prevista no caixa, mantida em sincronia. */
@Service
@RequiredArgsConstructor
public class ExtraEventoService {

    private static final String SEM_CATEGORIA = "Crie uma categoria de receita em Caixa › Categorias.";

    private final ExtraEventoRepository extraRepository;
    private final LancamentoRepository lancamentoRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    @Transactional
    public ExtraResponseDTO criarExtra(ExtraEventoRequestDTO req) {
        EventoEntity evento = cadastro.evento(req.eventoId());
        CategoriaFinanceiraEntity categoria = categoriaDoExtra(evento);
        ExtraEventoEntity extra = new ExtraEventoEntity();
        extra.setEvento(evento);
        aplicar(req, extra);
        extraRepository.save(extra);
        LancamentoEntity receita = CaixaService.novoLancamento(categoria, evento, descricao(req, evento), total(req),
            evento.getDataEvento() != null ? evento.getDataEvento() : LocalDate.now(), "extra");
        receita.setExtra(extra);
        return GestaoMapper.extra(extra, lancamentoRepository.save(receita).getId());
    }

    @Transactional
    public ExtraResponseDTO atualizarExtra(Long id, ExtraEventoRequestDTO req) {
        ExtraEventoEntity extra = buscar(id);
        aplicar(req, extra);
        LancamentoEntity receita = lancamentoRepository.findByExtraId(id).orElse(null);
        if (receita != null) {
            receita.setDescricao(CaixaService.descricao(descricao(req, extra.getEvento())));
            receita.setValor(total(req));
        }
        return GestaoMapper.extra(extra, receita == null ? null : receita.getId());
    }

    @Transactional
    public void apagarExtra(Long id) {
        ExtraEventoEntity extra = buscar(id);
        lancamentoRepository.findByExtraId(id).ifPresent(lancamentoRepository::delete);
        extraRepository.delete(extra);
    }

    /** Categoria de extras (nome com "extra"); sem ela, a do contrato; sem contrato, a receita padrão do tipo do evento. */
    private CategoriaFinanceiraEntity categoriaDoExtra(EventoEntity evento) {
        Dados d = dadosService.carregar();
        Long id = Caixa.categoriasOrdenadas(d).stream().filter(c -> "receita".equals(c.grupo()) && c.nome().toLowerCase().contains("extra"))
            .map(CategoriaResponseDTO::id).findFirst().orElse(null);
        if (id == null && GestaoMapper.temContrato(evento) && evento.getContrato().getCategoria() != null) {
            id = evento.getContrato().getCategoria().getId();
        }
        if (id == null) {
            id = Caixa.receitaPadrao(d, evento.getTipo());
        }
        if (id == null) {
            throw new AppException(SEM_CATEGORIA, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return cadastro.categoriaDoCorpo(id, SEM_CATEGORIA);
    }

    private void aplicar(ExtraEventoRequestDTO req, ExtraEventoEntity extra) {
        extra.setDescricao(req.descricao().trim());
        extra.setQuantidade(req.quantidade());
        extra.setValorUnitario(req.valorUnitario());
    }

    private static String descricao(ExtraEventoRequestDTO req, EventoEntity evento) {
        return "Extra · " + req.descricao().trim() + " · " + evento.getNome();
    }

    private static BigDecimal total(ExtraEventoRequestDTO req) {
        return req.valorUnitario().multiply(BigDecimal.valueOf(req.quantidade())).setScale(2, RoundingMode.HALF_UP);
    }

    private ExtraEventoEntity buscar(Long id) {
        return extraRepository.findById(id).orElseThrow(() -> new AppException("O extra de id " + id + " não existe.", HttpStatus.NOT_FOUND));
    }
}
