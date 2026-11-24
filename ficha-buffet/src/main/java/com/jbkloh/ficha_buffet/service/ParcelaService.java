package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.ParcelaRequestDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.ParcelaEntity;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;
import com.jbkloh.ficha_buffet.repositories.ParcelaRepository;

import lombok.RequiredArgsConstructor;

/** Parcelas do contrato. Cada uma tem um lançamento espelho no caixa, mantido em sincronia. */
@Service
@RequiredArgsConstructor
public class ParcelaService {

    private final ParcelaRepository parcelaRepository;
    private final LancamentoRepository lancamentoRepository;

    /**
     * "Recebida por inteiro" completa o que falta com um recebimento na data informada;
     * desmarcar uma parcela que estava quitada desfaz os recebimentos dela. Recebimentos
     * em partes são registrados em Caixa › Contas a receber.
     */
    @Transactional
    public void atualizarParcela(Long id, ParcelaRequestDTO req) {
        ParcelaEntity parcela = buscar(id);
        LocalDate pagoEm = Formato.dataOpcional(req.pagoEm(), "data de pagamento");
        LocalDate vencimento = Formato.data(req.vencimento(), "vencimento");
        BigDecimal valor = req.valor().setScale(2, RoundingMode.HALF_UP);
        String conta = req.conta() == null ? "banco" : req.conta();
        String forma = pagoEm == null ? "" : Formato.texto(req.forma());
        LancamentoEntity l = lancamentoRepository.findByParcelaId(id).orElse(null);
        boolean estavaQuitada = parcela.getPagoEm() != null;
        parcela.setVencimento(vencimento);
        parcela.setValor(valor);
        if (l == null) {
            parcela.setPagoEm(pagoEm);
            parcela.setForma(forma);
            return;
        }
        double ja = CaixaService.pagoTotal(l).doubleValue();
        if (pagoEm != null) {
            if (ja > valor.doubleValue() + 0.005) {
                throw new AppException("Já foi recebido " + Formato.brl(ja) + ", mais que o novo valor.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (ja < valor.doubleValue() - 0.005) {
                CaixaService.novoPagamento(l, pagoEm, valor.subtract(BigDecimal.valueOf(ja)), conta, forma);
            }
        } else if (estavaQuitada) {
            l.getPagamentos().clear();
        } else if (ja > valor.doubleValue() + 0.005) {
            throw new AppException("Já foi recebido " + Formato.brl(ja) + ", mais que o novo valor.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        l.setValor(valor);
        l.setDataCompetencia(vencimento);
        l.setVencimento(vencimento);
        l.setConta(conta);
        CaixaService.sincronizar(l);
        CaixaService.auditar(l);
        if (pagoEm != null && "realizado".equals(l.getStatus())) {
            parcela.setForma(forma);
        }
    }

    @Transactional
    public void apagarParcela(Long id) {
        ParcelaEntity parcela = buscar(id);
        lancamentoRepository.findByParcelaId(id).ifPresent(lancamentoRepository::delete);
        parcelaRepository.delete(parcela);
    }

    private ParcelaEntity buscar(Long id) {
        return parcelaRepository.findById(id).orElseThrow(() -> new AppException("A parcela de id " + id + " não existe.", HttpStatus.NOT_FOUND));
    }
}
