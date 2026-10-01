package com.jbkloh.ficha_buffet.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.dtos.req.CreateOrcamentoRequest;
import com.jbkloh.ficha_buffet.dtos.resp.OrcamentoDetalhadoDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.OrcamentoEntity;
import com.jbkloh.ficha_buffet.repositories.OrcamentoRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;

    @Transactional 
    public OrcamentoDetalhadoDTO criarOrcamento(CreateOrcamentoRequest req) {
        OrcamentoEntity entity = new OrcamentoEntity(
            null,
            req.nomeCliente().trim(),
            aparar(req.contatoCliente()),
            aparar(req.tipoServico()),
            req.duracaoEvento(),
            req.numeroConvidados(),
            req.valorAlimentos(),
            req.valorEquipe(),
            req.valorDegustacao(),
            req.valorOutros(),
            req.valorTotal(),
            req.valorPorPessoa()
        );
        return new OrcamentoDetalhadoDTO(orcamentoRepository.save(entity));
    }

    @Transactional (readOnly = true)
    public List<OrcamentoDetalhadoDTO> listarOrcamentos() {
        return orcamentoRepository.findAll()
                .stream()
                .map(OrcamentoDetalhadoDTO::new)
                .toList();
    }

    @Transactional 
    public void apagarOrcamento(Long id) {
        OrcamentoEntity entity = orcamentoRepository.findById(id)
            .orElseThrow(() -> new AppException("O orçamento de ID " + id + " não existe.", HttpStatus.NOT_FOUND));
        orcamentoRepository.delete(entity);
    }

    // Contato e tipo de serviço são opcionais no frontend.
    private String aparar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
