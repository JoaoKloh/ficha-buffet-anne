package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.jbkloh.ficha_buffet.dtos.req.AssociationPratoProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreatePratoItemIngredienteDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreatePratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.IngredienteDTO;
import com.jbkloh.ficha_buffet.dtos.req.PratoDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdatePratoRequest;
import com.jbkloh.ficha_buffet.dtos.resp.PratoDetalhadoResponseDTO;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.PratoEntity;
import com.jbkloh.ficha_buffet.model.ProducaoEntity;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.PratoRepository;
import com.jbkloh.ficha_buffet.repositories.ProducaoRepository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;

@DataJpaTest
@Import(PratoService.class)
class PratoServicePersistenceTest {

    @Autowired
    private PratoService pratoService;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private IngredientesRepository ingredientesRepository;

    @Autowired
    private ProducaoRepository producaoRepository;

    @Autowired
    private EntityManager entityManager;

    private Long criarIngrediente(String nome) {
        IngredientesEntity ingrediente = new IngredientesEntity();
        ingrediente.setNome(nome);
        ingrediente.setUnidade("g");
        ingrediente.setCategoria("Geral");
        ingrediente.setCusto(1.0);
        return ingredientesRepository.save(ingrediente).getId();
    }

    @Test
    void criarPrato_devePersistirItensIngredientesNoBanco() {
        Long ingredienteId = criarIngrediente("Farinha");

        CreatePratoRequestDTO dto = new CreatePratoRequestDTO(
            "Bolo", "Sobremesa", null, 1, "unidade", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 200.0, "g"))
        );

        PratoEntity salvo = pratoService.criarPrato(dto);

        entityManager.flush();
        entityManager.clear();

        PratoEntity recarregado = pratoRepository.findById(salvo.getId()).orElseThrow();

        assertThat(recarregado.getItensIngredientes())
            .as("os itens de ingrediente do prato devem ser persistidos na tabela intermediaria")
            .hasSize(1);
    }

    @Test
    void atualizarPrato_removendoIngrediente_deveExcluirLinhaOrfaDaTabelaIntermediaria() {
        Long ingredienteId = criarIngrediente("Acucar");

        CreatePratoRequestDTO createDto = new CreatePratoRequestDTO(
            "Pudim", "Sobremesa", null, 1, "unidade", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 100.0, "g"))
        );
        PratoEntity salvo = pratoService.criarPrato(createDto);
        entityManager.flush();
        entityManager.clear();

        UpdatePratoRequest updateDto = new UpdatePratoRequest(
            salvo.getId(), "Pudim", "Sobremesa", null, 1, "unidade", 1.0, "receita",
            List.of()
        );
        pratoService.atualizarPrato(updateDto);
        entityManager.flush();
        entityManager.clear();

        Number count = (Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM prato_ingrediente WHERE prato_id = :id")
            .setParameter("id", salvo.getId())
            .getSingleResult();

        assertThat(count.longValue())
            .as("linhas orfas da tabela intermediaria devem ser removidas ao atualizar o prato")
            .isZero();
    }

    @Test
    void apagarPrato_naoDeveExcluirIngredientesDoCatalogo() {
        Long ingredienteId = criarIngrediente("Leite");

        CreatePratoRequestDTO createDto = new CreatePratoRequestDTO(
            "Vitamina", "Bebida", null, 1, "unidade", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 300.0, "ml"))
        );
        PratoEntity salvo = pratoService.criarPrato(createDto);
        entityManager.flush();

        pratoService.apagarPrato(salvo.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(pratoRepository.findById(salvo.getId())).isEmpty();
        assertThat(ingredientesRepository.findById(ingredienteId))
            .as("apagar um prato nao deve apagar o ingrediente do catalogo global")
            .isPresent();

        Number linhasIntermediarias = (Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM prato_ingrediente WHERE prato_id = :id")
            .setParameter("id", salvo.getId())
            .getSingleResult();
        assertThat(linhasIntermediarias.longValue())
            .as("apagar um prato deve apagar as linhas da tabela intermediaria prato_ingrediente")
            .isZero();
    }

    @Test
    void salvarPratosComIngredientes_reaproveitaIngredienteExistenteEcriaNovoQuandoNecessario() {
        Long farinhaId = criarIngrediente("Farinha de trigo");

        PratoDTO pratoDto = new PratoDTO(
            "Pao", "Padaria", null, 2, "unidades", 1.0, "receita",
            List.of(
                new IngredienteDTO("Farinha de trigo", 500.0, "g"),
                new IngredienteDTO("Fermento", 10.0, "g")
            )
        );

        List<PratoEntity> salvos = pratoService.salvarPratosComIngredientes(List.of(pratoDto));
        entityManager.flush();
        entityManager.clear();

        PratoEntity recarregado = pratoRepository.findById(salvos.get(0).getId()).orElseThrow();
        assertThat(recarregado.getItensIngredientes()).hasSize(2);

        assertThat(ingredientesRepository.findById(farinhaId))
            .as("o ingrediente ja cadastrado nao deve ser duplicado")
            .isPresent();
        assertThat(ingredientesRepository.findByNomeAndUnidadeMedida("Fermento", "g"))
            .as("um novo ingrediente deve ser criado quando nao existir no catalogo")
            .isPresent();
        assertThat(ingredientesRepository.findAll())
            .as("nao deve haver duplicidade do ingrediente ja existente")
            .hasSize(2);
    }

    @Test
    void listarPratos_deveExporQuantidadeDoIngredienteNaFichaTecnica() {
        Long ingredienteId = criarIngrediente("Tomate");

        CreatePratoRequestDTO createDto = new CreatePratoRequestDTO(
            "Molho", "Base", null, 1, "litro", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 750.0, "g"))
        );
        pratoService.criarPrato(createDto);
        entityManager.flush();
        entityManager.clear();

        List<PratoDetalhadoResponseDTO> pratos = pratoService.listarPratos();

        assertThat(pratos).hasSize(1);
        assertThat(pratos.get(0).ingredientes()).hasSize(1);
        assertThat(pratos.get(0).ingredientes().get(0).qtd())
            .as("a quantidade do ingrediente na ficha tecnica do prato deve ser exposta sem duplicar o catalogo")
            .isEqualTo(750.0);
    }

    @Test
    void associarEventos_deveVincularPratoAoEventoSemDuplicar() {
        Long ingredienteId = criarIngrediente("Sal");
        CreatePratoRequestDTO createDto = new CreatePratoRequestDTO(
            "Tempero", "Base", null, 1, "unidade", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 5.0, "g"))
        );
        PratoEntity prato = pratoService.criarPrato(createDto);

        ProducaoEntity evento = new ProducaoEntity(null, "Casamento", 100, LocalDate.now(), new java.util.ArrayList<>());
        producaoRepository.save(evento);
        entityManager.flush();
        entityManager.clear();

        AssociationPratoProducaoRequestDTO req = new AssociationPratoProducaoRequestDTO(prato.getId(), List.of(evento.getId()));
        pratoService.associarEventos(req);
        pratoService.associarEventos(req);
        entityManager.flush();
        entityManager.clear();

        ProducaoEntity recarregado = producaoRepository.findById(evento.getId()).orElseThrow();
        assertThat(recarregado.getPratos())
            .as("associar o mesmo evento duas vezes nao deve duplicar o vinculo")
            .hasSize(1);
    }
}
