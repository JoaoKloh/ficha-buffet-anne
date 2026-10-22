package com.jbkloh.ficha_buffet.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.dtos.req.AtualizarLancamentoDTO;
import com.jbkloh.ficha_buffet.dtos.req.CriarLancamentoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.ProducaoEntity;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;
import com.jbkloh.ficha_buffet.repositories.ProducaoRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class LancamentoService {

    private final LancamentoRepository lancamentoRepository;
    private final ProducaoRepository producaoRepository;

    @Transactional 
    public LancamentoResponseDTO criarLancamento(CriarLancamentoDTO req) {
        LancamentoEntity entity = CriarLancamentoDTO.toEntity(req, buscarProducoes(req.producaoIds()));
        return new LancamentoResponseDTO(lancamentoRepository.save(entity));
    }

    @Transactional 
    public LancamentoResponseDTO atualizarLancamento(AtualizarLancamentoDTO req) {
        LancamentoEntity entity = buscarEntidade(req.id());

        AtualizarLancamentoDTO.atualizarEntidade(req, entity, buscarProducoes(req.producaoIds()));

        return new LancamentoResponseDTO(entity);
    }

    @Transactional (readOnly = true)
    public List<LancamentoResponseDTO> listarLancamentos() {
        return lancamentoRepository.findAll()
                .stream()
                .map(LancamentoResponseDTO::new)
                .toList();
    }

    @Transactional 
    public void apagarLancamento(Long id) {
        lancamentoRepository.delete(buscarEntidade(id));
    }

    private LancamentoEntity buscarEntidade(Long id) {
        return lancamentoRepository.findById(id)
            .orElseThrow(() -> new AppException("O lançamento de ID " + id + " não existe.", HttpStatus.NOT_FOUND));
    }

    // Lista mutável: o Hibernate gerencia a coleção do @ManyToMany depois de atribuída.
    private List<ProducaoEntity> buscarProducoes(List<Long> producaoIds) {
        if (producaoIds == null || producaoIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> ids = producaoIds.stream().distinct().toList();
        List<ProducaoEntity> producoes = producaoRepository.findAllById(ids);

        if (producoes.size() != ids.size()) {
            List<Long> encontrados = producoes.stream().map(ProducaoEntity::getId).toList();
            List<Long> inexistentes = ids.stream().filter(id -> !encontrados.contains(id)).toList();
            throw new AppException("As produções de ID " + inexistentes + " não existem.", HttpStatus.NOT_FOUND);
        }

        return new ArrayList<>(producoes);
    }
}
