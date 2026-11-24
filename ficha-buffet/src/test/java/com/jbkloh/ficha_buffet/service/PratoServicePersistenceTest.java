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
import com.jbkloh.ficha_buffet.dtos.req.UpdatePratoRequest;
import com.jbkloh.ficha_buffet.dtos.resp.PratoDetalhadoResponseDTO;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.PreparacaoEntity;
import com.jbkloh.ficha_buffet.model.ClienteEntity;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.PreparacaoRepository;
import com.jbkloh.ficha_buffet.repositories.EventoRepository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;

@DataJpaTest
@Import(PratoService.class)
class PratoServicePersistenceTest {

    @Autowired
    private PratoService pratoService;

    @Autowired
    private PreparacaoRepository pratoRepository;

    @Autowired
    private IngredientesRepository ingredientesRepository;

    @Autowired
    private EventoRepository producaoRepository;

    @Autowired
    private EntityManager entityManager;

    // Evento mínimo válido: todo evento tem cliente e código.
    private EventoEntity novoEvento(String nome, int convidados, LocalDate data) {
        ClienteEntity cliente = new ClienteEntity();
        cliente.setNome(nome);
        entityManager.persist(cliente);
        EventoEntity evento = new EventoEntity();
        evento.setNome(nome);
        evento.setConvidadosPrevistos(convidados);
        evento.setDataEvento(data);
        evento.setCliente(cliente);
        evento.setCodigo(String.format("26-%03d", ++sequenciaCodigo));
        return evento;
    }

    private int sequenciaCodigo = 0;

    private Long criarIngrediente(String nome) {
        IngredientesEntity ingrediente = new IngredientesEntity();
        ingrediente.setNome(nome);
        ingrediente.setUnidade("g");
        ingrediente.setCategoria("Geral");
        ingrediente.setCusto(new java.math.BigDecimal("1.0"));
        return ingredientesRepository.save(ingrediente).getId();
    }

    @Test
    void criarPrato_devePersistirItensIngredientesNoBanco() {
        Long ingredienteId = criarIngrediente("Farinha");

        CreatePratoRequestDTO dto = new CreatePratoRequestDTO(
            "Bolo", "sobremesa", null, 1.0, "un", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 200.0, "g"))
        );

        pratoService.criarPrato(dto);
        PreparacaoEntity salvo = buscarPratoPorNome("Bolo");

        entityManager.flush();
        entityManager.clear();

        PreparacaoEntity recarregado = pratoRepository.findById(salvo.getId()).orElseThrow();

        assertThat(recarregado.getItensIngredientes())
            .as("os itens de ingrediente do prato devem ser persistidos na tabela intermediaria")
            .hasSize(1);
    }

    @Test
    void atualizarPrato_removendoIngrediente_deveExcluirLinhaOrfaDaTabelaIntermediaria() {
        Long ingredienteId = criarIngrediente("Acucar");

        CreatePratoRequestDTO createDto = new CreatePratoRequestDTO(
            "Pudim", "sobremesa", null, 1.0, "un", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 100.0, "g"))
        );
        pratoService.criarPrato(createDto);
        PreparacaoEntity salvo = buscarPratoPorNome("Pudim");
        entityManager.flush();
        entityManager.clear();

        UpdatePratoRequest updateDto = new UpdatePratoRequest(
            salvo.getId(), "Pudim", "sobremesa", null, 1.0, "un", 1.0, "receita",
            List.of()
        );
        pratoService.atualizarPrato(updateDto);
        entityManager.flush();
        entityManager.clear();

        Number count = (Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM prato_ingrediente WHERE preparacao_id = :id")
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
            "Vitamina", "bebida", null, 1.0, "un", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 300.0, "ml"))
        );
        pratoService.criarPrato(createDto);
        PreparacaoEntity salvo = buscarPratoPorNome("Vitamina");
        entityManager.flush();

        pratoService.apagarPrato(salvo.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(pratoRepository.findById(salvo.getId())).isEmpty();
        assertThat(ingredientesRepository.findById(ingredienteId))
            .as("apagar um prato nao deve apagar o ingrediente do catalogo global")
            .isPresent();

        Number linhasIntermediarias = (Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM prato_ingrediente WHERE preparacao_id = :id")
            .setParameter("id", salvo.getId())
            .getSingleResult();
        assertThat(linhasIntermediarias.longValue())
            .as("apagar um prato deve apagar as linhas da tabela intermediaria prato_ingrediente")
            .isZero();
    }

    @Test
    void listarPratos_deveExporQuantidadeDoIngredienteNaFichaTecnica() {
        Long ingredienteId = criarIngrediente("Tomate");

        CreatePratoRequestDTO createDto = new CreatePratoRequestDTO(
            "Molho", "base", null, 1.0, "l", 1.0, "receita",
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
            "Tempero", "base", null, 1.0, "un", 1.0, "receita",
            List.of(new CreatePratoItemIngredienteDTO(ingredienteId, 5.0, "g"))
        );
        pratoService.criarPrato(createDto);
        PreparacaoEntity prato = buscarPratoPorNome("Tempero");

        EventoEntity evento = novoEvento("Casamento", 100, LocalDate.now());
        producaoRepository.save(evento);
        entityManager.flush();
        entityManager.clear();

        AssociationPratoProducaoRequestDTO req = new AssociationPratoProducaoRequestDTO(prato.getId(), List.of(evento.getId()));
        pratoService.associarEventos(req);
        pratoService.associarEventos(req);
        entityManager.flush();
        entityManager.clear();

        EventoEntity recarregado = producaoRepository.findById(evento.getId()).orElseThrow();
        assertThat(recarregado.getCardapio())
            .as("associar o mesmo evento duas vezes nao deve duplicar o vinculo")
            .hasSize(1);
    }

    // criarPrato não devolve a entidade; o prato criado é localizado pelo nome.
    private PreparacaoEntity buscarPratoPorNome(String nome) {
        return pratoRepository.findAll().stream()
            .filter(p -> p.getNome().equals(nome))
            .findFirst()
            .orElseThrow();
    }
}
