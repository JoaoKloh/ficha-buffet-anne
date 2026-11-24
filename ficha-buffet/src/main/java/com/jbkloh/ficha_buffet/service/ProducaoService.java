package com.jbkloh.ficha_buffet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.dtos.resp.ProducaoResponseDTO;
import com.jbkloh.ficha_buffet.repositories.EventoRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class ProducaoService {

    private final EventoRepository producaoRepository;

    @Transactional (readOnly = true)
    public List<ProducaoResponseDTO> listarEventos() {
        return producaoRepository.findAll()
                .stream()
                .map(ProducaoResponseDTO::new)
                .toList();
    }

}
