package com.jbkloh.ficha_buffet.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import com.jbkloh.ficha_buffet.calculo.Estoque;
import com.jbkloh.ficha_buffet.calculo.Fichas;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.LotePreparoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MovimentarLoteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoquePreparosResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.LoteMovimentoEntity;
import com.jbkloh.ficha_buffet.model.LotePreparoEntity;
import com.jbkloh.ficha_buffet.model.PreparacaoEntity;
import com.jbkloh.ficha_buffet.repositories.LotePreparoRepository;
import com.jbkloh.ficha_buffet.repositories.PlanoProducaoItemRepository;

import lombok.RequiredArgsConstructor;

/** Lotes de pré-preparo: produção com validade, uso e descarte. */
@Service
@RequiredArgsConstructor
public class LotePreparoService {

    private static final Comparator<LoteListaItemDTO> VENCE_ANTES = Comparator.comparing(l -> Formato.vazio(l.lote().validade()) ? "9" : l.lote().validade());

    private final LotePreparoRepository loteRepository;
    private final PlanoProducaoItemRepository planoItemRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    private static LoteListaItemDTO listaItem(Dados d, LoteResponseDTO l, String hoje) {
        PreparacaoResponseDTO p = d.preparacao(l.preparacaoId());
        return new LoteListaItemDTO(l, p == null ? "Ficha removida" : p.nome(), d.nomeEvento(l.eventoId()), Estoque.loteSituacao(l, hoje),
            p == null ? 0 : Fichas.custoPrep(d, p).porUnid() * Math.max(0, l.saldo()),
            Dominio.rotulo(Dominio.CONSERVACOES, p == null ? "refrigerado" : p.conservacao(), "—"));
    }

    /**
     * Pré-preparos: KPIs dos lotes com saldo, o que está pronto agora por
     * preparação (usar primeiro o que vence antes) e até 300 lotes do filtro
     * (ativos, atencao ou todos).
     */
    @Transactional(readOnly = true)
    public EstoquePreparosResponseDTO listarLotes(String filtro) {
        Dados d = dadosService.carregar();
        String hoje = Formato.hoje();
        List<LoteListaItemDTO> todos = d.lotes.stream().map(l -> listaItem(d, l, hoje)).toList();
        List<LoteListaItemDTO> ativos = todos.stream().filter(l -> Estoque.loteAtivo(l.lote())).toList();
        List<LoteListaItemDTO> validos = ativos.stream().filter(l -> !"Vencido".equals(l.situacao().get(1))).toList();

        Map<Long, List<LoteListaItemDTO>> porPreparacao = new LinkedHashMap<>();
        validos.forEach(l -> porPreparacao.computeIfAbsent(l.lote().preparacaoId(), k -> new ArrayList<>()).add(l));
        List<EstoquePreparosResponseDTO.Pronto> pronto = porPreparacao.entrySet().stream().map(x -> {
            List<LoteListaItemDTO> ordenados = x.getValue().stream().sorted(VENCE_ANTES).toList();
            LoteListaItemDTO primeiro = ordenados.get(0);
            return new EstoquePreparosResponseDTO.Pronto(x.getKey(), primeiro.preparacaoNome(), Formato.r3(Formato.soma(ordenados, l -> l.lote().saldo())),
                primeiro.lote().unidade(), ordenados.size(), primeiro);
        }).sorted(Formato.porNome(EstoquePreparosResponseDTO.Pronto::nome)).toList();

        List<LoteListaItemDTO> lista = todos.stream().filter(l -> switch (Formato.texto(filtro)) {
            case "todos" -> true;
            case "atencao" -> Estoque.loteAtivo(l.lote()) && ("bad".equals(l.situacao().get(0)) || "warn".equals(l.situacao().get(0)));
            default -> Estoque.loteAtivo(l.lote());
        }).sorted(Comparator.<LoteListaItemDTO, Boolean>comparing(l -> !Estoque.loteAtivo(l.lote())).thenComparing(VENCE_ANTES)
            .thenComparing(Comparator.<LoteListaItemDTO, String>comparing(l -> l.lote().produzidoEm()).reversed())).limit(300).toList();

        return new EstoquePreparosResponseDTO(new EstoquePreparosResponseDTO.Kpis(ativos.size(),
            (int) ativos.stream().map(l -> l.lote().preparacaoId()).distinct().count(),
            (int) ativos.stream().filter(l -> "Vence hoje".equals(l.situacao().get(1)) || "Vence amanhã".equals(l.situacao().get(1))).count(),
            ativos.size() - validos.size(), Formato.soma(validos, LoteListaItemDTO::valor)), pronto, lista);
    }

    @Transactional(readOnly = true)
    public LoteListaItemDTO detalharLote(Long id) {
        Dados d = dadosService.carregar();
        return d.lotes.stream().filter(l -> l.id().equals(id)).findFirst().map(l -> listaItem(d, l, Formato.hoje()))
            .orElseThrow(() -> new AppException("Lote não encontrado.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public LoteResponseDTO criarLote(LotePreparoRequestDTO req) {
        return GestaoMapper.lote(registrarLote(req, null));
    }

    /** Grava o lote novo; unidadeNegocio só vem do item de plano de produção (restaurante ou chocolates). */
    @Transactional
    public LotePreparoEntity registrarLote(LotePreparoRequestDTO req, String unidadeNegocio) {
        LotePreparoEntity lote = new LotePreparoEntity();
        aplicarProducao(req, lote);
        lote.setUnidadeNegocio(unidadeNegocio);
        return loteRepository.save(lote);
    }

    /** Lote sem uso pode ser refeito por inteiro; com uso (ou sem preparacaoId no corpo), só validade, responsável e observações. */
    @Transactional
    public LoteResponseDTO atualizarLote(Long id, LotePreparoRequestDTO req) {
        LotePreparoEntity lote = buscar(id);
        if (req.preparacaoId() == null) {
            LocalDate validade = Formato.dataOpcional(req.validade(), "validade");
            validarValidade(validade, lote.getProduzidoEm());
            lote.setValidade(validade);
            lote.setResponsavel(Formato.texto(req.responsavel()).trim());
            lote.setObs(Formato.texto(req.obs()).trim());
            return GestaoMapper.lote(lote);
        }
        if (!lote.getHistorico().isEmpty()) {
            throw new AppException("Este lote já teve uso ou descarte; só dá para mudar validade, responsável e observações.", HttpStatus.CONFLICT);
        }
        if (req.qtd() == null || Formato.vazio(req.produzidoEm())) {
            throw new AppException("Informe a quantidade e a data de produção.", HttpStatus.BAD_REQUEST);
        }
        aplicarProducao(req, lote);
        return GestaoMapper.lote(lote);
    }

    /** Uso ou descarte de parte do lote: baixa o saldo e entra no histórico. */
    @Transactional
    public LoteResponseDTO movimentarLote(Long id, MovimentarLoteRequestDTO req) {
        LotePreparoEntity lote = buscar(id);
        double saldo = lote.getSaldo();
        if (req.qtd() > saldo + 1e-9) {
            throw new AppException("Maior que o saldo do lote (" + Formato.fmtN(Formato.r3(saldo)) + ").", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        boolean uso = "uso".equals(req.tipo());
        if (uso != List.of("restaurante", "evento", "outra").contains(req.para())) {
            throw new AppException(uso ? "Informe para onde o lote foi usado." : "Informe o motivo do descarte.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        double qtd = Formato.r3(req.qtd());
        LoteMovimentoEntity movimento = new LoteMovimentoEntity();
        movimento.setLote(lote);
        movimento.setTipo(req.tipo());
        movimento.setData(Formato.data(req.data(), "data"));
        movimento.setQtd(qtd);
        movimento.setPara(req.para());
        // Só o uso em evento guarda o evento.
        movimento.setEvento(uso && "evento".equals(req.para()) ? cadastro.eventoDoCorpo(req.eventoId()) : null);
        movimento.setObs(Formato.texto(req.obs()).trim());
        lote.getHistorico().add(movimento);
        lote.setSaldo(Formato.r3(saldo - qtd));
        return GestaoMapper.lote(lote);
    }

    @Transactional
    public void apagarLote(Long id) {
        LotePreparoEntity lote = buscar(id);
        if (!lote.getHistorico().isEmpty()) {
            throw new AppException("Este lote já teve uso ou descarte e não pode ser excluído.", HttpStatus.CONFLICT);
        }
        // O lote de plano sai pelo "Desfazer" do item, que volta o item para a produção.
        planoItemRepository.findByLoteId(id).ifPresent(item -> {
            throw new AppException("Este lote veio do plano de produção \"" + item.getPlano().getTitulo() + "\". Desfaça o item no plano.",
                HttpStatus.CONFLICT);
        });
        loteRepository.delete(lote);
    }

    private void aplicarProducao(LotePreparoRequestDTO req, LotePreparoEntity lote) {
        PreparacaoEntity ficha = cadastro.preparacaoDoCorpo(req.preparacaoId());
        LocalDate produzidoEm = Formato.data(req.produzidoEm(), "data de produção");
        LocalDate validade = Formato.dataOpcional(req.validade(), "validade");
        validarValidade(validade, produzidoEm);
        String destino = req.destino() == null ? "restaurante" : req.destino();
        if ("evento".equals(destino) && req.eventoId() == null) {
            throw new AppException("Escolha o evento.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!produzidoEm.equals(lote.getProduzidoEm())) {
            lote.setCodigo(proximoCodigo(produzidoEm));
        }
        lote.setPreparacao(ficha);
        lote.setQtd(Formato.r3(req.qtd()));
        lote.setSaldo(Formato.r3(req.qtd()));
        lote.setUnidade(ficha.getUnidadeRendimento());
        lote.setProduzidoEm(produzidoEm);
        lote.setValidade(validade);
        lote.setDestino(destino);
        lote.setEvento("evento".equals(destino) ? cadastro.eventoDoCorpo(req.eventoId()) : null);
        lote.setResponsavel(Formato.texto(req.responsavel()).trim());
        lote.setObs(Formato.texto(req.obs()).trim());
    }

    private static void validarValidade(LocalDate validade, LocalDate produzidoEm) {
        if (validade != null && validade.isBefore(produzidoEm)) {
            throw new AppException("A validade não pode ser antes da produção.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /** "LAAMMDD-NN": a sequência continua do maior número já usado no dia (lotes excluídos não liberam o número). */
    private String proximoCodigo(LocalDate produzidoEm) {
        String prefixo = "L" + produzidoEm.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        int maior = loteRepository.findByProduzidoEm(produzidoEm).stream().map(LotePreparoEntity::getCodigo)
            .filter(c -> c != null && c.startsWith(prefixo)).mapToInt(c -> {
                try {
                    return Integer.parseInt(c.substring(prefixo.length()));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }).max().orElse(0);
        return prefixo + String.format("%02d", maior + 1);
    }

    private LotePreparoEntity buscar(Long id) {
        return loteRepository.findById(id).orElseThrow(() -> new AppException("O lote de id " + id + " não existe.", HttpStatus.NOT_FOUND));
    }
}
