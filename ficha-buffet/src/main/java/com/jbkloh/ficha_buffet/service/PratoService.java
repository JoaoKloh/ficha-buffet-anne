package com.jbkloh.ficha_buffet.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.dtos.req.AssociationPratoProducaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreatePratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.IngredienteDTO;
import com.jbkloh.ficha_buffet.dtos.req.PratoDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdatePratoRequest;
import com.jbkloh.ficha_buffet.dtos.resp.PratoDetalhadoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.PratoEntity;
import com.jbkloh.ficha_buffet.model.ProducaoEntity;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.PratoRepository;
import com.jbkloh.ficha_buffet.repositories.ProducaoRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PratoService {

    private final PratoRepository pratoRepository;
    private final IngredientesRepository ingredientesRepository;
    private final ProducaoRepository producaoRepository;

    @Transactional
    public List<PratoEntity> salvarPratosComIngredientes(List<PratoDTO> pratosDTO) {
        List<PratoEntity> pratos = new ArrayList<>();
        for (PratoDTO pratoDto : pratosDTO) {

            // Instancia e preenche a entidade do Prato
            PratoEntity pratoEntity = new PratoEntity();
            pratoEntity.setNome(pratoDto.nome());
            pratoEntity.setCategoria(pratoDto.categoria());
            pratoEntity.setRendimentoReceita(pratoDto.rendQtd());
            pratoEntity.setUnidade(pratoDto.rendUnid());
            pratoEntity.setRendimentoPessoa(pratoDto.porPessoa());
            pratoEntity.setDescricao(pratoDto.receita());
            pratoEntity.setUrlImagem(pratoDto.foto());

            // Processa a lista de ingredientes apenas se ela existir e NÃO estiver vazia
            if (pratoDto.ingredientes() != null && !pratoDto.ingredientes().isEmpty()) {
                for (IngredienteDTO ingDto : pratoDto.ingredientes()) {

                    // Busca o ingrediente para verificar se ele já existe
                    Optional<IngredientesEntity> ingredienteOptional = 
                        ingredientesRepository.findByNomeAndUnidadeMedida(ingDto.nome(), ingDto.unidade());

                    IngredientesEntity ingrediente;

                    // Estrutura IF/ELSE utilizando Optional sem o uso do orElseThrow
                    if (ingredienteOptional.isPresent()) {
                        ingrediente = ingredienteOptional.get();
                    } else {
                        ingrediente = new IngredientesEntity();
                        ingrediente.setNome(ingDto.nome());
                        ingrediente.setUnidade(ingDto.unidade());
                        ingrediente = ingredientesRepository.save(ingrediente);
                    }

                    // Associa o ingrediente ao prato
                    pratoEntity.addIngrediente(ingrediente, ingDto.qtd());
                }
            }

            pratos.add(pratoEntity);
            pratoRepository.save(pratoEntity);
        }
        return pratos;
    }
    @Transactional
    public PratoEntity criarPrato(CreatePratoRequestDTO dto) {
        PratoEntity prato = new PratoEntity();
        prato.setNome(dto.nome());
        prato.setCategoria(dto.categoria());
        prato.setUrlImagem(dto.foto());
        prato.setRendimentoReceita(dto.rendQtd());
        prato.setUnidade(dto.rendUnid());
        prato.setRendimentoPessoa(dto.porPessoa());
        prato.setDescricao(dto.receita());

        if (dto.ingredientes() != null) {
            dto.ingredientes().forEach(itemDto -> {
                IngredientesEntity ingrediente = ingredientesRepository.findById(itemDto.ingredienteId())
                        .orElseThrow(() -> new RuntimeException("Ingrediente não encontrado: " + itemDto.ingredienteId()));
                prato.addIngrediente(ingrediente, itemDto.qtd());
            });
        }

        return pratoRepository.save(prato);
    }

@Transactional 
public void atualizarPrato(UpdatePratoRequest req) {

    PratoEntity pratoEntity = pratoRepository.findById(req.id())
        .orElseThrow(() -> new AppException("O prato com id " + req.id() + " não existe.", HttpStatus.BAD_REQUEST));

    pratoEntity.setNome(req.nome());
    pratoEntity.setCategoria(req.categoria());
    pratoEntity.setUrlImagem(req.foto());
    pratoEntity.setRendimentoReceita(req.rendQtd());
    pratoEntity.setUnidade(req.rendUnid());
    pratoEntity.setRendimentoPessoa(req.porPessoa());
    pratoEntity.setDescricao(req.receita());

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
        PratoEntity prato = pratoRepository.findById(id)
            .orElseThrow(() -> new AppException("Prato não encontrado", HttpStatus.NOT_FOUND));
        
        prato.getItensIngredientes().clear();
        pratoRepository.delete(prato);
    }

    @Transactional
    public void associarEventos(AssociationPratoProducaoRequestDTO req) {
        PratoEntity prato = pratoRepository.findById(req.pratoId())
            .orElseThrow(() -> new AppException(
                "O prato de ID: " + req.pratoId() + " não está cadastrado.", 
                HttpStatus.NOT_FOUND
            ));

        List<ProducaoEntity> eventos = req.eventosId().stream()
            .map(eventoId -> producaoRepository.findById(eventoId)
                .orElseThrow(() -> new AppException(
                    "O evento/produção de ID: " + eventoId + " não foi encontrado.", 
                    HttpStatus.NOT_FOUND
                ))
            )
            .toList();

        for (ProducaoEntity evento : eventos) {
            if (!evento.getPratos().contains(prato)) {
                evento.getPratos().add(prato);
            }
        }

        producaoRepository.saveAll(eventos);
    }
}

