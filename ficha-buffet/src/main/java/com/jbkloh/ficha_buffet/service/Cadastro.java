package com.jbkloh.ficha_buffet.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.CategoriaFinanceiraEntity;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.PreparacaoEntity;
import com.jbkloh.ficha_buffet.repositories.CategoriaFinanceiraRepository;
import com.jbkloh.ficha_buffet.repositories.EventoRepository;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.PreparacaoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Busca de registros que vários services referenciam. O recurso da própria
 * rota que não existe é 404; uma referência inválida dentro do corpo é 422.
 */
@Component
@RequiredArgsConstructor
public class Cadastro {

    private final EventoRepository eventoRepository;
    private final PreparacaoRepository preparacaoRepository;
    private final IngredientesRepository ingredientesRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;

    public EventoEntity evento(Long id) {
        return eventoRepository.findById(id).orElseThrow(() -> new AppException("Evento não encontrado.", HttpStatus.NOT_FOUND));
    }

    public PreparacaoEntity preparacao(Long id) {
        return preparacaoRepository.findById(id).orElseThrow(() -> new AppException("Ficha não encontrada.", HttpStatus.NOT_FOUND));
    }

    public IngredientesEntity ingrediente(Long id) {
        return ingredientesRepository.findById(id).orElseThrow(() -> new AppException("Item não encontrado.", HttpStatus.NOT_FOUND));
    }

    /** Evento opcional citado no corpo: nulo fica nulo. */
    public EventoEntity eventoDoCorpo(Long id) {
        if (id == null) {
            return null;
        }
        return eventoRepository.findById(id).orElseThrow(() -> new AppException("O evento informado não existe.", HttpStatus.UNPROCESSABLE_ENTITY));
    }

    public PreparacaoEntity preparacaoDoCorpo(Long id) {
        return preparacaoRepository.findById(id).orElseThrow(() -> new AppException("Escolha a preparação.", HttpStatus.UNPROCESSABLE_ENTITY));
    }

    public IngredientesEntity ingredienteDoCorpo(Long id, String mensagem) {
        return ingredientesRepository.findById(id).orElseThrow(() -> new AppException(mensagem, HttpStatus.UNPROCESSABLE_ENTITY));
    }

    public CategoriaFinanceiraEntity categoriaDoCorpo(Long id, String mensagem) {
        return categoriaRepository.findById(id).orElseThrow(() -> new AppException(mensagem, HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
