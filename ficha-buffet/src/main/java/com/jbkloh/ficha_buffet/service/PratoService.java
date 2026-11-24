package com.jbkloh.ficha_buffet.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.dtos.req.AssociationPratoProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreatePratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdatePratoRequest;
import com.jbkloh.ficha_buffet.dtos.resp.PratoDetalhadoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.PreparacaoEntity;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.PreparacaoRepository;
import com.jbkloh.ficha_buffet.repositories.EventoRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PratoService {

    private final PreparacaoRepository pratoRepository;
    private final IngredientesRepository ingredientesRepository;
    private final EventoRepository producaoRepository;

    @Transactional
    public void criarPrato(CreatePratoRequestDTO dto) {
        PreparacaoEntity prato = CreatePratoRequestDTO.toEntity(dto);

        if (dto.ingredientes() != null) {
            dto.ingredientes().forEach(itemDto -> {
                IngredientesEntity ingrediente = ingredientesRepository.findById(itemDto.ingredienteId())
                        .orElseThrow(() -> new AppException("Ingrediente não encontrado: " + itemDto.ingredienteId(), HttpStatus.BAD_REQUEST));
                prato.addIngrediente(ingrediente, itemDto.qtd());
            });
        }

         pratoRepository.save(prato);
    }

@Transactional 
public void atualizarPrato(UpdatePratoRequest req) {

    PreparacaoEntity pratoEntity = pratoRepository.findById(req.id())
        .orElseThrow(() -> new AppException("O prato com id " + req.id() + " não existe.", HttpStatus.BAD_REQUEST));

    UpdatePratoRequest.atualizarEntidade(req, pratoEntity);

    // 1. Limpa os ingredientes atuais do prato (mantenha o orphanRemoval = true na Entidade Prato)
    pratoEntity.getItensIngredientes().clear();

    if (req.ingredientes() != null && !req.ingredientes().isEmpty()) {
        Set<Long> ingredientesProcessados = new HashSet<>();

        req.ingredientes().forEach(itemReq -> {
            if (!ingredientesProcessados.add(itemReq.ingredienteId())) {
                throw new AppException("O ingrediente com ID " + itemReq.ingredienteId() + " foi informado mais de uma vez para este prato.", HttpStatus.BAD_REQUEST);
            }

            IngredientesEntity ingredienteEntity = ingredientesRepository.findById(itemReq.ingredienteId())
                .orElseThrow(() -> new AppException("O ingrediente com id " + itemReq.ingredienteId() + " não existe.", HttpStatus.BAD_REQUEST));

            pratoEntity.addIngrediente(ingredienteEntity, itemReq.qtd());
        });
    }
}
    @Transactional(readOnly = true)
    public List<PratoDetalhadoResponseDTO> listarPratos() {
    return pratoRepository.findAll()
            .stream()
            .map(PratoDetalhadoResponseDTO::new)
            .toList();
    }
    
    @Transactional 
    public void apagarPrato(Long id) {
        PreparacaoEntity prato = pratoRepository.findById(id)
            .orElseThrow(() -> new AppException("Prato não encontrado", HttpStatus.NOT_FOUND));
        
        prato.getItensIngredientes().clear();
        pratoRepository.delete(prato);
    }

    @Transactional
    public void associarEventos(AssociationPratoProducaoRequestDTO req) {
        PreparacaoEntity prato = pratoRepository.findById(req.pratoId())
            .orElseThrow(() -> new AppException(
                "O prato de ID: " + req.pratoId() + " não está cadastrado.", 
                HttpStatus.NOT_FOUND
            ));

        List<EventoEntity> eventos = req.eventosId().stream()
            .map(eventoId -> producaoRepository.findById(eventoId)
                .orElseThrow(() -> new AppException(
                    "O evento/produção de ID: " + eventoId + " não foi encontrado.", 
                    HttpStatus.NOT_FOUND
                ))
            )
            .toList();

        for (EventoEntity evento : eventos) {
            boolean jaNoCardapio = evento.getCardapio().stream()
                .anyMatch(item -> item.getPreparacao().getId().equals(prato.getId()));
            if (!jaNoCardapio) {
                evento.addPreparacao(prato);
            }
        }

        producaoRepository.saveAll(eventos);
    }
}

