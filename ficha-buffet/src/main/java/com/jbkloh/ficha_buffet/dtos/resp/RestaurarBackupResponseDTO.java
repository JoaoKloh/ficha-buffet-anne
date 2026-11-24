package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

public record RestaurarBackupResponseDTO(int total, int gravados, List<Falha> falhas, int conferidos) {

    public record Falha(String colecao, String rotulo, String id) {}
}
