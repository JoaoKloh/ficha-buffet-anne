package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.jbkloh.ficha_buffet.model.json.ComponenteEnvio;
import com.jbkloh.ficha_buffet.model.json.EtapaPreparacao;

/**
 * Ficha técnica (preparação). setor, dnProducao, tempoMin, equipamentos, armazenamento e
 * compradoPronto são os campos de produção usados nas ordens da cozinha.
 */
public record PreparacaoResponseDTO(Long id, String nome, String categoria, String praca, String conservacao, Integer validadeDias, String setor,
        Integer dnProducao, Integer tempoMin, String equipamentos, String armazenamento, boolean compradoPronto, double rendimento,
        String unidadeRendimento, double porcaoPadrao, List<Item> itens, List<Louca> loucas, List<EtapaPreparacao> etapas, List<ComponenteEnvio> componentes,
        String obsEnvio, String modoPreparo, String atualizadoEm) {

    /** Linha da receita: um insumo ou uma sub-receita, nunca os dois. */
    public record Item(Long insumoId, Long subId, double qtd, double fc) {}

    public record Louca(Long insumoId, double qtd) {}
}
