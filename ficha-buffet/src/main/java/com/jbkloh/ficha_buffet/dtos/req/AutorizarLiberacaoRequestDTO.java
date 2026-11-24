package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Autoriza a saída do evento. pendencias = justificativa, obrigatória quando nem tudo está pronto. */
public record AutorizarLiberacaoRequestDTO(
    @NotBlank(message = "Informe quem autoriza.") @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.") String quem,
    @Size(max = 2000, message = "As pendências devem ter no máximo 2000 caracteres.") String pendencias
) {}
