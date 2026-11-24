package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Ação da equipe numa ordem de produção (a ação vem na rota). quem = nome de quem registra,
 * gravado no histórico. Cada ação usa só os seus campos: iniciar (resp), concluir (qtd, perda, obs),
 * bloquear (motivo, texto), responsavel (resp).
 */
public record AcaoOrdemRequestDTO(
    @NotNull(message = "Informe o evento.") @Positive(message = "Evento inválido.") Long eventoId,
    @NotBlank(message = "Informe a ordem.") @Size(max = 40, message = "Ordem inválida.") String k,
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.") String quem,
    @Size(max = 80, message = "O responsável deve ter no máximo 80 caracteres.") String resp,
    @Positive(message = "Informe a quantidade produzida.") Double qtd,
    @PositiveOrZero(message = "A perda não pode ser negativa.") Double perda,
    @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.") String obs,
    @Pattern(regexp = Dominio.RE_MOTIVO_BLOQUEIO, message = "Motivo do bloqueio inválido.") String motivo,
    @Size(max = 500, message = "A descrição do impedimento deve ter no máximo 500 caracteres.") String texto
) {}
