package com.jbkloh.ficha_buffet.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.dtos.req.CategoriaFinanceiraRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.CategoriaFinanceiraEntity;
import com.jbkloh.ficha_buffet.repositories.CategoriaFinanceiraRepository;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaFinanceiraService {

    private final CategoriaFinanceiraRepository categoriaRepository;
    private final LancamentoRepository lancamentoRepository;

    @Transactional
    public CategoriaResponseDTO criarCategoria(CategoriaFinanceiraRequestDTO req) {
        CategoriaFinanceiraEntity categoria = new CategoriaFinanceiraEntity(req.nome().trim(), req.grupo(), req.ordem() == null ? 0 : req.ordem());
        return GestaoMapper.categoria(categoriaRepository.save(categoria));
    }

    /** Mudar o grupo entre receita e despesa retipa os lançamentos da categoria. */
    @Transactional
    public CategoriaResponseDTO atualizarCategoria(Long id, CategoriaFinanceiraRequestDTO req) {
        CategoriaFinanceiraEntity categoria = buscar(id);
        boolean mudouGrupo = !categoria.getGrupo().equals(req.grupo());
        categoria.setNome(req.nome().trim());
        categoria.setGrupo(req.grupo());
        categoria.setOrdem(req.ordem() == null ? 0 : req.ordem());
        if (mudouGrupo) {
            String tipo = tipoDoGrupo(req.grupo());
            lancamentoRepository.findByCategoriaId(id).stream().filter(l -> !"transferencia".equals(l.getTipo())).forEach(l -> l.setTipo(tipo));
        }
        return GestaoMapper.categoria(categoria);
    }

    @Transactional
    public void apagarCategoria(Long id) {
        CategoriaFinanceiraEntity categoria = buscar(id);
        if (lancamentoRepository.existsByCategoriaId(id)) {
            throw new AppException("Esta categoria tem lançamentos, por isso não pode ser excluída.", HttpStatus.CONFLICT);
        }
        categoriaRepository.delete(categoria);
    }

    /** O tipo do lançamento acompanha o grupo da categoria. */
    public static String tipoDoGrupo(String grupo) {
        return "receita".equals(grupo) ? "receita" : "despesa";
    }

    private CategoriaFinanceiraEntity buscar(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new AppException("A categoria de id " + id + " não existe.", HttpStatus.NOT_FOUND));
    }
}
