package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cabeçalho do plano de produção (criar e editar). status nulo = planejado. */
public record SalvarPlanoRequestDTO(
    @NotBlank(message = "Dê um nome ao plano.") @Size(max = 160, message = "O nome do plano deve ter no máximo 160 caracteres.") String titulo,
    @NotBlank(message = "Escolha a unidade.") @Pattern(regexp = Dominio.RE_UNIDADE_PLANO, message = "Unidade inválida.") String unidade,
    @Pattern(regexp = Dominio.RE_STATUS_PLANO, message = "Situação do plano inválida.") String status,
    @NotBlank(message = "Informe a data de início.") @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data de início.") String dataInicio,
    @NotBlank(message = "Informe a data final.") @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data final.") String dataFim,
    @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.") String obs
) {
    @AssertTrue(message = "A data final não pode ser antes do início.")
    public boolean isPeriodoValido() {
        // Datas ausentes ou malformadas já têm a própria mensagem; ISO compara como texto.
        return dataInicio == null || dataFim == null || !dataInicio.matches(Dominio.RE_DATA) || !dataFim.matches(Dominio.RE_DATA)
            || dataFim.compareTo(dataInicio) >= 0;
    }
}
