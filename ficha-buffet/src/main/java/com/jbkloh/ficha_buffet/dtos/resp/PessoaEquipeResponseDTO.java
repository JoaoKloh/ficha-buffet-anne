package com.jbkloh.ficha_buffet.dtos.resp;

/**
 * Pessoa na escala do evento. id é a linha da escala (usado para editar e tirar da escala);
 * equipeId é a pessoa no cadastro da equipe. lancamentoId/lancamentoStatus: conta a pagar da
 * diária, quando lançada. pago: diária já paga no todo ou em parte (valor travado).
 */
public record PessoaEquipeResponseDTO(Long id, Long equipeId, String nome, String funcao, String chegada, String saida, double valor, String contato,
        String pix, Long lancamentoId, String lancamentoStatus, boolean pago) {}
