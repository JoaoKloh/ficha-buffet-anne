package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Uso ou descarte de parte de um lote. No uso em evento, eventoId diz qual: a ordem e
 * o checklist desse evento deixam de baixar os insumos dessa quantidade.
 */
public record MovimentarLoteRequestDTO(
    @NotBlank(message = "Informe se é uso ou descarte.")
    @Pattern(regexp = "uso|descarte", message = "O tipo deve ser uso ou descarte.")
    String tipo,
    @NotNull(message = "Informe a quantidade.")
    @Positive(message = "Informe a quantidade.")
    Double qtd,
    @NotBlank(message = "Informe a data.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data.")
    String data,
    @NotBlank(message = "Informe o destino ou o motivo.")
    @Pattern(regexp = Dominio.RE_PARA_LOTE, message = "Destino ou motivo inválido.")
    String para,
    @Positive(message = "Evento inválido.")
    Long eventoId,
    @Size(max = 300, message = "A observação deve ter no máximo 300 caracteres.")
    String obs
) {
    @AssertTrue(message = "Escolha o evento.")
    public boolean isEventoDoUsoInformado() {
        return !("uso".equals(tipo) && "evento".equals(para)) || eventoId != null;
    }
}
