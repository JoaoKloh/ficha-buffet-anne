package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.jbkloh.ficha_buffet.dtos.req.AtualizarLancamentoDTO;
import com.jbkloh.ficha_buffet.dtos.req.CriarLancamentoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(LancamentoService.class)
class LancamentoServicePersistenceTest {

    @Autowired
    private LancamentoService lancamentoService;

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private EntityManager entityManager;

    private LancamentoResponseDTO criarReceita() {
        LancamentoResponseDTO criado = lancamentoService.criarLancamento(new CriarLancamentoDTO(
            "receita", "Evento fechado", "  Casamento Silva ", new BigDecimal("1500.50"), LocalDate.of(2026, 9, 24)
        ));
        entityManager.flush();
        entityManager.clear();
        return criado;
    }

    @Test
    void criarLancamento_persisteERetornaOIdGerado() {
        LancamentoResponseDTO criado = criarReceita();

        assertThat(criado.id()).isNotNull();
        LancamentoEntity salvo = lancamentoRepository.findById(criado.id()).orElseThrow();
        assertThat(salvo.getTipo()).isEqualTo("receita");
        assertThat(salvo.getCategoria()).isEqualTo("Evento fechado");
        assertThat(salvo.getDescricao()).isEqualTo("Casamento Silva");
        assertThat(salvo.getValor()).isEqualByComparingTo("1500.50");
        assertThat(salvo.getData()).isEqualTo(LocalDate.of(2026, 9, 24));
    }

    @Test
    void listarLancamentos_retornaTodosComoDTO() {
        criarReceita();
        criarReceita();

        List<LancamentoResponseDTO> lista = lancamentoService.listarLancamentos();
        assertThat(lista).hasSize(2);
    }

    @Test
    void atualizarLancamento_alteraTodosOsCampos() {
        Long id = criarReceita().id();

        lancamentoService.atualizarLancamento(new AtualizarLancamentoDTO(
            id, "despesa", "Bebidas", "Refrigerantes", new BigDecimal("320.00"), LocalDate.of(2026, 9, 20)
        ));
        entityManager.flush();
        entityManager.clear();

        LancamentoEntity atualizado = lancamentoRepository.findById(id).orElseThrow();
        assertThat(atualizado.getTipo()).isEqualTo("despesa");
        assertThat(atualizado.getCategoria()).isEqualTo("Bebidas");
        assertThat(atualizado.getDescricao()).isEqualTo("Refrigerantes");
        assertThat(atualizado.getValor()).isEqualByComparingTo("320.00");
        assertThat(atualizado.getData()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void atualizarLancamento_inexistenteLanca404() {
        AtualizarLancamentoDTO req = new AtualizarLancamentoDTO(
            999L, "despesa", "Bebidas", "Refrigerantes", new BigDecimal("320.00"), LocalDate.of(2026, 9, 20)
        );

        assertThatThrownBy(() -> lancamentoService.atualizarLancamento(req))
            .isInstanceOf(AppException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void apagarLancamento_removeDoBanco() {
        Long id = criarReceita().id();

        lancamentoService.apagarLancamento(id);
        entityManager.flush();

        assertThat(lancamentoRepository.findById(id)).isEmpty();
    }

    @Test
    void apagarLancamento_inexistenteLanca404() {
        assertThatThrownBy(() -> lancamentoService.apagarLancamento(999L))
            .isInstanceOf(AppException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
