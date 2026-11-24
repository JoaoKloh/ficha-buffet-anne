package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

public record BackupResumoResponseDTO(UltimoBackupDTO ultimo, List<Contagem> contagens, int total) {

    public record Contagem(String colecao, String rotulo, int n) {}
}
