package com.jbkloh.ficha_buffet.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.AjustarEquipeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EquipeResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.EquipeEntity;
import com.jbkloh.ficha_buffet.model.EquipeEventoEntity;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.repositories.EquipeEventoRepository;
import com.jbkloh.ficha_buffet.repositories.EquipeRepository;

import lombok.RequiredArgsConstructor;

/** Cadastro da equipe do buffet: as pessoas que podem ser escaladas nos eventos. */
@Service
@RequiredArgsConstructor
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final EquipeEventoRepository escalaRepository;

    /** Todas as pessoas do cadastro, em ordem alfabética. */
    @Transactional(readOnly = true)
    public List<EquipeResponseDTO> listar() {
        return equipeRepository.findAll().stream().map(EquipeService::resposta).sorted(Formato.porNome(EquipeResponseDTO::nome)).toList();
    }

    /**
     * Edita o cadastro: só o que vier muda. Nome, telefone e Pix valem em todas as escalas; função e
     * diária padrão só nas próximas. Trocar o nome leva junto as contas a pagar de diária ainda não pagas.
     */
    @Transactional
    public EquipeResponseDTO ajustar(Long id, AjustarEquipeRequestDTO req) {
        EquipeEntity pessoa = equipeRepository.findById(id).orElseThrow(() -> new AppException("Pessoa da equipe não encontrada.", HttpStatus.NOT_FOUND));
        if (req.nome() != null && !req.nome().trim().equals(pessoa.getNome())) {
            if (porNome(req.nome(), id).isPresent()) {
                throw new AppException("Já existe na equipe outra pessoa com o nome " + req.nome().trim() + ".", HttpStatus.CONFLICT);
            }
            pessoa.setNome(req.nome().trim());
            String usuario = Usuario.atual();
            for (EquipeEventoEntity x : escalaRepository.findByPessoaId(id)) {
                LancamentoEntity l = x.getLancamento();
                if (l != null && !EquipeEventoService.pago(l)) {
                    l.setDescricao(EquipeEventoService.descricao(x));
                    l.setFornecedor(pessoa.getNome());
                    l.setAlteradoPor(usuario);
                    l.setAlteradoEm(Instant.now());
                }
            }
        }
        if (req.funcaoPadrao() != null) {
            pessoa.setFuncaoPadrao(req.funcaoPadrao());
        }
        if (req.diariaPadrao() != null) {
            pessoa.setDiariaPadrao(req.diariaPadrao());
        }
        if (req.contato() != null) {
            pessoa.setContato(req.contato().trim());
        }
        if (req.pix() != null) {
            pessoa.setPix(req.pix().trim());
        }
        return resposta(pessoa);
    }

    /** Pessoa do cadastro com o mesmo nome (sem diferenciar acentos, maiúsculas e espaços), fora a de id `exceto`. */
    Optional<EquipeEntity> porNome(String nome, Long exceto) {
        String n = Formato.normTxt(nome);
        return equipeRepository.findAll().stream()
            .filter(p -> !p.getId().equals(exceto) && Formato.normTxt(p.getNome()).equals(n))
            .min(Comparator.comparing(EquipeEntity::getId));
    }

    static EquipeResponseDTO resposta(EquipeEntity p) {
        return new EquipeResponseDTO(p.getId(), p.getNome(), p.getFuncaoPadrao(), p.getDiariaPadrao().doubleValue(), p.getContato(), p.getPix());
    }
}
