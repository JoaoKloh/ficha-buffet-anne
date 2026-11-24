package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Marca ou desmarca uma etapa do evento (feito) ou dispensa/inclui a degustação (dispensada): só o que vier muda. */
public record AjustarEtapaRequestDTO(
    @NotBlank(message = "Informe a etapa.")
    @Pattern(regexp = Dominio.RE_ETAPA_EVENTO, message = "Etapa do evento inválida.")
    String etapa,
    Boolean feito,
    Boolean dispensada
) {
    @AssertTrue(message = "Nada a ajustar.")
    public boolean isAlgoAAjustar() {
        return feito != null || dispensada != null;
    }
}
