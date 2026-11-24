package com.jbkloh.ficha_buffet.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.req.CriarOcorrenciaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ResolverOcorrenciaRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Validações dos DTOs das ocorrências da cozinha, alinhadas aos contratos Zod do frontend. */
class CozinhaDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> mensagens(Object dto) {
        return validator.validate(dto).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private static CriarOcorrenciaRequestDTO ocorrencia(String tipo, String quem, String texto, String acao) {
        return new CriarOcorrenciaRequestDTO(1L, tipo, quem, texto, acao);
    }

    @Test
    void ocorrencia_aceitaOsTiposDaTelaEExigeQuemEOQueAconteceu() {
        for (String tipo : new String[] { "Atraso", "Falta de insumo", "Temperatura", "Carregamento", "Equipamento ou utensílio", "Acidente",
            "Pedido do cliente", "Outro" }) {
            assertThat(mensagens(ocorrencia(tipo, "Carla", "Algo aconteceu.", null))).isEmpty();
        }
        assertThat(mensagens(ocorrencia("atraso", "Carla", "Algo aconteceu.", ""))).containsExactly("Tipo de ocorrência inválido.");
        assertThat(mensagens(ocorrencia("Atraso", " ", "Algo aconteceu.", ""))).containsExactly("Informe quem registra.");
        assertThat(mensagens(ocorrencia("Atraso", "Carla", "", ""))).containsExactly("Descreva o que aconteceu.");
        assertThat(mensagens(ocorrencia("Atraso", "x".repeat(81), "Algo aconteceu.", ""))).containsExactly("O nome deve ter no máximo 80 caracteres.");
        assertThat(mensagens(ocorrencia("Atraso", "Carla", "Algo aconteceu.", "x".repeat(2001))))
            .containsExactly("O que foi feito deve ter no máximo 2000 caracteres.");
        assertThat(mensagens(new CriarOcorrenciaRequestDTO(null, "Atraso", "Carla", "Algo aconteceu.", ""))).containsExactly("Informe o evento.");
    }

    @Test
    void resolver_exigeOValor() {
        assertThat(mensagens(new ResolverOcorrenciaRequestDTO(true))).isEmpty();
        assertThat(mensagens(new ResolverOcorrenciaRequestDTO(null))).containsExactly("Informe se a ocorrência está resolvida.");
    }
}
