package com.jbkloh.ficha_buffet.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.dtos.req.AtualizarLancamentoDTO;
import com.jbkloh.ficha_buffet.dtos.req.CriarLancamentoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class LancamentoService {

    private final LancamentoRepository lancamentoRepository;

    @Transactional 
    public LancamentoResponseDTO criarLancamento(CriarLancamentoDTO req) {
        LancamentoEntity entity = new LancamentoEntity(
            null, req.tipo(), req.categoria(), req.descricao().trim(), req.valor(), req.data()
        );
        return new LancamentoResponseDTO(lancamentoRepository.save(entity));
    }

    @Transactional 
    public LancamentoResponseDTO atualizarLancamento(AtualizarLancamentoDTO req) {
        LancamentoEntity entity = buscarEntidade(req.id());

        entity.setTipo(req.tipo());
        entity.setCategoria(req.categoria());
        entity.setDescricao(req.descricao().trim());
        entity.setValor(req.valor());
        entity.setData(req.data());

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
}
