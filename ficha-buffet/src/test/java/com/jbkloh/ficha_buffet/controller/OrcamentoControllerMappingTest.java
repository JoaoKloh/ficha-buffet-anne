package com.jbkloh.ficha_buffet.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
class OrcamentoControllerMappingTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void endpointsDeOrcamentoEstaoRegistrados() {
        Set<String> rotas = handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(e -> e.getValue().getBeanType().equals(OrcamentoController.class))
            .map(e -> e.getKey().getMethodsCondition().getMethods() + " " + e.getKey().getPatternValues())
            .collect(Collectors.toSet());

        assertThat(rotas).containsExactlyInAnyOrder(
            "[POST] [/orcamento/criar]",
            "[GET] [/orcamento/retornarTodos]",
            "[DELETE] [/orcamento/apagar/{id}]"
        );
    }
}
