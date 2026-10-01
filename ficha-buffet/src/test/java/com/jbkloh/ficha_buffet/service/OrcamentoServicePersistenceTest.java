package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.jbkloh.ficha_buffet.dtos.req.CreateOrcamentoRequest;
import com.jbkloh.ficha_buffet.dtos.resp.OrcamentoDetalhadoDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.OrcamentoEntity;
import com.jbkloh.ficha_buffet.repositories.OrcamentoRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(OrcamentoService.class)
class OrcamentoServicePersistenceTest {

    @Autowired
    private OrcamentoService orcamentoService;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private EntityManager entityManager;

    private OrcamentoDetalhadoDTO criarOrcamento() {
        OrcamentoDetalhadoDTO criado = orcamentoService.criarOrcamento(new CreateOrcamentoRequest(
            "  Maria Silva ", "(11) 99999-0000", "Buffet completo", 4.5, 120,
            new BigDecimal("4912.80"), new BigDecimal("3150.00"), new BigDecimal("150.00"),
            new BigDecimal("2672.00"), new BigDecimal("24158.22"), new BigDecimal("201.32")
        ));
        entityManager.flush();
        entityManager.clear();
        return criado;
    }

    @Test
    void criarOrcamento_persisteTodosOsCamposERetornaOIdGerado() {
        OrcamentoDetalhadoDTO criado = criarOrcamento();

        assertThat(criado.id()).isNotNull();
        OrcamentoEntity salvo = orcamentoRepository.findById(criado.id()).orElseThrow();
        assertThat(salvo.getNomeCliente()).isEqualTo("Maria Silva");
        assertThat(salvo.getContatoCliente()).isEqualTo("(11) 99999-0000");
        assertThat(salvo.getTipoServico()).isEqualTo("Buffet completo");
        assertThat(salvo.getDuracaoEvento()).isEqualTo(4.5);
        assertThat(salvo.getNumeroConvidados()).isEqualTo(120);
        assertThat(salvo.getValorAlimentos()).isEqualByComparingTo("4912.80");
        assertThat(salvo.getValorEquipe()).isEqualByComparingTo("3150.00");
        assertThat(salvo.getValorDegustacao()).isEqualByComparingTo("150.00");
        assertThat(salvo.getValorOutros()).isEqualByComparingTo("2672.00");
        assertThat(salvo.getValorTotal()).isEqualByComparingTo("24158.22");
        assertThat(salvo.getValorPorPessoa()).isEqualByComparingTo("201.32");
    }

    @Test
    void listarOrcamentos_retornaDTOsComTodosOsCampos() {
        OrcamentoDetalhadoDTO criado = criarOrcamento();

        List<OrcamentoDetalhadoDTO> lista = orcamentoService.listarOrcamentos();

        assertThat(lista).hasSize(1);
        OrcamentoDetalhadoDTO dto = lista.get(0);
        assertThat(dto.id()).isEqualTo(criado.id());
        assertThat(dto.nomeCliente()).isEqualTo("Maria Silva");
        assertThat(dto.numeroConvidados()).isEqualTo(120);
        assertThat(dto.valorTotal()).isEqualByComparingTo("24158.22");
        assertThat(dto.valorPorPessoa()).isEqualByComparingTo("201.32");
    }

    @Test
    void apagarOrcamento_removeORegistro() {
        OrcamentoDetalhadoDTO criado = criarOrcamento();

        orcamentoService.apagarOrcamento(criado.id());
        entityManager.flush();

        assertThat(orcamentoRepository.findById(criado.id())).isEmpty();
    }

    @Test
    void apagarOrcamentoInexistente_lanca404() {
        assertThatThrownBy(() -> orcamentoService.apagarOrcamento(999L))
            .isInstanceOf(AppException.class)
            .extracting(e -> ((AppException) e).getHttpStatus())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
