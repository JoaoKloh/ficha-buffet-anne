package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.jbkloh.ficha_buffet.dtos.req.CreatePratoItemIngredienteDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreatePratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreateProducaoItemRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreateProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdateProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ProducaoResponseDTO;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.PratoEntity;
import com.jbkloh.ficha_buffet.model.ProducaoEntity;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.PratoRepository;
import com.jbkloh.ficha_buffet.repositories.ProducaoRepository;

import jakarta.persistence.EntityManager;

/**
 * Garante que a introducao da tabela intermediaria prato_ingrediente nao alterou
 * o comportamento das rotinas do ProducaoController/ProducaoService, mesmo quando
 * os pratos vinculados possuem ingredientes cadastrados.
 */
@DataJpaTest
@Import({ ProducaoService.class, PratoService.class })
class ProducaoServiceRegressionTest {

    @Autowired
    private ProducaoService producaoService;

    @Autowired
    private PratoService pratoService;

    @Autowired
    private ProducaoRepository producaoRepository;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private IngredientesRepository ingredientesRepository;

    @Autowired
    private EntityManager entityManager;

    private Long criarPratoComIngrediente(String nomePrato, String nomeIngrediente) {
        IngredientesEntity ingrediente = new IngredientesEntity();
        ingrediente.setNome(nomeIngrediente);
        ingrediente.setUnidade("g");
        ingrediente.setCategoria("Geral");
        Long ingredienteId = ingredientesRepository.save(ingrediente).getId();

        CreatePratoRequestDTO createDto = new CreatePratoRequestDTO(
            nomePrato, "Categoria", null, 1, "unidade", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 100.0, "g"))
        );
        return pratoService.criarPrato(createDto).getId();
    }

    @Test
    void criarEvento_vinculaPratosComIngredientesCorretamente() {
        Long pratoId = criarPratoComIngrediente("Frango assado", "Frango");
        entityManager.flush();
        entityManager.clear();

        CreateProducaoRequestDTO req = new CreateProducaoRequestDTO(
            "Festa junina", 50, LocalDate.now().plusDays(10),
            List.of(new CreateProducaoItemRequestDTO(pratoId))
        );
        producaoService.criarEvento(req);
        entityManager.flush();
        entityManager.clear();

        List<ProducaoResponseDTO> eventos = producaoService.listarEventos();
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).pratos()).hasSize(1);
        assertThat(eventos.get(0).pratos().get(0).id()).isEqualTo(pratoId);
    }

    @Test
    void atualizarEvento_trocaPratosSemAfetarIngredientesDoPratoAnterior() {
        Long pratoAntigoId = criarPratoComIngrediente("Arroz", "Arroz branco");
        Long pratoNovoId = criarPratoComIngrediente("Feijao", "Feijao preto");
        entityManager.flush();
        entityManager.clear();

        CreateProducaoRequestDTO createReq = new CreateProducaoRequestDTO(
            "Almoco corporativo", 30, LocalDate.now().plusDays(5),
            List.of(new CreateProducaoItemRequestDTO(pratoAntigoId))
        );
        producaoService.criarEvento(createReq);
        entityManager.flush();
        entityManager.clear();

        ProducaoEntity evento = producaoRepository.findAll().get(0);

        UpdateProducaoRequestDTO updateReq = new UpdateProducaoRequestDTO(
            evento.getId(), "Almoco corporativo", 40, LocalDate.now().plusDays(6),
            List.of(new CreateProducaoItemRequestDTO(pratoNovoId))
        );
        producaoService.atualizarEvento(updateReq);
        entityManager.flush();
        entityManager.clear();

        ProducaoEntity atualizado = producaoRepository.findById(evento.getId()).orElseThrow();
        assertThat(atualizado.getPratos()).hasSize(1);
        assertThat(atualizado.getPratos().get(0).getId()).isEqualTo(pratoNovoId);

        // O prato antigo e seus ingredientes continuam intactos, apenas desvinculados do evento.
        PratoEntity pratoAntigo = pratoRepository.findById(pratoAntigoId).orElseThrow();
        assertThat(pratoAntigo.getItensIngredientes()).hasSize(1);
    }

    @Test
    void apagarProducao_naoExcluiOsPratosNemOsIngredientesVinculados() {
        Long pratoId = criarPratoComIngrediente("Lasanha", "Massa");
        entityManager.flush();
        entityManager.clear();

        CreateProducaoRequestDTO req = new CreateProducaoRequestDTO(
            "Jantar", 20, LocalDate.now().plusDays(3),
            List.of(new CreateProducaoItemRequestDTO(pratoId))
        );
        producaoService.criarEvento(req);
        entityManager.flush();
        entityManager.clear();

        ProducaoEntity evento = producaoRepository.findAll().get(0);
        producaoService.apagarProducao(evento.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(producaoRepository.findById(evento.getId())).isEmpty();

        PratoEntity prato = pratoRepository.findById(pratoId).orElseThrow();
        assertThat(prato.getItensIngredientes())
            .as("apagar a producao nao deve apagar o prato nem seus ingredientes")
            .hasSize(1);

        Number linhasAssociacao = (Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM producao_prato WHERE producao_id = :id")
            .setParameter("id", evento.getId())
            .getSingleResult();
        assertThat(linhasAssociacao.longValue())
            .as("apagar a producao deve apagar as linhas da tabela de associacao producao_prato")
            .isZero();
    }
}
