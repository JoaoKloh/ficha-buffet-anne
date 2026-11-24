package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Dados lidos de um XML de NF-e, para pré-preencher a nota. */
public record NotaFiscalImportadaDTO(String chave, String numero, String serie, String emissao, String fornecedor, String razaoSocial,
        String cnpjFornecedor, List<NotaFiscalResponseDTO.Item> itens, double total, double acrescimo, String formaPag,
        List<NotaFiscalResponseDTO.Parcela> parcelas, @JsonProperty("importadaXML") boolean importadaXML, boolean importadaFoto, List<String> duvidas,
        Long duplicadaDe) {}
