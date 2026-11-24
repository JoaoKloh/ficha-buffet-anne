package com.jbkloh.ficha_buffet.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.jbkloh.ficha_buffet.dtos.req.MarcarPedidoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentarLoteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentoEstoqueRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Validações dos DTOs do estoque, alinhadas aos contratos Zod do frontend. */
class EstoqueDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> mensagens(Object dto) {
        return validator.validate(dto).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private static MovimentoEstoqueRequestDTO movimento(String tipo, String validade, String unidadeUso, String compradoPor) {
        return new MovimentoEstoqueRequestDTO(tipo, 1L, "2026-11-19", 2.0, null, null, "", validade, unidadeUso, compradoPor);
    }

    @Test
    void movimento_consumoExigeUnidadeQueUsouEDiferenteDeQuemComprou() {
        assertThat(mensagens(movimento("consumo", null, "restaurante", "buffet"))).isEmpty();
        assertThat(mensagens(movimento("consumo", null, "restaurante", null))).isEmpty();
        assertThat(mensagens(movimento("consumo", null, null, null))).containsExactly("Escolha a unidade que usou.");
        assertThat(mensagens(movimento("consumo", null, "buffet", "buffet")))
            .containsExactly("Quem comprou e quem usou são a mesma unidade: deixe \"A mesma unidade\".");
        assertThat(mensagens(movimento("consumo", null, "compartilhado", null))).containsExactly("Unidade que usou inválida.");
        assertThat(mensagens(movimento("consumo", null, "restaurante", "outra"))).containsExactly("Unidade que comprou inválida.");
    }

    @Test
    void movimento_validadeNoFormatoDeData() {
        assertThat(mensagens(movimento("entrada", "2026-12-01", null, null))).isEmpty();
        assertThat(mensagens(movimento("entrada", "01/12/2026", null, null))).containsExactly("Validade inválida.");
        assertThat(mensagens(movimento("baixa", null, null, null))).containsExactly("Tipo de movimento inválido.");
    }

    @Test
    void usoDeLoteEmEventoExigeOEvento() {
        assertThat(mensagens(new MovimentarLoteRequestDTO("uso", 1.0, "2026-11-19", "evento", 3L, ""))).isEmpty();
        assertThat(mensagens(new MovimentarLoteRequestDTO("uso", 1.0, "2026-11-19", "evento", null, ""))).containsExactly("Escolha o evento.");
        assertThat(mensagens(new MovimentarLoteRequestDTO("uso", 1.0, "2026-11-19", "restaurante", null, ""))).isEmpty();
        assertThat(mensagens(new MovimentarLoteRequestDTO("descarte", 1.0, "2026-11-19", "vencido", null, ""))).isEmpty();
    }

    @Test
    void marcarPedido_exigeItemEMarcacao() {
        assertThat(mensagens(new MarcarPedidoRequestDTO(1L, true))).isEmpty();
        assertThat(mensagens(new MarcarPedidoRequestDTO(null, null))).containsExactlyInAnyOrder("Id obrigatório.", "Informe se o pedido foi feito.");
        assertThat(mensagens(new MarcarPedidoRequestDTO(0L, false))).containsExactly("Id obrigatório.");
    }
}
