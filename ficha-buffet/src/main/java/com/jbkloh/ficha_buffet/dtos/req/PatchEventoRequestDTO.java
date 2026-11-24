package com.jbkloh.ficha_buffet.dtos.req;

import java.util.List;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Campos do evento que mudam sozinhos, fora do formulário: só os informados são alterados. */
public record PatchEventoRequestDTO(
    @Pattern(regexp = Dominio.RE_STATUS_EVENTO, message = "Etapa do evento inválida.")
    String status,
    @DecimalMin(value = "0", message = "A margem não pode ser negativa.")
    @DecimalMax(value = "100", message = "Use uma margem entre 0 e 100%.")
    Double margem,
    Boolean cardapioFechado,
    List<@Valid @NotNull EnvioExtraItem> envioExtras
) {
    /** Item de serviço, prataria ou descartável do checklist de envio. */
    public record EnvioExtraItem(
        @Size(max = 60, message = "Chave do item inválida.") String k,
        @NotBlank(message = "Escolha a seção do item.") @Pattern(regexp = Dominio.RE_SECAO_ENVIO_EXTRA, message = "Seção do item inválida.") String sec,
        @NotBlank(message = "Informe o nome do item.") @Size(max = 200, message = "O nome do item deve ter no máximo 200 caracteres.") String n,
        @NotBlank(message = "Escolha a unidade do item.") @Pattern(regexp = Dominio.RE_UNIDADE_ENVIO_EXTRA, message = "Unidade do item inválida.") String u,
        @NotBlank(message = "Escolha como a quantidade é calculada.") @Pattern(regexp = Dominio.RE_MODO_ENVIO_EXTRA, message = "Modo de cálculo inválido.") String modo,
        @NotNull(message = "Informe a quantidade do item.") @PositiveOrZero(message = "A quantidade não pode ser negativa.") Double v,
        @Size(max = 300, message = "A observação deve ter no máximo 300 caracteres.") String obs
    ) {}
}
