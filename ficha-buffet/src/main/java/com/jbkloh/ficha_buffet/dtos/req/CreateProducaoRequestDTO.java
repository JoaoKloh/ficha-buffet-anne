package com.jbkloh.ficha_buffet.dtos.req;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProducaoRequestDTO(

    @NotBlank(message = "O nome do evento/produção é obrigatório.")
    @Size(max = 255, message = "O nome do evento deve ter no máximo 255 caracteres.")
    String nome,

    @NotNull(message = "A quantidade de convidados é obrigatória.")
    @Positive(message = "A quantidade de convidados deve ser maior que zero.")
    Integer quantidade,

    @NotNull(message = "A data do evento é obrigatória.")
    @FutureOrPresent(message = "A data do evento não pode ser no passado.")
    LocalDate data,

    @NotEmpty(message = "A produção deve conter pelo menos um prato selecionado.")
    @Valid
    List<CreateProducaoItemRequestDTO> pratos
) {}