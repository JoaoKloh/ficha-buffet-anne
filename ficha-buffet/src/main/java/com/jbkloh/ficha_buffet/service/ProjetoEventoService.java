package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.SalvarLogisticaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarOrcamentoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FechamentoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LogisticaEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OrcamentoEventoDTO;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.FechamentoEventoEntity;
import com.jbkloh.ficha_buffet.model.LogisticaEventoEntity;
import com.jbkloh.ficha_buffet.model.OrcamentoEventoEntity;
import com.jbkloh.ficha_buffet.model.json.CustosPorTipo;
import com.jbkloh.ficha_buffet.repositories.FechamentoEventoRepository;
import com.jbkloh.ficha_buffet.repositories.LogisticaEventoRepository;
import com.jbkloh.ficha_buffet.repositories.OrcamentoEventoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Evento como projeto: plano de logística, orçamento dos custos fora das fichas técnicas e o
 * retrato guardado ao fechar o evento. Cada um é uma linha por evento, criada ao salvar; sem
 * linha, a ficha recebe o plano vazio. O fechamento em si (etapa Acertado) fica no EventoService,
 * que tem os indicadores do evento.
 */
@Service
@RequiredArgsConstructor
public class ProjetoEventoService {

    private final LogisticaEventoRepository logisticaRepository;
    private final OrcamentoEventoRepository orcamentoRepository;
    private final FechamentoEventoRepository fechamentoRepository;
    private final Cadastro cadastro;

    @Transactional(readOnly = true)
    public LogisticaEventoDTO logistica(Long eventoId) {
        return logisticaRepository.findByEventoId(eventoId).map(GestaoMapper::logistica).orElse(LogisticaEventoDTO.VAZIA);
    }

    @Transactional(readOnly = true)
    public OrcamentoEventoDTO orcamento(Long eventoId) {
        return orcamentoRepository.findByEventoId(eventoId).map(GestaoMapper::orcamento).orElse(OrcamentoEventoDTO.VAZIO);
    }

    /** Retrato do fechamento; nulo se o evento nunca foi fechado. */
    @Transactional(readOnly = true)
    public FechamentoEventoDTO fechamento(Long eventoId) {
        return fechamentoRepository.findByEventoId(eventoId).map(GestaoMapper::fechamento).orElse(null);
    }

    /** Substitui o plano de logística do evento. */
    @Transactional
    public void salvarLogistica(Long eventoId, SalvarLogisticaRequestDTO req) {
        EventoEntity evento = cadastro.evento(eventoId);
        LogisticaEventoEntity l = logisticaRepository.findByEventoId(eventoId).orElseGet(() -> {
            LogisticaEventoEntity nova = new LogisticaEventoEntity();
            nova.setEvento(evento);
            return nova;
        });
        l.setVeiculos(Formato.texto(req.veiculos()).trim());
        l.setMotorista(Formato.texto(req.motorista()).trim());
        l.setViagens(Formato.texto(req.viagens()).trim());
        l.setSaida(Formato.horaOpcional(req.saida()));
        l.setMontagem(Formato.horaOpcional(req.montagem()));
        l.setRetorno(Formato.horaOpcional(req.retorno()));
        l.setObs(Formato.texto(req.obs()).trim());
        l.setAtualizadoEm(Instant.now());
        l.setAtualizadoPor(Usuario.atual());
        logisticaRepository.save(l);
    }

    /** Grava transporte e outros; a equipe só muda quando vem no corpo (nula = vem da escala, mantém o valor gravado). */
    @Transactional
    public void salvarOrcamento(Long eventoId, SalvarOrcamentoRequestDTO req) {
        EventoEntity evento = cadastro.evento(eventoId);
        OrcamentoEventoEntity o = orcamentoRepository.findByEventoId(eventoId).orElseGet(() -> {
            OrcamentoEventoEntity novo = new OrcamentoEventoEntity();
            novo.setEvento(evento);
            return novo;
        });
        if (req.equipe() != null) {
            o.setEquipe(req.equipe());
        }
        o.setTransporte(req.transporte());
        o.setOutros(req.outros());
        o.setAtualizadoEm(Instant.now());
        o.setAtualizadoPor(Usuario.atual());
        orcamentoRepository.save(o);
    }

    /** Grava o retrato do fechamento, substituindo o de um fechamento anterior. */
    @Transactional
    public void gravarFechamento(EventoEntity evento, FechamentoEventoDTO retrato) {
        FechamentoEventoEntity f = fechamentoRepository.findByEventoId(evento.getId()).orElseGet(() -> {
            FechamentoEventoEntity novo = new FechamentoEventoEntity();
            novo.setEvento(evento);
            return novo;
        });
        f.setFechadoEm(Instant.parse(retrato.em()));
        f.setFechadoPor(Formato.texto(retrato.por()));
        f.setReceita(BigDecimal.valueOf(retrato.receita()));
        f.setRecebido(BigDecimal.valueOf(retrato.recebido()));
        f.setCustoPrevisto(BigDecimal.valueOf(retrato.custoPrevisto()));
        f.setCustoRealizado(BigDecimal.valueOf(retrato.custoRealizado()));
        f.setResultado(BigDecimal.valueOf(retrato.resultado()));
        f.setCmv(retrato.cmv() == null ? null : BigDecimal.valueOf(retrato.cmv()));
        f.setPorTipo(new CustosPorTipo(GestaoMapper.custos(retrato.porTipo().previsto()), GestaoMapper.custos(retrato.porTipo().realizado())));
        f.setPendencias(new ArrayList<>(retrato.pendencias()));
        fechamentoRepository.save(f);
    }

    /** Tira logística, orçamento e retrato do evento (exclusão do evento). */
    @Transactional
    public void apagarDoEvento(Long eventoId) {
        logisticaRepository.findByEventoId(eventoId).ifPresent(logisticaRepository::delete);
        orcamentoRepository.findByEventoId(eventoId).ifPresent(orcamentoRepository::delete);
        fechamentoRepository.findByEventoId(eventoId).ifPresent(fechamentoRepository::delete);
    }
}
