package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.calculo.NotasFiscais;
import com.jbkloh.ficha_buffet.dtos.req.NfDeParaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.NotaFiscalRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.DeParaSugestaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalImportadaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.MovimentoEstoqueEntity;
import com.jbkloh.ficha_buffet.model.NotaFiscalEntity;
import com.jbkloh.ficha_buffet.model.NotaFiscalItemEntity;
import com.jbkloh.ficha_buffet.model.json.ParcelaNf;
import com.jbkloh.ficha_buffet.repositories.IngredientesRepository;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;
import com.jbkloh.ficha_buffet.repositories.MovimentoEstoqueRepository;
import com.jbkloh.ficha_buffet.repositories.NotaFiscalRepository;

import lombok.RequiredArgsConstructor;

/** Notas fiscais de compra: rascunho, entrada no estoque e no caixa, estorno e leitura do XML da NF-e. */
@Service
@RequiredArgsConstructor
public class NotaFiscalService {

    private final NotaFiscalRepository notaFiscalRepository;
    private final IngredientesRepository ingredientesRepository;
    private final MovimentoEstoqueRepository movimentoRepository;
    private final LancamentoRepository lancamentoRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    private static NotaFiscalListaItemDTO listaItem(Dados d, NotaFiscalResponseDTO n) {
        EventoResponseDTO e = d.evento(n.eventoId());
        List<LancamentoCaixaResponseDTO> ls = !"lancada".equals(n.status()) ? List.of() : d.lancamentos.stream().filter(l -> n.id().equals(l.nfId()))
            .sorted(Comparator.comparingInt(l -> l.parcelaNF() == null ? 0 : l.parcelaNF())).toList();
        List<LancamentoCaixaResponseDTO> abertas = ls.stream().filter(l -> "previsto".equals(l.status()))
            .sorted(Comparator.comparing(l -> l.vencimento() != null ? l.vencimento() : l.dataCompetencia())).toList();
        String proximo = abertas.isEmpty() ? null : abertas.get(0).vencimento() != null ? abertas.get(0).vencimento() : abertas.get(0).dataCompetencia();
        return new NotaFiscalListaItemDTO(n, e == null ? null : e.codigo(), e == null ? null : e.nome(), proximo, !ls.isEmpty() && abertas.isEmpty(),
            ls.stream().map(l -> new NotaFiscalListaItemDTO.ParcelaLancada(l.id(), l.parcelaNF(), l.valor(), l.status(), l.vencimento(), l.dataCompetencia(),
                l.dataPagamento(), l.formaPag())).toList());
    }

    /** Notas da mais recente para a mais antiga (emissão e, no mesmo dia, registro). */
    @Transactional(readOnly = true)
    public List<NotaFiscalListaItemDTO> listarNotas() {
        Dados d = dadosService.carregar();
        return d.nfs.stream().sorted(Comparator.comparing(NotaFiscalResponseDTO::emissao).thenComparing(NotaFiscalResponseDTO::criadoEm).reversed())
            .map(n -> listaItem(d, n)).toList();
    }

    @Transactional(readOnly = true)
    public NotaFiscalListaItemDTO detalharNota(Long id) {
        Dados d = dadosService.carregar();
        return d.nfs.stream().filter(n -> n.id().equals(id)).findFirst().map(n -> listaItem(d, n))
            .orElseThrow(() -> new AppException("Nota fiscal não encontrada.", HttpStatus.NOT_FOUND));
    }

    /** Salva o rascunho (novo com id nulo). Rascunho não mexe no estoque nem no caixa. */
    @Transactional
    public NotaFiscalResponseDTO salvarRascunho(Long id, NotaFiscalRequestDTO req) {
        NotaFiscalEntity nf = id == null ? new NotaFiscalEntity() : buscar(id);
        if ("lancada".equals(nf.getStatus())) {
            throw new AppException("Nota lançada: estorne a entrada para alterar.", HttpStatus.CONFLICT);
        }
        aplicar(req, nf);
        return GestaoMapper.notaFiscal(notaFiscalRepository.save(nf), List.of(), List.of());
    }

    /** Dar entrada: confere tudo, cria os movimentos, atualiza preço e fornecedor dos itens e lança as parcelas em contas a pagar. */
    @Transactional
    public NotaFiscalResponseDTO lancarNota(Long id, NotaFiscalRequestDTO req) {
        NotaFiscalEntity rascunho = id == null ? new NotaFiscalEntity() : buscar(id);
        if ("lancada".equals(rascunho.getStatus())) {
            throw new AppException("Esta nota já foi lançada.", HttpStatus.CONFLICT);
        }
        aplicar(req, rascunho);
        double total = total(rascunho);
        boolean noCaixa = Boolean.TRUE.equals(rascunho.getLancarCaixa()) && total > 0;
        validarEntrada(rascunho, total, noCaixa, Boolean.TRUE.equals(req.aceitarDiferenca()));
        NotaFiscalEntity nf = notaFiscalRepository.save(rascunho);

        String rotulo = "NF " + (nf.getNumero().isEmpty() ? "s/n" : nf.getNumero()) + " · " + nf.getFornecedor();
        LocalDate hoje = LocalDate.now();
        List<MovimentoEstoqueEntity> movimentos = new ArrayList<>();
        for (NotaFiscalItemEntity it : nf.getItens()) {
            IngredientesEntity item = it.getIngrediente();
            BigDecimal custo = it.getValor().signum() > 0 ? it.getValor().divide(BigDecimal.valueOf(it.getQtd()), 4, RoundingMode.HALF_UP) : null;
            MovimentoEstoqueEntity m = MovimentoEstoqueService.novo(item, "entrada", it.getQtd(), it.getQtd(), nf.getEmissao(), rotulo, "nf");
            m.setCustoUnit(custo);
            m.setNotaFiscal(nf);
            m.setEvento(nf.getEvento());
            movimentos.add(m);
            item.setAtualizadoEm(hoje);
            if (custo != null) {
                item.setCusto(custo);
            }
            if (Formato.vazio(item.getFornecedor())) {
                item.setFornecedor(nf.getFornecedor());
            }
        }
        movimentoRepository.saveAll(movimentos);

        List<LancamentoEntity> lancamentos = new ArrayList<>();
        if (noCaixa) {
            // A despesa compete à data do evento a que a compra se destina; sem evento, à emissão.
            LocalDate competencia = nf.getEvento() != null && nf.getEvento().getDataEvento() != null ? nf.getEvento().getDataEvento() : nf.getEmissao();
            int n = nf.getParcelas().size();
            for (int k = 0; k < n; k++) {
                ParcelaNf p = nf.getParcelas().get(k);
                boolean pago = Boolean.TRUE.equals(p.pago());
                LancamentoEntity l = CaixaService.novoLancamento(nf.getCategoria(), nf.getEvento(),
                    rotulo + (n > 1 ? " · parcela " + (k + 1) + "/" + n : "") + ("boleto".equals(nf.getFormaPag()) ? " (boleto)" : ""), p.valor(), competencia, "nf");
                l.setVencimento(p.venc());
                l.setNotaFiscal(nf);
                l.setParcelaNF(k + 1);
                l.setFormaPag(nf.getFormaPag());
                l.setFornecedor(nf.getFornecedor());
                l.setUnidade(nf.getUnidade());
                if (pago) {
                    CaixaService.novoPagamento(l, p.venc().isAfter(hoje) ? hoje : p.venc(), p.valor(), "banco", nf.getFormaPag());
                }
                CaixaService.sincronizar(l);
                lancamentos.add(l);
            }
            lancamentoRepository.saveAll(lancamentos);
        }
        nf.setStatus("lancada");
        nf.setLancadaEm(hoje);
        return GestaoMapper.notaFiscal(nf, movimentos.stream().map(MovimentoEstoqueEntity::getId).toList(),
            lancamentos.stream().map(LancamentoEntity::getId).toList());
    }

    private void validarEntrada(NotaFiscalEntity nf, double total, boolean noCaixa, boolean aceitarDiferenca) {
        if (nf.getItens().isEmpty()) {
            throw regra("Inclua ao menos uma linha.");
        }
        if (!nf.getChave().isEmpty()) {
            notaFiscalRepository.findByChave(nf.getChave()).filter(outra -> !outra.getId().equals(nf.getId())).ifPresent(outra -> {
                throw new AppException("Esta nota já está no sistema (mesma chave de acesso).", HttpStatus.CONFLICT);
            });
        }
        if (nf.getItens().stream().anyMatch(it -> it.getIngrediente() == null)) {
            throw regra("Ligue todas as linhas a um item do cadastro.");
        }
        if (nf.getItens().stream().anyMatch(it -> !(it.getQtd() > 0))) {
            throw regra("Informe a quantidade de todas as linhas.");
        }
        if (nf.getItens().stream().anyMatch(it -> !Boolean.TRUE.equals(it.getConferido()))) {
            throw regra("Confira fisicamente todas as linhas antes de dar entrada.");
        }
        if (noCaixa) {
            if (nf.getCategoria() == null) {
                throw regra("Escolha a categoria da despesa.");
            }
            if (nf.getParcelas().stream().anyMatch(p -> p.valor().signum() <= 0)) {
                throw regra("Informe o valor de todas as parcelas.");
            }
            if (Formato.vazio(nf.getUnidade())) {
                throw regra("Escolha a unidade de negócio.");
            }
            double soma = Formato.round2(Formato.soma(nf.getParcelas(), p -> p.valor().doubleValue()));
            double dif = Formato.round2(soma - total);
            if (Math.abs(dif) >= 0.01) {
                if (!aceitarDiferenca) {
                    throw regra("A soma das parcelas (" + Formato.brl(soma) + ") é " + (dif < 0 ? "menor" : "maior") + " que o total dos produtos ("
                        + Formato.brl(total) + "), diferença de " + Formato.brl(Math.abs(dif)) + ". Confirme para dar entrada assim mesmo.");
                }
                // O estoque entra com os valores dos produtos e o caixa com o das parcelas: a diferença fica como acréscimo ou desconto.
                nf.setAcrescimo(nf.getAcrescimo().add(BigDecimal.valueOf(dif)).setScale(2, RoundingMode.HALF_UP));
            }
        }
    }

    /** Desfaz a entrada: remove movimentos e parcelas da nota, que volta a rascunho. O preço dos itens fica. */
    @Transactional
    public void estornarNota(Long id) {
        NotaFiscalEntity nf = buscar(id);
        movimentoRepository.deleteAll(movimentoRepository.findByNotaFiscalId(id));
        lancamentoRepository.deleteAll(lancamentoRepository.findByNotaFiscalIdOrderByParcelaNFAsc(id));
        nf.setStatus("rascunho");
        nf.setLancadaEm(null);
    }

    @Transactional
    public void apagarNota(Long id) {
        NotaFiscalEntity nf = buscar(id);
        if ("lancada".equals(nf.getStatus())) {
            throw new AppException("Nota lançada: estorne a entrada antes de excluir.", HttpStatus.CONFLICT);
        }
        notaFiscalRepository.delete(nf);
    }

    /** Lê o XML da NF-e, avisa se a nota já existe e liga as linhas pelo histórico do fornecedor. Não grava nada. */
    @Transactional(readOnly = true)
    public NotaFiscalImportadaDTO importarXml(String xml) {
        NotaFiscalImportadaDTO x = NotasFiscais.lerNFeXML(xml);
        Dados d = dadosService.carregar();
        Long duplicada = x.chave().isEmpty() ? null : d.nfs.stream().filter(n -> x.chave().equals(n.chave())).map(NotaFiscalResponseDTO::id).findFirst().orElse(null);
        List<NotaFiscalResponseDTO.Item> itens = x.itens().stream().map(it -> {
            DeParaSugestaoDTO s = NotasFiscais.deParaSugestao(d, x.fornecedor(), it.desc(), it.cod());
            return s == null ? it : new NotaFiscalResponseDTO.Item(it.desc(), it.cod(), it.uCom(), s.insumoId(), it.qtdNota(), s.unid(), it.qtd(),
                it.valorUnit(), it.valor(), it.conferido());
        }).toList();
        return new NotaFiscalImportadaDTO(x.chave(), x.numero(), x.serie(), x.emissao(), x.fornecedor(), x.razaoSocial(), x.cnpjFornecedor(), itens, x.total(),
            x.acrescimo(), x.formaPag(), x.parcelas(), true, false, x.duvidas(), duplicada);
    }

    /** Para cada linha, o item do cadastro sugerido pelo histórico do fornecedor (ou nulo), na mesma ordem. */
    @Transactional(readOnly = true)
    public List<DeParaSugestaoDTO> sugerirItens(NfDeParaRequestDTO req) {
        Dados d = dadosService.carregar();
        List<DeParaSugestaoDTO> out = new ArrayList<>();
        req.itens().forEach(it -> out.add(NotasFiscais.deParaSugestao(d, req.fornecedor(), it.desc(), it.cod())));
        return out;
    }

    /** Passa o formulário para a nota: linhas com a quantidade convertida para a unidade de estoque e parcelas completas. */
    private void aplicar(NotaFiscalRequestDTO req, NotaFiscalEntity nf) {
        LocalDate emissao = Formato.data(req.emissao(), "emissão");
        nf.setFornecedor(req.fornecedor().trim());
        nf.setNumero(Formato.texto(req.numero()).trim());
        nf.setSerie(Formato.texto(req.serie()).trim());
        nf.setEmissao(emissao);
        nf.setEvento(cadastro.eventoDoCorpo(req.eventoId()));
        nf.setLancarCaixa(!Boolean.FALSE.equals(req.lancarCaixa()));
        nf.setCategoria(req.categoriaId() == null ? null : cadastro.categoriaDoCorpo(req.categoriaId(), "Escolha a categoria da despesa."));
        nf.setFormaPag(req.formaPag() == null ? "boleto" : req.formaPag());
        nf.setUnidade(Formato.vazio(req.unidade()) ? null : req.unidade());
        nf.setAcrescimo(req.acrescimo() == null ? BigDecimal.ZERO : req.acrescimo());
        nf.setChave(Formato.texto(req.chave()));
        nf.setRazaoSocial(Formato.texto(req.razaoSocial()).trim());
        nf.setCnpjFornecedor(Formato.texto(req.cnpjFornecedor()).trim());
        nf.setImportadaXml(Boolean.TRUE.equals(req.importadaXML()));
        nf.setImportadaFoto(Boolean.TRUE.equals(req.importadaFoto()));
        nf.setStatus("rascunho");

        nf.getItens().clear();
        for (NotaFiscalRequestDTO.Item it : req.itens() == null ? List.<NotaFiscalRequestDTO.Item>of() : req.itens()) {
            double qtdNota = it.qtdNota() == null ? 0 : it.qtdNota();
            String desc = Formato.texto(it.desc()).trim();
            if (desc.isEmpty() && it.insumoId() == null && qtdNota == 0) {
                continue; // linha em branco do formulário
            }
            IngredientesEntity item = it.insumoId() == null ? null : ingredientesRepository.findById(it.insumoId()).orElse(null);
            String unid = it.unid() == null ? "estoque" : it.unid();
            NotaFiscalItemEntity linha = new NotaFiscalItemEntity();
            linha.setNotaFiscal(nf);
            linha.setOrdem(nf.getItens().size());
            linha.setDescricao(desc);
            linha.setCodigo(Formato.texto(it.cod()).trim());
            linha.setUnidadeComercial(Formato.texto(it.uCom()).trim());
            linha.setIngrediente(item);
            linha.setQtdNota(qtdNota);
            linha.setUnid(unid);
            linha.setQtd(item == null ? 0 : NotasFiscais.qtdConvertida(qtdNota, unid, item.getFatorCompra()));
            linha.setValorUnit(it.valorUnit() == null ? null : BigDecimal.valueOf(it.valorUnit()).setScale(4, RoundingMode.HALF_UP));
            linha.setValor(dinheiro(it.valor()));
            linha.setConferido(Boolean.TRUE.equals(it.conferido()));
            nf.getItens().add(linha);
        }

        // Sem parcelas informadas, a nota tem uma só, no valor total, vencendo na emissão.
        BigDecimal total = BigDecimal.valueOf(total(nf)).setScale(2, RoundingMode.HALF_UP);
        List<ParcelaNf> parcelas = new ArrayList<>();
        for (NotaFiscalRequestDTO.Parcela p : req.parcelas() == null ? List.<NotaFiscalRequestDTO.Parcela>of() : req.parcelas()) {
            parcelas.add(new ParcelaNf(Formato.vazio(p.venc()) ? emissao : Formato.data(p.venc(), "vencimento da parcela"), dinheiro(p.valor()),
                Boolean.TRUE.equals(p.pago())));
        }
        if (parcelas.isEmpty()) {
            parcelas.add(new ParcelaNf(emissao, total, false));
        } else if (parcelas.size() == 1 && parcelas.get(0).valor().signum() == 0) {
            parcelas.set(0, new ParcelaNf(parcelas.get(0).venc(), total, parcelas.get(0).pago()));
        }
        nf.setParcelas(parcelas);
    }

    private static double total(NotaFiscalEntity nf) {
        return Formato.round2(Formato.round2(Formato.soma(nf.getItens(), it -> it.getValor().doubleValue())) + nf.getAcrescimo().doubleValue());
    }

    private static BigDecimal dinheiro(Double valor) {
        return BigDecimal.valueOf(valor == null ? 0 : valor).setScale(2, RoundingMode.HALF_UP);
    }

    private static AppException regra(String mensagem) {
        return new AppException(mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private NotaFiscalEntity buscar(Long id) {
        return notaFiscalRepository.findById(id).orElseThrow(() -> new AppException("A nota de id " + id + " não existe.", HttpStatus.NOT_FOUND));
    }
}
