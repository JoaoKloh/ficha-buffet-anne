package com.jbkloh.ficha_buffet.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.req.CriarLancamentoDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class LancamentoDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> camposInvalidos(CriarLancamentoDTO dto) {
        return validator.validate(dto).stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(Object::toString)
            .collect(Collectors.toSet());
    }

    @Test
    void lancamentoValido_naoTemViolacoes() {
        CriarLancamentoDTO dto = new CriarLancamentoDTO(
            "despesa", "Bebidas", "Refrigerantes", new BigDecimal("10.99"), LocalDate.now()
        );
        assertThat(camposInvalidos(dto)).isEmpty();
    }

    @Test
    void camposObrigatoriosAusentes_saoRejeitados() {
        CriarLancamentoDTO dto = new CriarLancamentoDTO(null, " ", "", null, null);
        assertThat(camposInvalidos(dto)).containsExactlyInAnyOrder("tipo", "categoria", "descricao", "valor", "data");
    }

    @Test
    void tipoForaDoContrato_eRejeitado() {
        CriarLancamentoDTO dto = new CriarLancamentoDTO(
            "RECEITA", "Evento fechado", "Casamento", new BigDecimal("10"), LocalDate.now()
        );
        assertThat(camposInvalidos(dto)).containsExactly("tipo");
    }

    @Test
    void valorZeroNegativoOuComMaisDeDuasCasas_eRejeitado() {
        for (String valor : new String[] { "0", "-5", "10.999" }) {
            CriarLancamentoDTO dto = new CriarLancamentoDTO(
                "receita", "Evento fechado", "Casamento", new BigDecimal(valor), LocalDate.now()
            );
            assertThat(camposInvalidos(dto)).as("valor %s", valor).containsExactly("valor");
        }
    }
}
