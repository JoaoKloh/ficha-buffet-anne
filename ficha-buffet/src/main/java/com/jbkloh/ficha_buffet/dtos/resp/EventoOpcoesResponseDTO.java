package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Opções dos formulários do evento. Pares são [id, rótulo]. Cada formulário recebe
 * só os campos que usa; os demais ficam nulos e não entram no JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventoOpcoesResponseDTO(List<List<Object>> clientes, List<List<Object>> categoriasReceita, List<GrupoOpcoesDTO> categoriasTodas,
        List<GrupoOpcoesDTO> preparacoes, Map<Long, PreparacaoInfo> preparacoesInfo, List<List<Object>> eventos, Map<Long, String> eventosData,
        List<EventoComCardapio> eventosComCardapio) {

    public record PreparacaoInfo(double porcaoPadrao, String praca, String unidadeRendimento, double rendimento) {}

    public record EventoComCardapio(Long id, String rotulo) {}
}
