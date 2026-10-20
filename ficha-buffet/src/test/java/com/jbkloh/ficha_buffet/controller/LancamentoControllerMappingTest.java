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
class LancamentoControllerMappingTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void endpointsDeLancamentoEstaoRegistrados() {
        Set<String> rotas = handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(e -> e.getValue().getBeanType().equals(LancamentoController.class))
            .map(e -> e.getKey().getMethodsCondition().getMethods() + " " + e.getKey().getPatternValues())
            .collect(Collectors.toSet());

        assertThat(rotas).containsExactlyInAnyOrder(
            "[POST] [/lancamentos/criar]",
            "[PUT] [/lancamentos/atualizar]",
            "[GET] [/lancamentos/retornarTodos]",
            "[DELETE] [/lancamentos/apagar/{id}]"
        );
    }
}
