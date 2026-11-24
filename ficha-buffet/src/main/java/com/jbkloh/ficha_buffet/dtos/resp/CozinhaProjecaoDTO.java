package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Evento projetado para a cozinha, sem nenhum valor financeiro. producao = mapa de produção (nulo = ainda não gerado). */
public record CozinhaProjecaoDTO(Long eventoId, String codigo, String nome, String data, String hora, String local, String cidade, String status,
        @JsonProperty("N") int n, int base, double margem, boolean fechado, String obs, String equipe, List<Prato> pratos, List<Compra> compras,
        List<Louca> loucas, List<Tarefa> tarefas, List<Extra> extras, MapaProducaoDTO producao) {

    public record InsumoPrato(String k, String n, String u, String via, String cons, double qb, double qt) {}

    public record Componente(String k, String n, String u, double q, double prev, String cons, String obs) {}

    public record Etapa(String desc, String praca, int dn) {}

    public record LoucaPrato(String n, double q) {}

    /** dnf = antecedência efetiva da produção (ficha ou padrão da praça); pronto = comprado pronto do fornecedor. */
    public record Prato(Long cid, String nome, String cat, String praca, String resp, String st, double porcao, String fator, double total, String uu,
            String ulabel, double rend, String rendLabel, double lotes, String conserv, String ficha, String modo, String obsEnvio, List<Etapa> etapas,
            List<InsumoPrato> ins, List<LoucaPrato> loucas, List<Componente> comp, int dnf, boolean pronto) {}

    public record Compra(Long id, String n, String u, String cat, double q, double qd, double est, double fc, String uc, String usos) {}

    public record Louca(Long id, String n, double q, String usos) {}

    /** dn = dias antes do evento; -1 = a posicionar. */
    public record Tarefa(String k, String desc, String praca, int dn, String origem, String produtos, String resp, boolean padrao) {}

    public record Extra(String k, String sec, String n, String u, double q, String obs) {}
}
