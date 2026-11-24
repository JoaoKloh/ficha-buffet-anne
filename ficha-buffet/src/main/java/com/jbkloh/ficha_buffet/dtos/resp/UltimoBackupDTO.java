package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.Map;

public record UltimoBackupDTO(String ultimoEm, String tipo, Map<String, Integer> contagens, int total) {}
