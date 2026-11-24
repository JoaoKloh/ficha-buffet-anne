package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Evento no contrato do Anne Gestão: com um cliente existente ou um novo, criado junto. */
public record EventoRequestDTO(
    @NotBlank(message = "Informe o nome do evento.")
    @Size(max = 200, message = "O nome do evento deve ter no máximo 200 caracteres.")
    String nome,
    @Positive(message = "Cliente inválido.")
    Long clienteId,
    @Valid
    NovoCliente novoCliente,
    @NotBlank(message = "Escolha o tipo do evento.")
    @Pattern(regexp = Dominio.RE_TIPO_EVENTO, message = "Tipo de evento inválido.")
    String tipo,
    @NotBlank(message = "Escolha a etapa do evento.")
    @Pattern(regexp = Dominio.RE_STATUS_EVENTO, message = "Etapa do evento inválida.")
    String status,
    // Vazia ou nula: data a combinar.
    @Pattern(regexp = Dominio.RE_DATA_OU_VAZIA, message = "Data inválida.")
    String dataEvento,
    @Pattern(regexp = Dominio.RE_HORA_OU_VAZIA, message = "Hora inválida: use HH:MM.")
    String horaInicio,
    @Size(max = 200, message = "O local deve ter no máximo 200 caracteres.")
    String local,
    @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
    String cidade,
    @Positive(message = "Os convidados previstos devem ser maior que zero.")
    Integer convidadosPrevistos,
    @NotNull(message = "Informe a margem de produção.")
    @DecimalMin(value = "0", message = "A margem não pode ser negativa.")
    @DecimalMax(value = "100", message = "Use uma margem entre 0 e 100%.")
    Double margem,
    @Size(max = 4000, message = "As observações devem ter no máximo 4000 caracteres.")
    String observacoes
) {
    @AssertTrue(message = "Escolha o cliente ou cadastre um novo.")
    public boolean isClienteInformado() {
        return clienteId != null || novoCliente != null;
    }

    public record NovoCliente(
        @NotBlank(message = "Informe o nome do novo cliente.")
        @Size(max = 200, message = "O nome do cliente deve ter no máximo 200 caracteres.")
        String nome,
        @Size(max = 40, message = "O telefone deve ter no máximo 40 caracteres.")
        String telefone
    ) {}
}
