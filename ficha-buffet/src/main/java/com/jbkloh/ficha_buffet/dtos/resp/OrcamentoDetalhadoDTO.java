package com.jbkloh.ficha_buffet.dtos.resp;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.model.OrcamentoEntity;

public record OrcamentoDetalhadoDTO(
    Long id,
    String nomeCliente,
    String contatoCliente,
    String tipoServico,
    Double duracaoEvento,
    Integer numeroConvidados,
    BigDecimal valorAlimentos,
    BigDecimal valorEquipe,
    BigDecimal valorDegustacao,
    BigDecimal valorOutros,
    BigDecimal valorTotal,
    BigDecimal valorPorPessoa
) {
    public OrcamentoDetalhadoDTO(OrcamentoEntity entity) {
        this(
            entity.getId(),
            entity.getNomeCliente(),
            entity.getContatoCliente(),
            entity.getTipoServico(),
            entity.getDuracaoEvento(),
            entity.getNumeroConvidados(),
            entity.getValorAlimentos(),
            entity.getValorEquipe(),
            entity.getValorDegustacao(),
            entity.getValorOutros(),
            entity.getValorTotal(),
            entity.getValorPorPessoa()
        );
    }
}
