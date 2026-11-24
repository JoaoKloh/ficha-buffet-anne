package com.jbkloh.ficha_buffet.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.req.AdicionarItensPlanoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AjustarItemPlanoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CalcularCartaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarPlanoRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Validações dos DTOs dos planos de produção, alinhadas aos contratos Zod do frontend (lib/models/planoProducao.ts). */
class PlanoProducaoDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> mensagens(Object dto) {
        return validator.validate(dto).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private static SalvarPlanoRequestDTO plano(String titulo, String unidade, String status, String inicio, String fim) {
        return new SalvarPlanoRequestDTO(titulo, unidade, status, inicio, fim, "");
    }

    @Test
    void plano_exigeNomeUnidadeSituacaoEPeriodo() {
        assertThat(mensagens(plano("Semana", "restaurante", null, "2026-11-20", "2026-11-26"))).isEmpty();
        assertThat(mensagens(plano("Natal", "chocolates", "em_producao", "2026-12-01", "2026-12-01"))).isEmpty();
        assertThat(mensagens(plano(" ", "restaurante", null, "2026-11-20", "2026-11-26"))).containsExactly("Dê um nome ao plano.");
        assertThat(mensagens(plano("Semana", "buffet", null, "2026-11-20", "2026-11-26"))).containsExactly("Unidade inválida.");
        assertThat(mensagens(plano("Semana", "restaurante", "feito", "2026-11-20", "2026-11-26"))).containsExactly("Situação do plano inválida.");
        assertThat(mensagens(plano("Semana", "restaurante", null, "", "2026-11-26"))).containsExactly("Informe a data de início.");
        assertThat(mensagens(plano("Semana", "restaurante", null, "2026-11-20", "2026-11-19"))).containsExactly("A data final não pode ser antes do início.");
    }

    @Test
    void itens_exigemAoMenosUmComFichaEQuantidade() {
        assertThat(mensagens(new AdicionarItensPlanoRequestDTO(List.of(new AdicionarItensPlanoRequestDTO.Item(1L, 2.5))))).isEmpty();
        assertThat(mensagens(new AdicionarItensPlanoRequestDTO(List.of()))).containsExactly("Marque ao menos um item.");
        assertThat(mensagens(new AdicionarItensPlanoRequestDTO(List.of(new AdicionarItensPlanoRequestDTO.Item(1L, 0.0)))))
            .containsExactly("Informe a quantidade de todos os itens marcados.");
        assertThat(mensagens(new AjustarItemPlanoRequestDTO(0.0))).containsExactly("Informe uma quantidade maior que zero.");
    }

    @Test
    void carta_exigePratoPrevistoComChaveNumerica() {
        assertThat(mensagens(new CalcularCartaRequestDTO(Map.of("12", 3, "13", 0)))).isEmpty();
        assertThat(mensagens(new CalcularCartaRequestDTO(Map.of("12", 0)))).containsExactly("Informe ao menos um prato previsto.");
        assertThat(mensagens(new CalcularCartaRequestDTO(Map.of("abc", 2)))).containsExactly("Prato da carta inválido.");
        assertThat(mensagens(new CalcularCartaRequestDTO(Map.of("12", -1)))).contains("Quantidade de pratos inválida.");
    }
}
