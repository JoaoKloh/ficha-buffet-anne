package com.jbkloh.ficha_buffet.dtos.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AnalisarBackupRequestDTO(
    @NotNull(message = "Este arquivo não é um backup do Anne Gestão.") @Valid BackupArquivoDTO arquivo
) {}
