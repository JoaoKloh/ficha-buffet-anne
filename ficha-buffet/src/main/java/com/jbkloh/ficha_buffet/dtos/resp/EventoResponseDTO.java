package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.jbkloh.ficha_buffet.model.json.EnvioExtra;
import com.jbkloh.ficha_buffet.model.json.PlanoCozinhaItem;

/** Evento do buffet. Datas e horas vazias saem como "" (a combinar). */
public record EventoResponseDTO(Long id, String codigo, String nome, Long clienteId, String tipo, String status, String dataEvento, String horaInicio,
        String local, String cidade, Integer convidadosPrevistos, double margem, String observacoes, Contrato contrato, String cardapioFechadoEm,
        List<EnvioExtra> envioExtras, List<PlanoCozinhaItem> planoCozinha, String equipeCozinha, String criadoEm) {

    public record Contrato(String numero, String dataAssinatura, int convidadosContratados, double valorPorConvidado, double valorTotal,
            double valorConvidadoExtra, Long categoriaId) {}
}
