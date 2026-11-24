package com.jbkloh.ficha_buffet.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Fichas;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.CalcularCustoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PreparacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CustoPreparacaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FichasOpcoesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SetorProducaoDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.PratoIngredienteEntity;
import com.jbkloh.ficha_buffet.model.PratoLoucaEntity;
import com.jbkloh.ficha_buffet.model.PreparacaoEntity;
import com.jbkloh.ficha_buffet.model.json.ComponenteEnvio;
import com.jbkloh.ficha_buffet.model.json.EtapaPreparacao;
import com.jbkloh.ficha_buffet.repositories.ItemCardapioRepository;
import com.jbkloh.ficha_buffet.repositories.LotePreparoRepository;
import com.jbkloh.ficha_buffet.repositories.PlanoProducaoItemRepository;
import com.jbkloh.ficha_buffet.repositories.PratoVendaRepository;
import com.jbkloh.ficha_buffet.repositories.PreparacaoRepository;

import lombok.RequiredArgsConstructor;

/** Fichas técnicas no contrato do Anne Gestão: receita com sub-receitas, louças, etapas e componentes de envio. */
@Service
@RequiredArgsConstructor
public class FichaService {

    private final PreparacaoRepository preparacaoRepository;
    private final ItemCardapioRepository itemCardapioRepository;
    private final PratoVendaRepository pratoVendaRepository;
    private final LotePreparoRepository loteRepository;
    private final PlanoProducaoItemRepository planoItemRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    @Transactional(readOnly = true)
    public FichasOpcoesResponseDTO opcoes() {
        Dados d = dadosService.carregar();
        List<InsumoResponseDTO> consumiveis = d.insumos.stream().filter(i -> !Dominio.reutilizavel(i.tipo())).toList();
        Map<Long, String> unidades = new LinkedHashMap<>();
        d.insumos.forEach(i -> unidades.put(i.id(), i.unidade()));
        return new FichasOpcoesResponseDTO(
            Opcoes.insumosPorCategoria(consumiveis),
            d.insumos.stream().filter(i -> Dominio.reutilizavel(i.tipo())).sorted(Formato.porNome(InsumoResponseDTO::nome))
                .map(i -> GestaoMapper.par(i.id(), i.nome())).toList(),
            // Sub-receitas (base) primeiro: são as mais escolhidas dentro de outra ficha.
            d.preparacoes.stream()
                .sorted(Comparator.<PreparacaoResponseDTO>comparingInt(p -> "base".equals(p.categoria()) ? 0 : 1).thenComparing(Formato.porNome(PreparacaoResponseDTO::nome)))
                .map(p -> new FichasOpcoesResponseDTO.Preparacao(p.id(), p.nome(), p.categoria(), Fichas.unidRendLabel(p.unidadeRendimento(), 2), p.porcaoPadrao()))
                .toList(),
            unidades,
            Opcoes.preparacoesPorCategoria(d, true),
            d.setores.stream().filter(SetorProducaoDTO::ativo).map(x -> List.<Object>of(x.id(), x.nome())).toList());
    }

    /** Setor de produção da ficha: "" = automático; senão, um setor da cozinha. */
    private String setorDaFicha(String setor) {
        String s = Formato.texto(setor).trim();
        if (!s.isEmpty() && dadosService.setores().stream().noneMatch(x -> x.id().equals(s))) {
            throw new AppException("Setor de produção inválido.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return s;
    }

    @Transactional(readOnly = true)
    public List<PreparacaoListaItemDTO> listarFichas(String busca) {
        Dados d = dadosService.carregar();
        String q = Formato.busca(busca);
        return d.preparacoes.stream().filter(p -> q.isEmpty() || Formato.contem(p.nome(), q)).sorted(Formato.porNome(PreparacaoResponseDTO::nome))
            .map(p -> new PreparacaoListaItemDTO(p, Fichas.custoPrep(d, p), Fichas.fichaStatus(d, p),
                d.cardapio.stream().anyMatch(c -> p.id().equals(c.preparacaoId())),
                d.preparacoes.stream().filter(x -> x.itens().stream().anyMatch(it -> p.id().equals(it.subId()))).map(PreparacaoResponseDTO::nome).toList()))
            .toList();
    }

    @Transactional(readOnly = true)
    public CustoPreparacaoDTO calcularCusto(CalcularCustoRequestDTO req) {
        List<PreparacaoResponseDTO.Item> itens = req.itens() == null ? List.of() : req.itens().stream()
            .map(it -> new PreparacaoResponseDTO.Item(it.insumoId(), it.subId(), it.qtd() == null ? 0 : it.qtd(), it.fc() == null ? 1 : it.fc())).toList();
        return Fichas.custoPrep(dadosService.carregar(), itens, req.rendimento() == null ? 1 : req.rendimento());
    }

    @Transactional
    public PreparacaoResponseDTO criarFicha(PreparacaoRequestDTO req) {
        PreparacaoEntity ficha = new PreparacaoEntity();
        aplicar(req, ficha);
        return GestaoMapper.preparacao(preparacaoRepository.save(ficha));
    }

    @Transactional
    public PreparacaoResponseDTO atualizarFicha(Long id, PreparacaoRequestDTO req) {
        PreparacaoEntity ficha = cadastro.preparacao(id);
        aplicar(req, ficha);
        return GestaoMapper.preparacao(ficha);
    }

    /** Ficha em cardápio, usada como sub-receita, em prato da carta, com lote ou em plano de produção não pode ser excluída. */
    @Transactional
    public void apagarFicha(Long id) {
        PreparacaoEntity ficha = cadastro.preparacao(id);
        if (itemCardapioRepository.existsByPreparacaoId(id)) {
            throw new AppException("Esta ficha está no cardápio de algum evento e não pode ser excluída.", HttpStatus.CONFLICT);
        }
        if (preparacaoRepository.usadaComoSubReceita(id)) {
            throw new AppException("Esta ficha é sub-receita de outra e não pode ser excluída.", HttpStatus.CONFLICT);
        }
        if (pratoVendaRepository.usaPreparacao(id)) {
            throw new AppException("Esta ficha está em um prato do restaurante e não pode ser excluída.", HttpStatus.CONFLICT);
        }
        if (loteRepository.existsByPreparacaoId(id)) {
            throw new AppException("Esta ficha tem lotes de pré-preparo e não pode ser excluída.", HttpStatus.CONFLICT);
        }
        if (planoItemRepository.existsByPreparacaoId(id)) {
            throw new AppException("Esta ficha está em um plano de produção e não pode ser excluída.", HttpStatus.CONFLICT);
        }
        preparacaoRepository.delete(ficha);
    }

    private void aplicar(PreparacaoRequestDTO req, PreparacaoEntity ficha) {
        ficha.setNome(req.nome().trim());
        ficha.setCategoria(req.categoria());
        ficha.setPraca(Formato.vazio(req.praca()) ? Fichas.pracaPadrao(req.categoria()) : req.praca());
        ficha.setConservacao(req.conservacao() == null ? "refrigerado" : req.conservacao());
        ficha.setValidadeDias(req.validadeDias());
        ficha.setSetor(setorDaFicha(req.setor()));
        ficha.setDnProducao(req.dnProducao());
        ficha.setTempoMin(req.tempoMin());
        ficha.setEquipamentos(Formato.texto(req.equipamentos()).trim());
        ficha.setArmazenamento(Formato.texto(req.armazenamento()).trim());
        ficha.setCompradoPronto(Boolean.TRUE.equals(req.compradoPronto()));
        ficha.setRendimento(req.rendimento());
        ficha.setUnidadeRendimento(req.unidadeRendimento() == null ? "porções" : req.unidadeRendimento());
        ficha.setPorcaoPadrao(req.porcaoPadrao() == null ? 1.0 : req.porcaoPadrao());
        ficha.setObsEnvio(Formato.texto(req.obsEnvio()).trim());
        ficha.setModoPreparo(Formato.texto(req.modoPreparo()).trim());
        ficha.setAtualizadoEm(LocalDate.now());

        ficha.getItensIngredientes().clear();
        for (PreparacaoRequestDTO.Item it : lista(req.itens())) {
            PratoIngredienteEntity linha = new PratoIngredienteEntity();
            linha.setPreparacao(ficha);
            if (it.insumoId() != null) {
                linha.setIngrediente(cadastro.ingredienteDoCorpo(it.insumoId(), "Um dos itens da receita não existe mais no cadastro."));
            } else {
                linha.setSubPreparacao(subReceita(ficha, it.subId()));
            }
            linha.setQuantidade(it.qtd());
            linha.setFc(it.fc() == null ? 1.0 : it.fc());
            linha.setOrdem(ficha.getItensIngredientes().size());
            ficha.getItensIngredientes().add(linha);
        }

        ficha.getLoucas().clear();
        for (PreparacaoRequestDTO.Louca l : lista(req.loucas())) {
            PratoLoucaEntity louca = new PratoLoucaEntity();
            louca.setPreparacao(ficha);
            louca.setIngrediente(cadastro.ingredienteDoCorpo(l.insumoId(), "Uma das louças não existe mais no cadastro."));
            louca.setQtd(l.qtd());
            louca.setOrdem(ficha.getLoucas().size());
            ficha.getLoucas().add(louca);
        }

        ficha.setEtapas(new ArrayList<>(lista(req.etapas()).stream().map(t -> new EtapaPreparacao(t.desc().trim(), Formato.texto(t.praca()), t.dn())).toList()));
        ficha.setComponentes(new ArrayList<>(lista(req.componentes()).stream().map(k -> new ComponenteEnvio(k.n().trim(), k.u() == null ? "kg" : k.u(), k.q(),
            k.cons() == null ? "ambiente" : k.cons(), Formato.vazio(k.obs()) ? null : k.obs().trim())).toList()));
    }

    /** A sub-receita precisa existir e não pode conter (em nenhum nível) a própria ficha. */
    private PreparacaoEntity subReceita(PreparacaoEntity ficha, Long subId) {
        PreparacaoEntity sub = preparacaoRepository.findById(subId).orElse(null);
        if (sub == null || subId.equals(ficha.getId()) || (ficha.getId() != null && contem(sub, ficha.getId(), 0))) {
            throw new AppException("Sub-receita inválida.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return sub;
    }

    private boolean contem(PreparacaoEntity ficha, Long procurada, int nivel) {
        if (nivel > 6) {
            return false;
        }
        for (PratoIngredienteEntity it : ficha.getItensIngredientes()) {
            PreparacaoEntity sub = it.getSubPreparacao();
            if (sub != null && (sub.getId().equals(procurada) || contem(sub, procurada, nivel + 1))) {
                return true;
            }
        }
        return false;
    }

    private static <T> List<T> lista(List<T> l) {
        return l == null ? List.of() : l;
    }
}
