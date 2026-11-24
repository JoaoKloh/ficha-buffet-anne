package com.jbkloh.ficha_buffet.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.GrupoOpcoesDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;

/** Opções agrupadas dos selects, compartilhadas pelos formulários de evento, ficha e estoque. */
final class Opcoes {

    private Opcoes() {}

    /** Fichas por categoria, na ordem das categorias; `comOutros` inclui as de categoria desconhecida. */
    static List<GrupoOpcoesDTO> preparacoesPorCategoria(Dados d, boolean comOutros) {
        List<PreparacaoResponseDTO> ordenadas = d.preparacoes.stream().sorted(Formato.porNome(PreparacaoResponseDTO::nome)).toList();
        List<GrupoOpcoesDTO> grupos = new ArrayList<>();
        for (Map.Entry<String, String> cat : Dominio.CATEGORIAS_PREPARACAO.entrySet()) {
            grupos.add(new GrupoOpcoesDTO(cat.getValue(), ordenadas.stream().filter(p -> cat.getKey().equals(p.categoria()))
                .map(p -> GestaoMapper.par(p.id(), p.nome())).toList()));
        }
        if (comOutros) {
            grupos.add(new GrupoOpcoesDTO("Outros", ordenadas.stream().filter(p -> !Dominio.CATEGORIAS_PREPARACAO.containsKey(p.categoria()))
                .map(p -> GestaoMapper.par(p.id(), p.nome())).toList()));
        }
        return grupos.stream().filter(g -> !g.itens().isEmpty()).toList();
    }

    /** Itens por setor de compra, com o rótulo "Nome (unidade)". */
    static List<GrupoOpcoesDTO> insumosPorCategoria(List<InsumoResponseDTO> insumos) {
        List<InsumoResponseDTO> ordenados = insumos.stream().sorted(Formato.porNome(InsumoResponseDTO::nome)).toList();
        return Dominio.CATEGORIAS_INSUMO.stream()
            .map(g -> new GrupoOpcoesDTO(g, ordenados.stream().filter(i -> g.equals(Formato.vazio(i.categoria()) ? "Outros" : i.categoria()))
                .map(i -> GestaoMapper.par(i.id(), i.nome() + " (" + i.unidade() + ")")).toList()))
            .filter(g -> !g.itens().isEmpty()).toList();
    }

    /** Categorias financeiras dos grupos pedidos, agrupadas pelo rótulo do grupo. */
    static List<GrupoOpcoesDTO> categoriasPorGrupo(Dados d, List<String> grupos) {
        List<CategoriaResponseDTO> categorias = Caixa.categoriasOrdenadas(d);
        return grupos.stream()
            .map(g -> new GrupoOpcoesDTO(Dominio.GRUPOS_DRE.get(g), categorias.stream().filter(c -> g.equals(c.grupo()))
                .map(c -> GestaoMapper.par(c.id(), c.nome())).toList()))
            .filter(g -> !g.itens().isEmpty()).toList();
    }
}
