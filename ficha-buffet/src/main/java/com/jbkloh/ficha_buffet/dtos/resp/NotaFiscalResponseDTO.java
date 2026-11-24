package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Nota fiscal de compra. total, pago, movIds e lancIds são calculados. */
public record NotaFiscalResponseDTO(Long id, String fornecedor, String numero, String emissao, Long eventoId, boolean lancarCaixa, Long categoriaId,
        String formaPag, List<Parcela> parcelas, boolean pago, String status, List<Item> itens, double acrescimo, double total, String chave, String serie,
        String razaoSocial, String cnpjFornecedor, @JsonProperty("importadaXML") boolean importadaXML, boolean importadaFoto, List<Long> movIds,
        List<Long> lancIds, String lancadaEm, String criadoEm, String unidade) {

    public record Parcela(String venc, double valor, boolean pago) {}

    public record Item(String desc, String cod, @JsonProperty("uCom") String uCom, Long insumoId, double qtdNota, String unid, double qtd, Double valorUnit,
            double valor, boolean conferido) {}
}
