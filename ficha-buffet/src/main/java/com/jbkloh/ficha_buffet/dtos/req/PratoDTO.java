package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import com.jbkloh.ficha_buffet.model.PratoEntity;

public record PratoDTO(
    String nome,
    String categoria,
    String foto,
    Integer rendQtd,
    String rendUnid,
    Double porPessoa,
    String receita,
    List<IngredienteDTO> ingredientes
) {

    public static PratoEntity toEntity(PratoDTO dto) {
        PratoEntity prato = new PratoEntity();
        prato.setNome(dto.nome());
        prato.setCategoria(dto.categoria());
        prato.setUrlImagem(dto.foto());
        prato.setRendimentoReceita(dto.rendQtd());
        prato.setUnidade(dto.rendUnid());
        prato.setRendimentoPessoa(dto.porPessoa());
        prato.setDescricao(dto.receita());
        return prato;
    }
}