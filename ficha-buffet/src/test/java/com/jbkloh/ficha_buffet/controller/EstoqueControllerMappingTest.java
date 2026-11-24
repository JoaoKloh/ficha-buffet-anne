package com.jbkloh.ficha_buffet.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.jbkloh.ficha_buffet.TestRsaKeys;

@SpringBootTest
class EstoqueControllerMappingTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void endpointsDeEstoqueEstaoRegistrados() {
        Set<String> rotas = handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(e -> e.getValue().getBeanType().equals(EstoqueController.class))
            .map(e -> e.getKey().getMethodsCondition().getMethods() + " " + e.getKey().getPatternValues())
            .collect(Collectors.toSet());

        assertThat(rotas).containsExactlyInAnyOrder(
            "[GET] [/estoque/opcoes]",
            "[GET] [/estoque/saldo]",
            "[GET] [/estoque/item/{id}]",
            "[GET] [/estoque/loucas]",
            "[GET] [/estoque/compras]",
            "[PUT] [/estoque/compras/marcarPedido]",
            "[GET] [/estoque/validades]",
            "[GET] [/estoque/perdas]",
            "[GET] [/estoque/conferencia]"
        );
    }
}
