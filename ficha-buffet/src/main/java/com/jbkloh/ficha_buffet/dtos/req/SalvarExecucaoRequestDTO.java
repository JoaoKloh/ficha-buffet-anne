package com.jbkloh.ficha_buffet.dtos.req;

import java.util.Map;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Gravação parcial do que a cozinha marcou: só os campos tocados, mesclados ao registro atual. */
public record SalvarExecucaoRequestDTO(
    @NotBlank(message = "A chave do registro é obrigatória.")
    @Size(max = 120, message = "Chave do registro inválida.")
    String chave,
    @NotBlank(message = "O tipo do registro é obrigatório.")
    @Pattern(regexp = Dominio.RE_KIND_EXECUCAO, message = "Tipo de registro inválido.")
    String kind,
    @NotNull(message = "O evento é obrigatório.")
    @Positive(message = "Evento inválido.")
    Long eventoId,
    @Positive(message = "Prato do cardápio inválido.")
    Long cardapioId,
    @Positive(message = "Item inválido.")
    Long insumoId,
    @Size(max = 60, message = "Referência do registro inválida.")
    String key,
    @NotNull(message = "Nenhum campo para gravar.")
    Map<String, Object> campos
) {}
