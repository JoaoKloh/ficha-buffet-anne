package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Plano da gestão para uma ordem: prazo (dias antes do evento), horário limite, setor e responsável. */
public record PlanoOrdemRequestDTO(
    @NotBlank(message = "Informe a ordem.") @Size(max = 40, message = "Ordem inválida.") String k,
    @NotNull(message = "Escolha o prazo.") @Min(value = 0, message = "O prazo deve ser de 0 a 30 dias.") @Max(value = 30, message = "O prazo deve ser de 0 a 30 dias.")
    Integer dn,
    @Pattern(regexp = Dominio.RE_HORA_OU_VAZIA, message = "Horário inválido.") String hora,
    @NotBlank(message = "Escolha o setor.") @Size(max = 40, message = "Setor inválido.") String setor,
    @Size(max = 80, message = "O responsável deve ter no máximo 80 caracteres.") String resp
) {}
