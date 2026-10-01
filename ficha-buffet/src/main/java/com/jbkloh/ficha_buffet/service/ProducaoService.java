package com.jbkloh.ficha_buffet.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.dtos.req.CreateProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdateProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ProducaoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.PratoEntity;
import com.jbkloh.ficha_buffet.model.ProducaoEntity;
import com.jbkloh.ficha_buffet.repositories.PratoRepository;
import com.jbkloh.ficha_buffet.repositories.ProducaoRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class ProducaoService {

    private final ProducaoRepository producaoRepository;
    private final PratoRepository pratoRepository;

    @Transactional 
    public void criarEvento(CreateProducaoRequestDTO req){
        List<PratoEntity> pratos = req.pratos().stream()
            .map(item -> pratoRepository.findById(item.pratoId())
                .orElseThrow(() -> new AppException(
                    "O item de id: " + item.pratoId() + " não está cadastrado.", 
                    HttpStatus.BAD_REQUEST
                ))
            )
            .toList();
        ProducaoEntity entity = new ProducaoEntity(null,req.nome(), req.quantidade(), req.data(), pratos);
        producaoRepository.save(entity);
    }

    @Transactional 
    public void atualizarEvento(UpdateProducaoRequestDTO req) {

        ProducaoEntity producaoEntity = producaoRepository.findById(req.id())
            .orElseThrow(() -> new AppException("A produção de ID " + req.id() + " não existe.", HttpStatus.BAD_REQUEST));

        Set<Long> idsRecebidos = new HashSet<>();
        for (var item : req.pratos()) {
            if (!idsRecebidos.add(item.pratoId())) {
                throw new AppException("O prato de ID " + item.pratoId() + " foi informado mais de uma vez na requisição.", HttpStatus.BAD_REQUEST);
            }
        }

        List<PratoEntity> novosPratos = req.pratos().stream()
            .map(item -> pratoRepository.findById(item.pratoId())
                .orElseThrow(() -> new AppException(
                    "O prato de ID " + item.pratoId() + " não está cadastrado.", 
                    HttpStatus.BAD_REQUEST
                ))
            )
            .toList();

        producaoEntity.setNome(req.nome());
        producaoEntity.setData(req.data());
        producaoEntity.setQuantidade(req.quantidade());

        // 4. Sincronizar a coleção de pratos (apaga as antigas e insere as novas)
        producaoEntity.getPratos().clear();
        producaoEntity.getPratos().addAll(novosPratos);

        }

    @Transactional (readOnly = true)
    public List<ProducaoResponseDTO> listarEventos() {
        return producaoRepository.findAll()
                .stream()
                .map(ProducaoResponseDTO::new)
                .toList();
    }

    @Transactional 
    public void apagarProducao(Long id){
        ProducaoEntity evento = producaoRepository.findById(id)
        .orElseThrow(() -> new AppException("Prato não encontrado", HttpStatus.NOT_FOUND));

        evento.getPratos().clear();

        producaoRepository.delete(evento);
    }
}
