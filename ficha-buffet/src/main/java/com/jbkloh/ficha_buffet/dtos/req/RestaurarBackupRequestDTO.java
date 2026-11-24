package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** modo "faltantes" só cria o que não existe; "todos" também volta os registros alterados à versão do backup. */
public record RestaurarBackupRequestDTO(
    @Size(max = 120, message = "O nome do arquivo deve ter no máximo 120 caracteres.") String nomeArquivo,
    @NotBlank(message = "Escolha o que restaurar.") @Pattern(regexp = "faltantes|todos", message = "O modo deve ser faltantes ou todos.") String modo,
    @NotNull(message = "Este arquivo não é um backup do Anne Gestão.") @Valid BackupArquivoDTO arquivo
) {}
