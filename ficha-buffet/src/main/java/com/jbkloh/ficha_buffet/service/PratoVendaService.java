package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Fichas;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.CalcularCmvRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PratoVendaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CmvDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratosVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.PratoVendaComponenteEntity;
import com.jbkloh.ficha_buffet.model.PratoVendaEntity;
import com.jbkloh.ficha_buffet.model.PratoVendaInsumoEntity;
import com.jbkloh.ficha_buffet.repositories.PratoVendaRepository;

import lombok.RequiredArgsConstructor;

/** Pratos da carta do restaurante, com custo e CMV calculados pelas fichas técnicas. */
@Service
@RequiredArgsConstructor
public class PratoVendaService {

    private final PratoVendaRepository pratoVendaRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    /** KPIs de CMV da carta e pratos por seção (ativos primeiro), filtrados pelo nome. */
    @Transactional(readOnly = true)
    public PratosVendaResponseDTO listarPratos(String busca) {
        Dados d = dadosService.carregar();
        String q = Formato.busca(busca);
        List<PratoVendaResponseDTO> ativos = d.pratosVenda.stream().filter(PratoVendaResponseDTO::ativo).toList();
        List<Double> cmvs = ativos.stream().map(x -> Fichas.cmv(d, x).cmv()).filter(Objects::nonNull).toList();
        PratosVendaResponseDTO.Kpis kpis = new PratosVendaResponseDTO.Kpis(ativos.size(), d.pratosVenda.size() - ativos.size(),
            cmvs.isEmpty() ? null : cmvs.stream().mapToDouble(Double::doubleValue).sum() / cmvs.size(),
            (int) cmvs.stream().filter(c -> c > Dominio.META_CMV).count(),
            (int) ativos.stream().filter(x -> (x.componentes().isEmpty() && x.extras().isEmpty()) || Fichas.cmv(d, x).faltando() > 0 || !(x.preco() > 0)).count());
        List<PratoVendaListaItemDTO> pratos = d.pratosVenda.stream().filter(x -> q.isEmpty() || Formato.contem(x.nome(), q))
            .sorted(Comparator.<PratoVendaResponseDTO>comparingInt(x -> Dominio.SECOES_CARTA.indexOf(x.secao())).thenComparing(x -> !x.ativo())
                .thenComparing(Formato.porNome(PratoVendaResponseDTO::nome)))
            .map(x -> new PratoVendaListaItemDTO(x, Fichas.cmv(d, x),
                x.componentes().stream().map(k -> d.preparacao(k.preparacaoId())).filter(Objects::nonNull).map(PreparacaoResponseDTO::nome).toList()))
            .toList();
        return new PratosVendaResponseDTO(kpis, pratos);
    }

    @Transactional(readOnly = true)
    public CmvDTO calcularCmv(CalcularCmvRequestDTO req) {
        return Fichas.cmv(dadosService.carregar(),
            req.componentes() == null ? List.of() : req.componentes().stream().map(c -> new PratoVendaResponseDTO.Componente(c.preparacaoId(), c.qtd())).toList(),
            req.extras() == null ? List.of() : req.extras().stream().map(x -> new PratoVendaResponseDTO.Extra(x.insumoId(), x.qtd())).toList(),
            req.preco() == null ? 0 : req.preco());
    }

    @Transactional
    public PratoVendaResponseDTO criarPrato(PratoVendaRequestDTO req) {
        PratoVendaEntity prato = new PratoVendaEntity();
        aplicar(req, prato);
        return GestaoMapper.pratoVenda(pratoVendaRepository.save(prato));
    }

    @Transactional
    public PratoVendaResponseDTO atualizarPrato(Long id, PratoVendaRequestDTO req) {
        PratoVendaEntity prato = buscar(id);
        aplicar(req, prato);
        return GestaoMapper.pratoVenda(prato);
    }

    @Transactional
    public void apagarPrato(Long id) {
        pratoVendaRepository.delete(buscar(id));
    }

    private void aplicar(PratoVendaRequestDTO req, PratoVendaEntity prato) {
        String codigo = Formato.texto(req.codigoPdv()).trim();
        if (!codigo.isEmpty()) {
            pratoVendaRepository.findByCodigoPdv(codigo).filter(outro -> !outro.getId().equals(prato.getId())).ifPresent(outro -> {
                throw new AppException("Outro prato já usa este código do PDV.", HttpStatus.CONFLICT);
            });
        }
        prato.setNome(req.nome().trim());
        prato.setSecao(req.secao() == null ? "campo" : req.secao());
        prato.setPreco(req.preco() == null ? BigDecimal.ZERO : req.preco());
        prato.setCodigoPdv(codigo);
        prato.setAtivo(!Boolean.FALSE.equals(req.ativo()));
        prato.setObs(Formato.texto(req.obs()).trim());
        prato.setAtualizadoEm(LocalDate.now());

        prato.getComponentes().clear();
        if (req.componentes() != null) {
            for (PratoVendaRequestDTO.Componente c : req.componentes()) {
                PratoVendaComponenteEntity componente = new PratoVendaComponenteEntity();
                componente.setPratoVenda(prato);
                componente.setPreparacao(cadastro.preparacaoDoCorpo(c.preparacaoId()));
                componente.setQtd(c.qtd());
                componente.setOrdem(prato.getComponentes().size());
                prato.getComponentes().add(componente);
            }
        }
        prato.getInsumos().clear();
        if (req.extras() != null) {
            for (PratoVendaRequestDTO.Extra x : req.extras()) {
                PratoVendaInsumoEntity insumo = new PratoVendaInsumoEntity();
                insumo.setPratoVenda(prato);
                insumo.setIngrediente(cadastro.ingredienteDoCorpo(x.insumoId(), "Um dos insumos do prato não existe mais no cadastro."));
                insumo.setQtd(x.qtd());
                insumo.setOrdem(prato.getInsumos().size());
                prato.getInsumos().add(insumo);
            }
        }
    }

    private PratoVendaEntity buscar(Long id) {
        return pratoVendaRepository.findById(id).orElseThrow(() -> new AppException("O prato de id " + id + " não existe.", HttpStatus.NOT_FOUND));
    }
}
