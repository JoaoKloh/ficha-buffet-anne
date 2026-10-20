package com.jbkloh.ficha_buffet.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.req.CreateOrcamentoRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class OrcamentoDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> camposInvalidos(CreateOrcamentoRequest dto) {
        return validator.validate(dto).stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(Object::toString)
            .collect(Collectors.toSet());
    }

    @Test
    void orcamentoValido_naoTemViolacoes() {
        CreateOrcamentoRequest dto = new CreateOrcamentoRequest(
            "Maria", "", "", 0.0, 100,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("10.99"), new BigDecimal("0.11")
        );
        assertThat(camposInvalidos(dto)).isEmpty();
    }

    @Test
    void camposObrigatoriosAusentes_saoRejeitados() {
        CreateOrcamentoRequest dto = new CreateOrcamentoRequest(
            " ", null, null, null, null, null, null, null, null, null, null
        );
        assertThat(camposInvalidos(dto)).containsExactlyInAnyOrder(
            "nomeCliente", "duracaoEvento", "numeroConvidados", "valorAlimentos", "valorEquipe",
            "valorDegustacao", "valorOutros", "valorTotal", "valorPorPessoa"
        );
    }

    @Test
    void valoresNegativosOuComMaisDeDuasCasas_saoRejeitados() {
        CreateOrcamentoRequest dto = new CreateOrcamentoRequest(
            "Maria", null, null, -1.0, 0,
            new BigDecimal("-1"), new BigDecimal("1.999"), BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO
        );
        assertThat(camposInvalidos(dto)).containsExactlyInAnyOrder(
            "duracaoEvento", "numeroConvidados", "valorAlimentos", "valorEquipe"
        );
    }
}
