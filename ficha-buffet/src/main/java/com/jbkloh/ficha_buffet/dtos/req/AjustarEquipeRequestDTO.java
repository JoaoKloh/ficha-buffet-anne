package com.jbkloh.ficha_buffet.dtos.req;

import java.math.BigDecimal;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Edição do cadastro da pessoa da equipe: só os campos informados mudam. Função e diária padrão
 * valem para as próximas escalas; as escalas existentes mantêm o que foi combinado em cada evento.
 */
public record AjustarEquipeRequestDTO(
    @Pattern(regexp = "(?s).*\\S.*", message = "Informe o nome.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    String nome,
    @Pattern(regexp = Dominio.RE_FUNCAO_EQUIPE, message = "Função da equipe inválida.")
    String funcaoPadrao,
    @PositiveOrZero(message = "A diária não pode ser negativa.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal diariaPadrao,
    @Size(max = 40, message = "O telefone deve ter no máximo 40 caracteres.")
    String contato,
    @Size(max = 120, message = "A chave Pix deve ter no máximo 120 caracteres.")
    String pix
) {
    @AssertTrue(message = "Nada a ajustar.")
    public boolean isAlgoAAjustar() {
        return nome != null || funcaoPadrao != null || diariaPadrao != null || contato != null || pix != null;
    }
}
