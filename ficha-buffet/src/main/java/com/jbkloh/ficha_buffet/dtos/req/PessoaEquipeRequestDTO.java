package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Pessoa na escala do evento. Na criação, equipeId escolhe alguém já cadastrado na equipe
 * (o nome do corpo é ignorado e o cadastro mantém o seu);
 * sem ele, a pessoa é procurada pelo nome (sem diferenciar acentos, maiúsculas e espaços) e,
 * se não existir, entra no cadastro. Nome, contato e Pix são da pessoa; o resto, da escala.
 */
public record PessoaEquipeRequestDTO(
    @Positive(message = "Pessoa da equipe inválida.")
    Long equipeId,
    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    String nome,
    @NotBlank(message = "Informe a função.")
    @Pattern(regexp = Dominio.RE_FUNCAO_EQUIPE, message = "Função da equipe inválida.")
    String funcao,
    @Pattern(regexp = Dominio.RE_HORA_OU_VAZIA, message = "Horário de chegada inválido.")
    String chegada,
    @Pattern(regexp = Dominio.RE_HORA_OU_VAZIA, message = "Horário de saída inválido.")
    String saida,
    @NotNull(message = "Informe a diária.")
    @PositiveOrZero(message = "A diária não pode ser negativa.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valor,
    @Size(max = 40, message = "O telefone deve ter no máximo 40 caracteres.")
    String contato,
    @Size(max = 120, message = "A chave Pix deve ter no máximo 120 caracteres.")
    String pix
) {}
