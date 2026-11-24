package com.jbkloh.ficha_buffet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.calculo.Painel;
import com.jbkloh.ficha_buffet.dtos.resp.PainelResumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.UltimoBackupDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PainelService {

    private final DadosService dadosService;
    private final BackupService backupService;

    /** Tudo que o Painel mostra, pronto: KPIs do mês, alertas, próximos eventos e séries dos gráficos. */
    @Transactional(readOnly = true)
    public PainelResumoResponseDTO resumo() {
        Dados d = dadosService.carregar();
        UltimoBackupDTO ultimo = backupService.ultimoBackup();
        String hoje = Formato.hoje();
        return new PainelResumoResponseDTO(hoje, hoje.substring(0, 7), Painel.kpis(d, hoje), Painel.alertas(d, ultimo == null ? null : ultimo.ultimoEm(), hoje),
            Painel.proximosEventos(d, hoje), Painel.graficos(d, hoje), Painel.unidades(d, hoje));
    }
}
