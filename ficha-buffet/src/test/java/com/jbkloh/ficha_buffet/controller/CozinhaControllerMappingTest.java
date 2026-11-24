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

/** Rotas da cozinha nos caminhos que o BFF (lib/server/gestao/cozinhaBackend.ts) chama. */
@SpringBootTest
class CozinhaControllerMappingTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void endpointsDaCozinhaEstaoRegistrados() {
        Set<String> rotas = handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(e -> e.getValue().getBeanType().equals(CozinhaController.class))
            .map(e -> e.getKey().getMethodsCondition().getMethods() + " " + e.getKey().getPatternValues())
            .collect(Collectors.toSet());

        assertThat(rotas).containsExactlyInAnyOrder(
            "[GET] [/cozinha/retornarTudo]",
            "[POST] [/cozinha/execucao/salvar]",
            "[POST] [/cozinha/avaria/lancar]",
            "[POST] [/cozinha/ordens/{acao:iniciar|concluir|conferir|bloquear|desbloquear|reabrir|responsavel}]",
            "[POST] [/cozinha/mapa/gerar/{eventoId}]",
            "[PUT] [/cozinha/mapa/plano/{eventoId}]",
            "[PUT] [/cozinha/mapa/mestre/{eventoId}]",
            "[PUT] [/cozinha/mapa/liberar/{eventoId}]",
            "[PUT] [/cozinha/setores/salvar]",
            "[PUT] [/cozinha/liberacao/salvar/{eventoId}]",
            "[POST] [/cozinha/liberacao/autorizar/{eventoId}]",
            "[POST] [/cozinha/liberacao/desfazer/{eventoId}]",
            "[POST] [/cozinha/ocorrencias/criar]",
            "[PATCH] [/cozinha/ocorrencias/resolver/{id}]"
        );
    }
}
