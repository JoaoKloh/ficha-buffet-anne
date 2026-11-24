package com.jbkloh.ficha_buffet.dtos.req;

import java.util.Map;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

/** Conferências (item -> marcado) e horário de saída da liberação: só o que vier muda. */
public record SalvarLiberacaoRequestDTO(
    Map<String, Boolean> ck,
    @Pattern(regexp = Dominio.RE_HORA_OU_VAZIA, message = "Horário inválido.") String horaSaida
) {
    @AssertTrue(message = "Nada a salvar.")
    public boolean isAlgoASalvar() {
        return (ck != null && !ck.isEmpty()) || horaSaida != null;
    }

    @AssertTrue(message = "Conferência inválida.")
    public boolean isConferenciasValidas() {
        return ck == null || ck.entrySet().stream().allMatch(e -> Dominio.ITENS_LIBERACAO.containsKey(e.getKey()) && e.getValue() != null);
    }
}
