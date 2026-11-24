package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Ficha do evento pronta para a tela. outros: lançamentos do evento que não são parcela nem extra. recebido conta as partes já recebidas. etapas: as 14 etapas do evento, na ordem. equipe: a escala do evento (pessoas do cadastro da equipe). indicadores/desvios: previsto × realizado do evento. projeto: logística, orçamento, assinaturas da cozinha, fechamento e pendências para fechar. */
public record EventoDetalheResponseDTO(EventoResponseDTO evento, String codigo, Cliente cliente, String prazoCardapio, Long diasPrazo,
        ResultadoEventoDTO resultado, List<ParcelaResponseDTO> parcelas, double recebido, double totalParcelas, List<ExtraResponseDTO> extras,
        List<LancamentoListaItemDTO> outros, CardapioEventoDTO cardapio, String travado, boolean contratoTemParcelaPaga, Long categoriaReceitaPadrao,
        List<EtapaEventoDTO> etapas, List<PessoaEquipeResponseDTO> equipe, IndicadoresEventoDTO indicadores, List<DesvioCustoDTO> desvios,
        ProjetoEventoDTO projeto) {

    public record Cliente(Long id, String nome, String contato) {}
}
