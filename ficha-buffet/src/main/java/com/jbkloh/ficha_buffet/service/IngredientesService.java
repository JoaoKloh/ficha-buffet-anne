package com.jbkloh.ficha_buffet.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.dtos.req.AssociationIngredientePratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CreateIngredienteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.UpdateIngredienteRequestDto;
import com.jbkloh.ficha_buffet.dtos.resp.IngredienteResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.PratoEntity;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.PratoRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class IngredientesService {
    
    private final IngredientesRepository ingredientesRepository;
    private final PratoRepository pratoRepository;

    @Transactional 
    public void criarIngrediente(CreateIngredienteRequestDTO req){
        Optional<IngredientesEntity> ingredienteExistente = ingredientesRepository.findByNome(req.nome());
        if(ingredienteExistente.isPresent()){
            throw new AppException("Já existe um ingrediente com o nome: " + req.nome(), HttpStatus.BAD_REQUEST);
    }else{
        IngredientesEntity ingredientesEntity = new IngredientesEntity(null, req.categoria(),req.nome(), req.unidade(), req.custo(), req.fornecedor());
            ingredientesRepository.save(ingredientesEntity);
        
    }
}

    @Transactional 
    public void atualizarIngrediente(UpdateIngredienteRequestDto req){
        IngredientesEntity ingredientesEntity = ingredientesRepository.findById(req.id())
        .orElseThrow(()->new AppException("O ingrediente de id, "+req.id()+", não existe.", HttpStatus.BAD_REQUEST));

        ingredientesEntity.setCategoria(req.categoria());
        ingredientesEntity.setNome(req.nome());
        ingredientesEntity.setCusto(req.custo());
        ingredientesEntity.setFornecedor(req.fornecedor());
        ingredientesEntity.setUnidade(req.unidade());

        ingredientesRepository.save(ingredientesEntity);
    }

    @Transactional(readOnly = true)
    public List<IngredienteResponseDTO> listarIngredientes() {
        return ingredientesRepository.findAll()
                .stream()
                .map(IngredienteResponseDTO::new) 
                .toList();
    }

    @Transactional
    public void associarPratos(AssociationIngredientePratoRequestDTO req) {
        IngredientesEntity ingrediente = ingredientesRepository.findById(req.ingredienteId())
            .orElseThrow(() -> new AppException(
                "O ingrediente de ID: " + req.ingredienteId() + " não está cadastrado.", 
                HttpStatus.NOT_FOUND
            ));

        List<PratoEntity> pratos = req.pratosId().stream()
            .map(pratoid -> pratoRepository.findById(pratoid)
                .orElseThrow(() -> new AppException(
                    "O prato de ID: " + pratoid + " não foi encontrado.", 
                    HttpStatus.NOT_FOUND
                ))
            )
            .toList();

        for (PratoEntity prato : pratos) {
            boolean jaAssociado = prato.getItensIngredientes().stream()
                .anyMatch(item -> item.getIngrediente().getId().equals(ingrediente.getId()));

            if (!jaAssociado) {
                prato.addIngrediente(ingrediente, 1.0);
            }
        }

        pratoRepository.saveAll(pratos);
    }

    @Transactional 
    public void apagarIngrediente(Long id){
        ingredientesRepository.deleteById(id);
    }
}
