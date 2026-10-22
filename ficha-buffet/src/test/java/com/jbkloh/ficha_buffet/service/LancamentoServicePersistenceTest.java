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
import com.jbkloh.ficha_buffet.model.ProducaoEntity;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;
import com.jbkloh.ficha_buffet.repositories.ProducaoRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({ LancamentoService.class, ProducaoService.class })
class LancamentoServicePersistenceTest {

    @Autowired
    private LancamentoService lancamentoService;

    @Autowired
    private ProducaoService producaoService;

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private ProducaoRepository producaoRepository;

    @Autowired
    private EntityManager entityManager;

    private Long criarProducao() {
        return producaoRepository.save(new ProducaoEntity("Casamento Silva", 100, LocalDate.of(2026, 9, 24), List.of())).getId();
    }

    private CriarLancamentoDTO receitaVinculada(List<Long> producaoIds) {
        return new CriarLancamentoDTO(
            "receita", "Evento fechado", "Casamento Silva", new BigDecimal("1500.50"),
            LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25), producaoIds
        );
    }

    private LancamentoResponseDTO criarReceita() {
        LancamentoResponseDTO criado = lancamentoService.criarLancamento(new CriarLancamentoDTO(
            "receita", "Evento fechado", "  Casamento Silva ", new BigDecimal("1500.50"), LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25), null
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
        assertThat(salvo.getDataInical()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(salvo.getDataFinal()).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(salvo.getProducoes()).isEmpty();
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
            id, "despesa", "Bebidas", "Refrigerantes", new BigDecimal("320.00"), LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 21), null
        ));
        entityManager.flush();
        entityManager.clear();

        LancamentoEntity atualizado = lancamentoRepository.findById(id).orElseThrow();
        assertThat(atualizado.getTipo()).isEqualTo("despesa");
        assertThat(atualizado.getCategoria()).isEqualTo("Bebidas");
        assertThat(atualizado.getDescricao()).isEqualTo("Refrigerantes");
        assertThat(atualizado.getValor()).isEqualByComparingTo("320.00");
        assertThat(atualizado.getDataInical()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(atualizado.getDataFinal()).isEqualTo(LocalDate.of(2026, 9, 21));
    }

    @Test
    void atualizarLancamento_inexistenteLanca404() {
        AtualizarLancamentoDTO req = new AtualizarLancamentoDTO(
            999L, "despesa", "Bebidas", "Refrigerantes", new BigDecimal("320.00"), LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 21), null
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

    @Test
    void criarLancamento_comVariasProducoes_persisteOsVinculos() {
        Long producaoA = criarProducao();
        Long producaoB = criarProducao();

        LancamentoResponseDTO criado = lancamentoService.criarLancamento(receitaVinculada(List.of(producaoA, producaoB)));
        entityManager.flush();
        entityManager.clear();

        assertThat(criado.producaoIds()).containsExactlyInAnyOrder(producaoA, producaoB);
        LancamentoEntity salvo = lancamentoRepository.findById(criado.id()).orElseThrow();
        assertThat(salvo.getProducoes()).extracting(ProducaoEntity::getId).containsExactlyInAnyOrder(producaoA, producaoB);
    }

    @Test
    void criarLancamento_idsRepetidos_vinculaUmaVez() {
        Long producaoId = criarProducao();

        LancamentoResponseDTO criado = lancamentoService.criarLancamento(receitaVinculada(List.of(producaoId, producaoId)));

        assertThat(criado.producaoIds()).containsExactly(producaoId);
    }

    @Test
    void criarLancamento_mesmaProducaoEmVariosLancamentos_ePermitido() {
        Long producaoId = criarProducao();
        lancamentoService.criarLancamento(receitaVinculada(List.of(producaoId)));
        lancamentoService.criarLancamento(receitaVinculada(List.of(producaoId)));
        entityManager.flush();

        assertThat(lancamentoRepository.findByProducoesId(producaoId)).hasSize(2);
    }

    @Test
    void criarLancamento_comProducaoInexistenteLanca404() {
        Long producaoId = criarProducao();

        assertThatThrownBy(() -> lancamentoService.criarLancamento(receitaVinculada(List.of(producaoId, 999L))))
            .isInstanceOf(AppException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void atualizarLancamento_substituiAsProducoes() {
        Long producaoA = criarProducao();
        Long producaoB = criarProducao();
        Long id = lancamentoService.criarLancamento(receitaVinculada(List.of(producaoA))).id();
        entityManager.flush();
        entityManager.clear();

        lancamentoService.atualizarLancamento(new AtualizarLancamentoDTO(
            id, "receita", "Evento fechado", "Casamento Silva", new BigDecimal("1500.50"),
            LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25), List.of(producaoB)
        ));
        entityManager.flush();
        entityManager.clear();

        assertThat(lancamentoRepository.findById(id).orElseThrow().getProducoes())
            .extracting(ProducaoEntity::getId).containsExactly(producaoB);
    }

    @Test
    void atualizarLancamento_semProducoes_removeOsVinculos() {
        Long producaoId = criarProducao();
        Long id = lancamentoService.criarLancamento(receitaVinculada(List.of(producaoId))).id();
        entityManager.flush();
        entityManager.clear();

        lancamentoService.atualizarLancamento(new AtualizarLancamentoDTO(
            id, "receita", "Evento fechado", "Casamento Silva", new BigDecimal("1500.50"),
            LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25), null
        ));
        entityManager.flush();
        entityManager.clear();

        assertThat(lancamentoRepository.findById(id).orElseThrow().getProducoes()).isEmpty();
        assertThat(producaoRepository.findById(producaoId)).isPresent();
    }

    @Test
    void apagarLancamento_naoApagaAsProducoes() {
        Long producaoId = criarProducao();
        Long id = lancamentoService.criarLancamento(receitaVinculada(List.of(producaoId))).id();
        entityManager.flush();

        lancamentoService.apagarLancamento(id);
        entityManager.flush();
        entityManager.clear();

        assertThat(producaoRepository.findById(producaoId)).isPresent();
    }

    @Test
    void apagarProducao_mantemOLancamentoERemoveSoOVinculo() {
        Long producaoA = criarProducao();
        Long producaoB = criarProducao();
        Long id = lancamentoService.criarLancamento(receitaVinculada(List.of(producaoA, producaoB))).id();
        entityManager.flush();
        entityManager.clear();

        producaoService.apagarProducao(producaoA);
        entityManager.flush();
        entityManager.clear();

        assertThat(lancamentoRepository.findById(id).orElseThrow().getProducoes())
            .extracting(ProducaoEntity::getId).containsExactly(producaoB);
    }
}
