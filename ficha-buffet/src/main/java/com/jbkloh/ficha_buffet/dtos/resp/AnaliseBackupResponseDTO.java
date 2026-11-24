package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

public record AnaliseBackupResponseDTO(String exportadoEm, List<Linha> linhas, List<String> ignoradas, int invalidos) {

    public record Linha(String colecao, String rotulo, int total, int novos, int diferentes, int iguais, int soNoSistema) {}
}
