package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Estoque;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.InventarioRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentoEstoqueRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.TransferenciaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentosResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.MovimentoEstoqueEntity;
import com.jbkloh.ficha_buffet.repositories.MovimentoEstoqueRepository;

import lombok.RequiredArgsConstructor;

/** Movimentos de estoque: lista com as baixas calculadas, lançamento manual e inventário. */
@Service
@RequiredArgsConstructor
public class MovimentoEstoqueService {

    private static final Map<String, Predicate<MovimentoResponseDTO>> FILTROS = Map.of(
        "todos", m -> true,
        "entradas", m -> m.qtd() > 0 && !"inventario".equals(m.tipo()) && !"ajuste".equals(m.tipo()),
        "saidas", m -> "perda".equals(m.tipo()) || "devolucao".equals(m.tipo()) || "baixa".equals(m.tipo()) || "consumo".equals(m.tipo()),
        "contagens", m -> "inventario".equals(m.tipo()) || "ajuste".equals(m.tipo()));

    /** Mais recentes primeiro: pela data do movimento e, no mesmo dia, pelo registro. */
    static final Comparator<MovimentoResponseDTO> MAIS_RECENTES = Comparator.comparing(MovimentoResponseDTO::data)
        .thenComparing(m -> Formato.texto(m.criadoEm())).reversed();

    private final MovimentoEstoqueRepository movimentoRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;
    private final CaixaService caixaService;

    static MovimentoListaItemDTO listaItem(Dados d, MovimentoResponseDTO m) {
        InsumoResponseDTO i = d.insumo(m.insumoId());
        return new MovimentoListaItemDTO(m, i == null ? "Item removido" : i.nome(), d.nomeEvento(m.eventoId()));
    }

    /** Movimentos gravados e baixas calculadas do filtro (todos, entradas, saidas ou contagens): os 300 mais recentes e o total. */
    @Transactional(readOnly = true)
    public MovimentosResponseDTO listarMovimentos(String filtro) {
        Dados d = dadosService.carregar();
        List<MovimentoResponseDTO> todos = new ArrayList<>(d.movimentos);
        todos.addAll(Estoque.baixasVirtuais(d));
        List<MovimentoResponseDTO> filtrados = todos.stream().filter(FILTROS.getOrDefault(Formato.texto(filtro), FILTROS.get("todos"))).sorted(MAIS_RECENTES).toList();
        return new MovimentosResponseDTO(filtrados.stream().limit(300).map(m -> listaItem(d, m)).toList(), filtrados.size());
    }

    /**
     * Ajuste grava a diferença para a contagem real; entrada com preço atualiza o custo do item
     * e guarda a validade. Consumo guarda a unidade que usou e, se outra unidade pagou a compra,
     * lança a transferência do custo (quantidade × preço atual do item) na linha de A&B.
     */
    @Transactional
    public MovimentoResponseDTO criarMovimento(MovimentoEstoqueRequestDTO req) {
        IngredientesEntity item = cadastro.ingredienteDoCorpo(req.insumoId(), "Escolha o item.");
        String tipo = req.tipo();
        boolean ajuste = "ajuste".equals(tipo);
        boolean consumo = "consumo".equals(tipo);
        String motivo = Formato.texto(req.motivo()).trim();
        Dados d = dadosService.carregar();
        double saldo = Estoque.calcular(d).fisico(item.getId());
        if (ajuste && Math.abs(req.qtd() - saldo) < 1e-9) {
            throw new AppException("A contagem é igual ao saldo: não há o que ajustar.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!ajuste && !(req.qtd() > 0)) {
            throw new AppException("Informe a quantidade.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if ((ajuste || "perda".equals(tipo) || "devolucao".equals(tipo)) && motivo.isEmpty()) {
            throw new AppException("Informe o motivo.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        BigDecimal custo = "entrada".equals(tipo) && req.custoUnit() != null && req.custoUnit().signum() > 0
            ? req.custoUnit().setScale(4, RoundingMode.HALF_UP) : null;
        MovimentoEstoqueEntity m = novo(item, tipo, ajuste ? Formato.r3(req.qtd() - saldo) : Dominio.SINAL_MOVIMENTO.get(tipo) * req.qtd(), req.qtd(),
            Formato.data(req.data(), "data"), motivo, consumo ? "consumo" : "manual");
        m.setCustoUnit(custo);
        // Só sobra e perda pertencem a um evento.
        m.setEvento("sobra".equals(tipo) || "perda".equals(tipo) ? cadastro.eventoDoCorpo(req.eventoId()) : null);
        // Validade só na entrada; unidade que usou só no consumo.
        m.setValidade("entrada".equals(tipo) ? Formato.dataOpcional(req.validade(), "validade") : null);
        m.setUnidadeUso(consumo ? req.unidadeUso() : null);
        if (custo != null) {
            item.setCusto(custo);
            item.setAtualizadoEm(LocalDate.now());
        }
        MovimentoResponseDTO salvo = GestaoMapper.movimento(movimentoRepository.save(m));
        if (consumo && req.compradoPor() != null && !req.compradoPor().equals(req.unidadeUso())) {
            transferirCusto(d, item, req, motivo);
        }
        return salvo;
    }

    /** Outra unidade pagou a compra: o custo do que foi usado passa para quem usou (Caixa › Transferências). Sem preço ou sem linha de A&B, não há o que transferir. */
    private void transferirCusto(Dados d, IngredientesEntity item, MovimentoEstoqueRequestDTO req, String motivo) {
        double valor = Formato.round2(req.qtd() * (item.getCusto() == null ? 0 : item.getCusto().doubleValue()));
        CategoriaResponseDTO ab = Caixa.categoriaAB(d);
        if (!(valor > 0) || ab == null) {
            return;
        }
        caixaService.criarTransferencia(new TransferenciaRequestDTO(req.data(), "", req.compradoPor(), req.unidadeUso(), item.getId(), item.getNome(),
            req.qtd(), item.getUnidade(), BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP), ab.id(), motivo));
    }

    @Transactional
    public void apagarMovimento(Long id) {
        movimentoRepository.delete(movimentoRepository.findById(id)
            .orElseThrow(() -> new AppException("O movimento de id " + id + " não existe.", HttpStatus.NOT_FOUND)));
    }

    /** Cada item contado com diferença vira um movimento de inventário; devolve quantos foram lançados. */
    @Transactional
    public int lancarInventario(InventarioRequestDTO req) {
        Estoque estoque = Estoque.calcular(dadosService.carregar());
        boolean inicial = movimentoRepository.count() == 0;
        List<MovimentoEstoqueEntity> novos = new ArrayList<>();
        for (InventarioRequestDTO.Contagem c : req.contagens()) {
            IngredientesEntity item = cadastro.ingredienteDoCorpo(c.insumoId(), "Um dos itens contados não existe mais no cadastro.");
            double saldo = estoque.fisico(item.getId());
            if (Math.abs(c.contado() - saldo) > 1e-9) {
                novos.add(novo(item, "inventario", Formato.r3(c.contado() - saldo), c.contado(), LocalDate.now(),
                    inicial ? "Inventário inicial" : "Contagem de inventário", "inventario"));
            }
        }
        if (novos.isEmpty()) {
            throw new AppException("Nenhum item com diferença.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        movimentoRepository.saveAll(novos);
        return novos.size();
    }

    static MovimentoEstoqueEntity novo(IngredientesEntity item, String tipo, double efeito, double informada, LocalDate data, String motivo, String origem) {
        MovimentoEstoqueEntity m = new MovimentoEstoqueEntity();
        m.setTipo(tipo);
        m.setIngrediente(item);
        m.setQtd(efeito);
        m.setQtdInformada(informada);
        m.setUnidade(item.getUnidade());
        m.setData(data);
        m.setMotivo(motivo.length() > 300 ? motivo.substring(0, 300) : motivo);
        m.setOrigem(origem);
        return m;
    }
}
