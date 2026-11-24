package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Insumo da lista de compras do plano: o que os itens a produzir precisam, o saldo em estoque e o que falta comprar. */
public record CompraPlanoDTO(Long insumoId, String nome, String unidade, double precisa, double estoque, double falta, double custo, boolean semPreco,
        List<String> usos) {}
