package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequestDTO(
    @NotBlank(message = "Informe o nome do cliente.")
    @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres.")
    String nome,
    @Pattern(regexp = Dominio.RE_TIPO_CLIENTE, message = "O tipo do cliente deve ser PF ou PJ.")
    String tipo,
    @Size(max = 30, message = "O documento deve ter no máximo 30 caracteres.")
    String documento,
    @Size(max = 40, message = "O telefone deve ter no máximo 40 caracteres.")
    String telefone,
    @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
    String email,
    @Pattern(regexp = Dominio.RE_ORIGEM_CLIENTE, message = "Origem do cliente inválida.")
    String origem
) {}
