package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.InsumoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SituacaoInsumoDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.MovimentoEstoqueRepository;
import com.jbkloh.ficha_buffet.repositories.PreparacaoRepository;

import lombok.RequiredArgsConstructor;

/** Cadastro de itens de estoque no contrato do Anne Gestão (aba Itens da tela Fichas). */
@Service
@RequiredArgsConstructor
public class InsumoService {

    private final IngredientesRepository ingredientesRepository;
    private final PreparacaoRepository preparacaoRepository;
    private final MovimentoEstoqueRepository movimentoRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    @Transactional(readOnly = true)
    public List<InsumoListaItemDTO> listarItens(String busca) {
        Dados d = dadosService.carregar();
        String q = Formato.busca(busca);
        return d.insumos.stream().filter(i -> q.isEmpty() || Formato.contem(i.nome(), q)).sorted(Formato.porNome(InsumoResponseDTO::nome))
            .map(i -> new InsumoListaItemDTO(i, emFichas(d, i.id()))).toList();
    }

    static int emFichas(Dados d, Long insumoId) {
        return (int) d.preparacoes.stream().filter(p -> p.itens().stream().anyMatch(it -> insumoId.equals(it.insumoId()))).count();
    }

    @Transactional(readOnly = true)
    public SituacaoInsumoDTO situacaoItem(Long id) {
        cadastro.ingrediente(id);
        return new SituacaoInsumoDTO((int) preparacaoRepository.contarFichasComIngrediente(id), movimentoRepository.existsByIngredienteId(id));
    }

    @Transactional
    public InsumoResponseDTO criarItem(InsumoRequestDTO req) {
        IngredientesEntity item = new IngredientesEntity();
        aplicar(req, item);
        return GestaoMapper.insumo(ingredientesRepository.save(item));
    }

    @Transactional
    public InsumoResponseDTO atualizarItem(Long id, InsumoRequestDTO req) {
        IngredientesEntity item = cadastro.ingrediente(id);
        aplicar(req, item);
        return GestaoMapper.insumo(item);
    }

    /** Item usado em ficha ou com movimentos de estoque não pode ser excluído. */
    @Transactional
    public void apagarItem(Long id) {
        IngredientesEntity item = cadastro.ingrediente(id);
        if (preparacaoRepository.contarFichasComIngrediente(id) > 0) {
            throw new AppException("Este item está em fichas técnicas, por isso não pode ser excluído.", HttpStatus.CONFLICT);
        }
        if (movimentoRepository.existsByIngredienteId(id)) {
            throw new AppException("Este item tem movimentos de estoque, por isso não pode ser excluído.", HttpStatus.CONFLICT);
        }
        ingredientesRepository.delete(item);
    }

    private void aplicar(InsumoRequestDTO req, IngredientesEntity item) {
        String nome = req.nome().trim();
        ingredientesRepository.findByNome(nome).filter(outro -> !outro.getId().equals(item.getId())).ifPresent(outro -> {
            throw new AppException("Já existe um item com o nome: " + nome, HttpStatus.CONFLICT);
        });
        item.setNome(nome);
        item.setTipo(req.tipo() == null ? "ingrediente" : req.tipo());
        item.setCategoria(req.categoria() == null ? "Mercearia" : req.categoria());
        item.setUnidade(req.unidade() == null ? "kg" : req.unidade());
        item.setLocal(req.local() == null ? "Depósito" : req.local());
        item.setUnidadeCompra(Formato.texto(req.unidadeCompra()).trim());
        item.setFatorCompra(req.fatorCompra());
        item.setEstoqueMinimo(req.estoqueMinimo() == null ? 0.0 : req.estoqueMinimo());
        item.setCusto(req.custo() == null ? BigDecimal.ZERO : req.custo().setScale(4, RoundingMode.HALF_UP));
        item.setFornecedor(Formato.texto(req.fornecedor()).trim());
        item.setAtualizadoEm(LocalDate.now());
    }
}
