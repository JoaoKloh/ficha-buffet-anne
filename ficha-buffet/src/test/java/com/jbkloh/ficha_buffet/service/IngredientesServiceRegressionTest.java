package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.jbkloh.ficha_buffet.dtos.req.AssociationIngredientePratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreateIngredienteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreatePratoItemIngredienteDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreatePratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdateIngredienteRequestDto;
import com.jbkloh.ficha_buffet.dtos.resp.IngredienteResponseDTO;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.PratoEntity;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.PratoRepository;

import jakarta.persistence.EntityManager;

/**
 * Garante que a introducao da tabela intermediaria prato_ingrediente nao alterou
 * o comportamento das rotinas do IngredientesController/IngredientesService.
 */
@DataJpaTest
@Import({ IngredientesService.class, PratoService.class })
class IngredientesServiceRegressionTest {

    @Autowired
    private IngredientesService ingredientesService;

    @Autowired
    private PratoService pratoService;

    @Autowired
    private IngredientesRepository ingredientesRepository;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void criarIngrediente_atualizarIngrediente_eListar_continuamFuncionandoNormalmente() {
        CreateIngredienteRequestDTO createDto = new CreateIngredienteRequestDTO(
            "Ovo", "unidade", "Proteina", "Ovo caipira", 0.8, "Fornecedor A"
        );
        ingredientesService.criarIngrediente(createDto);
        entityManager.flush();
        entityManager.clear();

        List<IngredienteResponseDTO> listados = ingredientesService.listarIngredientes();
        assertThat(listados).hasSize(1);
        Long id = listados.get(0).id();

        UpdateIngredienteRequestDto updateDto = new UpdateIngredienteRequestDto(
            id, "Ovo caipira", "unidade", "Proteina", "Ovo caipira reforcado", 1.2, "Fornecedor B"
        );
        ingredientesService.atualizarIngrediente(updateDto);
        entityManager.flush();
        entityManager.clear();

        IngredientesEntity atualizado = ingredientesRepository.findById(id).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Ovo caipira");
        assertThat(atualizado.getCusto()).isEqualTo(1.2);
    }

    @Test
    void associarPratos_adicionaIngredienteAoPratoUsandoATabelaIntermediaria() {
        IngredientesEntity ingredienteBase = new IngredientesEntity();
        ingredienteBase.setNome("Manteiga");
        ingredienteBase.setUnidade("g");
        ingredienteBase.setCategoria("Gordura");
        ingredienteBase = ingredientesRepository.save(ingredienteBase);

        CreatePratoRequestDTO createDto = new CreatePratoRequestDTO(
            "Torrada", "Lanche", null, 1, "unidade", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteBase.getId(), 20.0, "g"))
        );
        PratoEntity prato = pratoService.criarPrato(createDto);
        entityManager.flush();
        entityManager.clear();

        IngredientesEntity novoIngrediente = new IngredientesEntity();
        novoIngrediente.setNome("Sal");
        novoIngrediente.setUnidade("g");
        novoIngrediente.setCategoria("Tempero");
        novoIngrediente = ingredientesRepository.save(novoIngrediente);

        AssociationIngredientePratoRequestDTO req = new AssociationIngredientePratoRequestDTO(
            novoIngrediente.getId(), List.of(prato.getId())
        );
        ingredientesService.associarPratos(req);
        entityManager.flush();
        entityManager.clear();

        PratoEntity recarregado = pratoRepository.findById(prato.getId()).orElseThrow();
        assertThat(recarregado.getItensIngredientes()).hasSize(2);
    }

    @Test
    void apagarIngrediente_semUsoEmNenhumPrato_continuaFuncionando() {
        IngredientesEntity ingrediente = new IngredientesEntity();
        ingrediente.setNome("Oregano");
        ingrediente.setUnidade("g");
        ingrediente.setCategoria("Tempero");
        Long id = ingredientesRepository.save(ingrediente).getId();
        entityManager.flush();

        ingredientesService.apagarIngrediente(id);
        entityManager.flush();

        assertThat(ingredientesRepository.findById(id)).isEmpty();
    }

    @Test
    void apagarIngrediente_emUsoEmUmPrato_eBloqueadoPelaIntegridadeReferencial() {
        IngredientesEntity ingrediente = new IngredientesEntity();
        ingrediente.setNome("Azeite");
        ingrediente.setUnidade("ml");
        ingrediente.setCategoria("Gordura");
        Long ingredienteId = ingredientesRepository.save(ingrediente).getId();

        CreatePratoRequestDTO createDto = new CreatePratoRequestDTO(
            "Salada", "Entrada", null, 1, "unidade", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 30.0, "ml"))
        );
        pratoService.criarPrato(createDto);
        entityManager.flush();
        entityManager.clear();

        // Comportamento pre-existente: a FK de prato_ingrediente.ingrediente_id nao possui
        // cascade de delecao, entao remover um ingrediente em uso deve falhar (nao silenciosamente
        // deixar dados orfaos). Isso nao foi introduzido pela tabela intermediaria, mas deve ser
        // tratado no service/controller como um AppException amigavel em vez de vazar a excecao do JPA.
        assertThatThrownBy(() -> {
            ingredientesService.apagarIngrediente(ingredienteId);
            ingredientesRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
