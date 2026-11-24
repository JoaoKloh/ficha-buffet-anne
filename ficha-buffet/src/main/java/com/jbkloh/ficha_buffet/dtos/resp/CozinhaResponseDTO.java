package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/**
 * Tela da cozinha: projeções dos eventos, execuções do checklist, tarefas e as ordens de produção
 * (com a liberação para expedição de cada evento com mapa gerado e os setores da cozinha), e as
 * ocorrências registradas nos eventos projetados.
 */
public record CozinhaResponseDTO(List<CozinhaProjecaoDTO> projecoes, List<ExecucaoResponseDTO> execucoes, List<TarefaCozinhaDTO> tarefas,
        List<OrdemProducaoDTO> ordens, List<LiberacaoExpedicaoDTO> liberacoes, List<OcorrenciaDTO> ocorrencias, List<SetorProducaoDTO> setores) {}
