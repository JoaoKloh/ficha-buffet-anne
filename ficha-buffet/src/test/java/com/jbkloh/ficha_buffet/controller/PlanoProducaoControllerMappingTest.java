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

/** Rotas dos planos de produção nos caminhos que o BFF (lib/server/gestao/planoProducaoBackend.ts) chama. */
@SpringBootTest
class PlanoProducaoControllerMappingTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void endpointsDosPlanosEstaoRegistrados() {
        Set<String> rotas = handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(e -> e.getValue().getBeanType().equals(PlanoProducaoController.class))
            .map(e -> e.getKey().getMethodsCondition().getMethods() + " " + e.getKey().getPatternValues())
            .collect(Collectors.toSet());

        assertThat(rotas).containsExactlyInAnyOrder(
            "[GET] [/planosProducao/retornarTodos]",
            "[GET] [/planosProducao/detalhar/{id}]",
            "[POST] [/planosProducao/criar]",
            "[PUT] [/planosProducao/atualizar/{id}]",
            "[DELETE] [/planosProducao/apagar/{id}]",
            "[POST] [/planosProducao/itens/adicionar/{id}]",
            "[PATCH] [/planosProducao/itens/ajustar/{id}/{k}]",
            "[DELETE] [/planosProducao/itens/apagar/{id}/{k}]",
            "[POST] [/planosProducao/itens/concluir/{id}/{k}]",
            "[POST] [/planosProducao/itens/desfazer/{id}/{k}]",
            "[POST] [/planosProducao/calcularCarta/{id}]"
        );
    }
}
