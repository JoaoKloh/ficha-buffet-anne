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
class LancamentoControllerMappingTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

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
            "[GET] [/lancamentos/caixa/retornarMes]",
            "[GET] [/lancamentos/caixa/detalhar/{id}]",
            "[POST] [/lancamentos/caixa/criar]",
            "[PUT] [/lancamentos/caixa/atualizar/{id}]",
            "[POST] [/lancamentos/caixa/pagar/{id}]",
            "[GET] [/lancamentos/caixa/contas]",
            "[GET] [/lancamentos/caixa/fluxo]",
            "[GET] [/lancamentos/caixa/transferencias]",
            "[GET] [/lancamentos/caixa/semUnidade]",
            "[POST] [/lancamentos/caixa/pagamentos/registrar/{id}]",
            "[POST] [/lancamentos/caixa/pagamentos/desfazer/{id}]",
            "[POST] [/lancamentos/caixa/transferencias/criar]",
            "[PUT] [/lancamentos/caixa/transferencias/atualizar/{id}]",
            "[PUT] [/lancamentos/caixa/classificar]",
            "[PUT] [/lancamentos/caixa/saldos]",
            "[PUT] [/lancamentos/caixa/rateio]",
            "[DELETE] [/lancamentos/apagar/{id}]",
            "[GET] [/lancamentos/dre]"
        );
    }
}
