package com.jbkloh.ficha_buffet.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.req.SalvarLogisticaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarOrcamentoRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Validações da logística e do orçamento do evento, alinhadas a SalvarLogisticaRequestSchema e SalvarOrcamentoRequestSchema do frontend. */
class ProjetoEventoDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> mensagens(Object dto) {
        return validator.validate(dto).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void logistica_horariosHHMMOuVaziosETamanhos() {
        assertThat(mensagens(new SalvarLogisticaRequestDTO("Van", "Mauricio", "2", "09:00", "", "23:59", ""))).isEmpty();
        assertThat(mensagens(new SalvarLogisticaRequestDTO(null, null, null, null, null, null, null))).isEmpty();
        assertThat(mensagens(new SalvarLogisticaRequestDTO("", "", "", "9h", "24:00", "12:5", "")))
            .containsExactlyInAnyOrder("Horário de saída inválido.", "Horário de montagem inválido.", "Horário de retorno inválido.");
        assertThat(mensagens(new SalvarLogisticaRequestDTO("v".repeat(201), "m".repeat(121), "x".repeat(121), "", "", "", "o".repeat(2001))))
            .containsExactlyInAnyOrder("Veículos deve ter no máximo 200 caracteres.", "Motorista deve ter no máximo 120 caracteres.",
                "Viagens deve ter no máximo 120 caracteres.", "As observações devem ter no máximo 2000 caracteres.");
    }

    @Test
    void orcamento_equipeOpcionalEValoresComDuasCasasNaoNegativos() {
        assertThat(mensagens(new SalvarOrcamentoRequestDTO(null, new BigDecimal("3000.00"), BigDecimal.ZERO))).isEmpty();
        assertThat(mensagens(new SalvarOrcamentoRequestDTO(new BigDecimal("500.50"), BigDecimal.ONE, BigDecimal.ONE))).isEmpty();
        assertThat(mensagens(new SalvarOrcamentoRequestDTO(null, null, null))).containsExactlyInAnyOrder("Informe o transporte.", "Informe locação e outros.");
        assertThat(mensagens(new SalvarOrcamentoRequestDTO(new BigDecimal("-1"), new BigDecimal("1.234"), BigDecimal.ZERO)))
            .containsExactlyInAnyOrder("O valor não pode ser negativo.", "O valor deve ter no máximo 2 casas decimais.");
    }
}
