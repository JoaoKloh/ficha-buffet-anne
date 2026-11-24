package com.jbkloh.ficha_buffet.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Estoque;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.calculo.PlanosProducao;
import com.jbkloh.ficha_buffet.dtos.req.AdicionarItensPlanoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CalcularCartaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LotePreparoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarPlanoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoProducaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanosProducaoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.LotePreparoEntity;
import com.jbkloh.ficha_buffet.model.PlanoProducaoEntity;
import com.jbkloh.ficha_buffet.model.PlanoProducaoItemEntity;
import com.jbkloh.ficha_buffet.model.PreparacaoEntity;
import com.jbkloh.ficha_buffet.repositories.LotePreparoRepository;
import com.jbkloh.ficha_buffet.repositories.PlanoProducaoItemRepository;
import com.jbkloh.ficha_buffet.repositories.PlanoProducaoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Planos de produção do restaurante e da chocolates (tela Produção), separados dos eventos do buffet.
 * Concluir um item cria um lote em Estoque › Pré-preparos com a validade da ficha; é o lote que baixa
 * os insumos, uma vez só. A situação do plano acompanha os itens (protótipo: prodStatusAuto).
 */
@Service
@RequiredArgsConstructor
public class PlanoProducaoService {

    private final PlanoProducaoRepository planoRepository;
    private final PlanoProducaoItemRepository itemRepository;
    private final LotePreparoRepository loteRepository;
    private final LotePreparoService loteService;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    /* ------------------------------------ Planos ------------------------------------ */

    /** Planos da unidade com os KPIs da tela. */
    @Transactional(readOnly = true)
    public PlanosProducaoResponseDTO listar(String unidade) {
        if (unidade == null || !unidade.matches(Dominio.RE_UNIDADE_PLANO)) {
            throw new AppException("Unidade inválida.", HttpStatus.BAD_REQUEST);
        }
        Dados d = dadosService.carregar();
        return PlanosProducao.lista(d, unidade, planoRepository.findByUnidade(unidade).stream().map(GestaoMapper::planoProducao).toList());
    }

    /** Plano com a lista de produção, os insumos por item e a lista de compras. */
    @Transactional(readOnly = true)
    public PlanoDetalheResponseDTO detalhar(Long id) {
        PlanoProducaoEntity plano = buscar(id);
        Dados d = dadosService.carregar();
        return PlanosProducao.detalhe(d, GestaoMapper.planoProducao(plano), Estoque.calcular(d));
    }

    @Transactional
    public PlanoProducaoResponseDTO criar(SalvarPlanoRequestDTO req) {
        PlanoProducaoEntity plano = new PlanoProducaoEntity();
        aplicar(req, plano);
        return GestaoMapper.planoProducao(planoRepository.save(plano));
    }

    @Transactional
    public PlanoProducaoResponseDTO atualizar(Long id, SalvarPlanoRequestDTO req) {
        PlanoProducaoEntity plano = buscar(id);
        aplicar(req, plano);
        plano.setAtualizadoEm(Instant.now());
        return GestaoMapper.planoProducao(plano);
    }

    /** Exclui o plano e os itens; os lotes já registrados continuam em Pré-preparos. */
    @Transactional
    public void apagar(Long id) {
        planoRepository.delete(buscar(id));
    }

    private void aplicar(SalvarPlanoRequestDTO req, PlanoProducaoEntity plano) {
        LocalDate inicio = Formato.data(req.dataInicio(), "data de início");
        LocalDate fim = Formato.data(req.dataFim(), "data final");
        if (fim.isBefore(inicio)) {
            throw new AppException("A data final não pode ser antes do início.", HttpStatus.BAD_REQUEST);
        }
        plano.setTitulo(req.titulo().trim());
        plano.setUnidade(req.unidade());
        if (req.status() != null) {
            plano.setStatus(req.status());
        }
        plano.setDataInicio(inicio);
        plano.setDataFim(fim);
        plano.setObs(Formato.texto(req.obs()).trim());
    }

    /* ------------------------------------- Itens ------------------------------------- */

    /** Fichas novas no plano, com a quantidade na unidade de cada ficha. A mesma ficha não entra duas vezes enquanto não for concluída. */
    @Transactional
    public PlanoProducaoResponseDTO adicionarItens(Long id, AdicionarItensPlanoRequestDTO req) {
        PlanoProducaoEntity plano = buscar(id);
        Set<Long> aProduzir = new HashSet<>();
        plano.getItens().stream().filter(x -> !x.isFeito()).forEach(x -> aProduzir.add(x.getPreparacao().getId()));
        for (AdicionarItensPlanoRequestDTO.Item it : req.itens()) {
            PreparacaoEntity ficha = cadastro.preparacaoDoCorpo(it.preparacaoId());
            if (!aProduzir.add(ficha.getId())) {
                throw new AppException("A ficha \"" + ficha.getNome() + "\" já está no plano.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            novoItem(plano, ficha, it.qtd());
        }
        atualizarSituacao(plano);
        return GestaoMapper.planoProducao(plano);
    }

    /** Ajusta a quantidade de um item ainda não concluído. */
    @Transactional
    public PlanoProducaoResponseDTO ajustarItem(Long id, String k, double qtd) {
        PlanoProducaoEntity plano = buscar(id);
        PlanoProducaoItemEntity item = item(plano, k);
        if (item.isFeito()) {
            throw new AppException("Este item já foi concluído. Desfaça antes de mudar a quantidade.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        item.setQtd(Formato.r3(qtd));
        atualizarSituacao(plano);
        return GestaoMapper.planoProducao(plano);
    }

    /** Tira do plano um item ainda não concluído. */
    @Transactional
    public void removerItem(Long id, String k) {
        PlanoProducaoEntity plano = buscar(id);
        PlanoProducaoItemEntity item = item(plano, k);
        if (item.isFeito()) {
            throw new AppException("Item concluído não sai do plano. Desfaça antes.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        plano.getItens().remove(item);
        atualizarSituacao(plano);
    }

    /**
     * Conclui o item: lote em Pré-preparos produzido hoje, com a validade da ficha, destino
     * restaurante (chocolates: estoque geral) e a unidade do plano. Responde { loteId, codigo }.
     */
    @Transactional
    public Map<String, Object> concluirItem(Long id, String k) {
        PlanoProducaoEntity plano = buscar(id);
        PlanoProducaoItemEntity item = item(plano, k);
        if (item.isFeito()) {
            throw new AppException("Este item já foi concluído.", HttpStatus.CONFLICT);
        }
        PreparacaoEntity ficha = item.getPreparacao();
        LocalDate hoje = LocalDate.now();
        LocalDate validade = hoje.plusDays(Estoque.validadeDiasDe(GestaoMapper.preparacao(ficha)));
        String destino = "chocolates".equals(plano.getUnidade()) ? "geral" : "restaurante";
        LotePreparoEntity lote = loteService.registrarLote(new LotePreparoRequestDTO(ficha.getId(), item.getQtd(), hoje.toString(), validade.toString(),
            destino, null, "", "Plano de produção: " + plano.getTitulo()), plano.getUnidade());
        item.setFeito(true);
        item.setFeitoEm(hoje);
        item.setLote(lote);
        atualizarSituacao(plano);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("loteId", lote.getId());
        r.put("codigo", lote.getCodigo());
        return r;
    }

    /** Volta o item para a produção e apaga o lote (a baixa dos insumos sai junto). Lote com uso ou descarte não volta. */
    @Transactional
    public void desfazerItem(Long id, String k) {
        PlanoProducaoEntity plano = buscar(id);
        PlanoProducaoItemEntity item = item(plano, k);
        if (!item.isFeito()) {
            throw new AppException("Este item ainda não foi concluído.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        LotePreparoEntity lote = item.getLote();
        if (lote != null && !lote.getHistorico().isEmpty()) {
            throw new AppException("Este lote já teve uso ou descarte. Ajuste em Estoque › Pré-preparos.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        item.setFeito(false);
        item.setFeitoEm(null);
        item.setLote(null);
        if (lote != null) {
            // O item solta o lote antes da exclusão (o flush grava as atualizações antes das exclusões).
            loteRepository.delete(lote);
        }
        atualizarSituacao(plano);
    }

    /* ------------------------------------- Carta ------------------------------------- */

    /**
     * Pratos da carta previstos no período: as fichas que eles levam entram no plano ou atualizam
     * a quantidade do item ainda não concluído da mesma ficha. Só no restaurante. Responde { fichas }.
     */
    @Transactional
    public Map<String, Integer> calcularCarta(Long id, CalcularCartaRequestDTO req) {
        PlanoProducaoEntity plano = buscar(id);
        if (!"restaurante".equals(plano.getUnidade())) {
            throw new AppException("Calcular pela carta é só do restaurante.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Map<String, Integer> previsao = new LinkedHashMap<>();
        req.previsao().forEach((pv, n) -> {
            if (n != null && n > 0) {
                previsao.put(pv, n);
            }
        });
        Map<Long, Double> need = PlanosProducao.necessidadeDaCarta(dadosService.carregar(), previsao);
        int fichas = 0;
        for (Map.Entry<Long, Double> e : need.entrySet()) {
            double q = Formato.r3(e.getValue());
            if (!(q > 0)) {
                continue;
            }
            PlanoProducaoItemEntity aberto = plano.getItens().stream().filter(x -> !x.isFeito() && x.getPreparacao().getId().equals(e.getKey())).findFirst()
                .orElse(null);
            if (aberto != null) {
                aberto.setQtd(q);
            } else {
                novoItem(plano, cadastro.preparacao(e.getKey()), q);
            }
            fichas++;
        }
        if (fichas == 0) {
            throw new AppException("Os pratos previstos não têm fichas. Confira em Fichas › Pratos do restaurante.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        plano.setPrevisao(previsao);
        atualizarSituacao(plano);
        return Map.of("fichas", fichas);
    }

    /* ------------------------------------ Apoio ------------------------------------ */

    /** Item novo já gravado: a chave do item (o id) sai na resposta. */
    private void novoItem(PlanoProducaoEntity plano, PreparacaoEntity ficha, double qtd) {
        PlanoProducaoItemEntity item = new PlanoProducaoItemEntity();
        item.setPlano(plano);
        item.setPreparacao(ficha);
        item.setQtd(Formato.r3(qtd));
        plano.getItens().add(itemRepository.save(item));
    }

    private static void atualizarSituacao(PlanoProducaoEntity plano) {
        int feitos = (int) plano.getItens().stream().filter(PlanoProducaoItemEntity::isFeito).count();
        plano.setStatus(PlanosProducao.statusAuto(plano.getItens().size(), feitos, plano.getStatus()));
        plano.setAtualizadoEm(Instant.now());
    }

    private PlanoProducaoEntity buscar(Long id) {
        return planoRepository.findById(id).orElseThrow(() -> new AppException("Plano de produção não encontrado.", HttpStatus.NOT_FOUND));
    }

    /** Item do plano pela chave (o id da linha). */
    private static PlanoProducaoItemEntity item(PlanoProducaoEntity plano, String k) {
        Long itemId = k != null && k.matches("\\d{1,18}") ? Long.valueOf(k) : null;
        return plano.getItens().stream().filter(x -> x.getId().equals(itemId)).findFirst()
            .orElseThrow(() -> new AppException("Este item não está mais no plano. Atualize a tela.", HttpStatus.NOT_FOUND));
    }
}
