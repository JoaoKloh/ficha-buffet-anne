package com.jbkloh.ficha_buffet.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.AcaoOrdemRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AutorizarLiberacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AvariaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CriarOcorrenciaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LiberarMapaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MestreMapaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PlanoOrdemRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ResolverOcorrenciaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarLiberacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarSetoresRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarExecucaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OcorrenciaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SalvarExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.service.CozinhaService;
import com.jbkloh.ficha_buffet.service.OrdemProducaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Tela da cozinha: projeção dos eventos (sem valores financeiros), o que a cozinha registra e as ordens de produção. */
@RestController
@RequestMapping("/cozinha")
@RequiredArgsConstructor
public class CozinhaController {

    private final CozinhaService cozinhaService;
    private final OrdemProducaoService ordemService;

    @GetMapping("/retornarTudo")
    public ResponseEntity<CozinhaResponseDTO> retornarTudo() {
        return ResponseEntity.ok(cozinhaService.retornarTudo());
    }

    /** Grava os campos tocados de um registro (tarefa, compra, linha do checklist ou transporte). */
    @PostMapping("/execucao/salvar")
    public ResponseEntity<SalvarExecucaoResponseDTO> salvarExecucao(@RequestBody @Valid SalvarExecucaoRequestDTO req) {
        return ResponseEntity.ok(cozinhaService.salvarExecucao(req));
    }

    /** Lança no estoque a perda das louças que faltaram no retorno. Responde { lancado }. */
    @PostMapping("/avaria/lancar")
    public ResponseEntity<Map<String, Double>> lancarAvaria(@RequestBody @Valid AvariaRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("lancado", cozinhaService.lancarAvaria(req)));
    }

    /* ------------------------------ Ordens de produção ------------------------------ */

    /** Ação da equipe numa ordem; quem registra vai para o histórico. */
    @PostMapping("/ordens/{acao:iniciar|concluir|conferir|bloquear|desbloquear|reabrir|responsavel}")
    public ResponseEntity<Void> agirOrdem(@PathVariable String acao, @RequestBody @Valid AcaoOrdemRequestDTO req) {
        ordemService.agir(acao, req);
        return ResponseEntity.noContent().build();
    }

    /** Gera o mapa de produção do evento (só a gestão vê até liberar). */
    @PostMapping("/mapa/gerar/{eventoId}")
    public ResponseEntity<Void> gerarMapa(@PathVariable Long eventoId) {
        ordemService.gerarMapa(eventoId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Prazo, horário limite, setor e responsável de uma ordem. */
    @PutMapping("/mapa/plano/{eventoId}")
    public ResponseEntity<Void> planejarOrdem(@PathVariable Long eventoId, @RequestBody @Valid PlanoOrdemRequestDTO req) {
        ordemService.planejar(eventoId, req);
        return ResponseEntity.noContent().build();
    }

    /** Ficha mestre: responsável geral e horários de serviço. */
    @PutMapping("/mapa/mestre/{eventoId}")
    public ResponseEntity<Void> salvarMestre(@PathVariable Long eventoId, @RequestBody @Valid MestreMapaRequestDTO req) {
        ordemService.salvarMestre(eventoId, req);
        return ResponseEntity.noContent().build();
    }

    /** Libera as ordens para a equipe ou recolhe de volta. */
    @PutMapping("/mapa/liberar/{eventoId}")
    public ResponseEntity<Void> liberarMapa(@PathVariable Long eventoId, @RequestBody @Valid LiberarMapaRequestDTO req) {
        ordemService.liberarMapa(eventoId, req);
        return ResponseEntity.noContent().build();
    }

    /** Setores da cozinha: renomear, ativar/desativar e criar. */
    @PutMapping("/setores/salvar")
    public ResponseEntity<Void> salvarSetores(@RequestBody @Valid SalvarSetoresRequestDTO req) {
        ordemService.salvarSetores(req);
        return ResponseEntity.noContent().build();
    }

    /** Conferências e horário de saída da liberação para expedição. */
    @PutMapping("/liberacao/salvar/{eventoId}")
    public ResponseEntity<Void> salvarLiberacao(@PathVariable Long eventoId, @RequestBody @Valid SalvarLiberacaoRequestDTO req) {
        ordemService.salvarLiberacao(eventoId, req);
        return ResponseEntity.noContent().build();
    }

    /** Libera a saída (com pendências justificadas quando nem tudo está pronto) e conclui a ordem de expedição. */
    @PostMapping("/liberacao/autorizar/{eventoId}")
    public ResponseEntity<Void> autorizarLiberacao(@PathVariable Long eventoId, @RequestBody @Valid AutorizarLiberacaoRequestDTO req) {
        ordemService.autorizarLiberacao(eventoId, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/liberacao/desfazer/{eventoId}")
    public ResponseEntity<Void> desfazerLiberacao(@PathVariable Long eventoId) {
        ordemService.desfazerLiberacao(eventoId);
        return ResponseEntity.noContent().build();
    }

    /* ---------------------------------- Ocorrências ---------------------------------- */

    /** Registra o que fugiu do planejado no evento. */
    @PostMapping("/ocorrencias/criar")
    public ResponseEntity<OcorrenciaDTO> criarOcorrencia(@RequestBody @Valid CriarOcorrenciaRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cozinhaService.criarOcorrencia(req));
    }

    /** Marca a ocorrência como resolvida ou reabre. */
    @PatchMapping("/ocorrencias/resolver/{id}")
    public ResponseEntity<OcorrenciaDTO> resolverOcorrencia(@PathVariable Long id, @RequestBody @Valid ResolverOcorrenciaRequestDTO req) {
        return ResponseEntity.ok(cozinhaService.resolverOcorrencia(id, req.resolvida()));
    }
}
