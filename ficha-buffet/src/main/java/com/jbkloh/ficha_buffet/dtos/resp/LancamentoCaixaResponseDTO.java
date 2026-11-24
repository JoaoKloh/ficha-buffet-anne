package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/**
 * Lançamento do caixa. unidade é a gravada (nula = sem unidade); transferências
 * usam unidadeOrigem/unidadeDestino e os dados do item. pagamentos é o histórico
 * de pagamentos ou recebimentos, do mais antigo para o mais recente.
 */
public record LancamentoCaixaResponseDTO(Long id, String tipo, Long categoriaId, Long eventoId, String descricao, double valor, String dataCompetencia,
        String dataPagamento, String vencimento, String status, String origem, String conta, Long parcelaId, Long extraId, Long nfId, Integer parcelaNF,
        String formaPag, String fornecedor, String criadoEm, String unidade, String unidadeOrigem, String unidadeDestino, Long insumoId, String item,
        Double qtd, String unid, String responsavel, String obs, List<Pagamento> pagamentos, String criadoPor, String alteradoPor, String alteradoEm) {

    /** Um pagamento ou recebimento: em = quando foi registrado, por = quem registrou. */
    public record Pagamento(Long id, String data, double valor, String conta, String forma, String em, String por) {}
}
