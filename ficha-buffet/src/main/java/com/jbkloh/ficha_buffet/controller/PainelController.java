package com.jbkloh.ficha_buffet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.resp.PainelResumoResponseDTO;
import com.jbkloh.ficha_buffet.service.PainelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/painel")
@RequiredArgsConstructor
public class PainelController {

    private final PainelService painelService;

    /** KPIs do mês, alertas, próximos eventos e séries dos gráficos, já calculados. */
    @GetMapping("/resumo")
    public ResponseEntity<PainelResumoResponseDTO> resumo() {
        return ResponseEntity.ok(painelService.resumo());
    }
}
