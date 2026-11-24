package com.jbkloh.ficha_buffet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.resp.ProducaoResponseDTO;
import com.jbkloh.ficha_buffet.service.ProducaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.jbkloh.ficha_buffet.dtos.req.AjustarEtapaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AssociarEquipeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.AssociarEquipeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.req.ContratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.EventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PatchEventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PessoaEquipeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarLogisticaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarOrcamentoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancarEquipeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PessoaEquipeResponseDTO;
import com.jbkloh.ficha_buffet.service.EquipeEventoService;
import com.jbkloh.ficha_buffet.service.ProjetoEventoService;
import com.jbkloh.ficha_buffet.dtos.resp.EventoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoOpcoesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventosResponseDTO;
import com.jbkloh.ficha_buffet.service.EventoService;
import jakarta.validation.groups.Default;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController 
@RequestMapping ("/producao")
@RequiredArgsConstructor 
public class ProducaoController {
    private final ProducaoService producaoService;
    private final EventoService eventoService;
    private final EquipeEventoService equipeEventoService;
    private final ProjetoEventoService projetoEventoService;

    @GetMapping ("/retornarTodos")
    public ResponseEntity<List<ProducaoResponseDTO>>retornarTodos(){
        return ResponseEntity.ok(producaoService.listarEventos());
    }

    /* ---------- Anne Gestão: eventos (funil, ficha do evento e contrato) ---------- */

    /** Eventos do filtro (ativos, funil, contratados, realizados, arquivo ou todos), por data; `busca` olha código, nome, local, cidade e cliente. */
    @GetMapping("/eventos/retornarTodos")
    public ResponseEntity<EventosResponseDTO> retornarEventos(@RequestParam(defaultValue = "ativos") String filtro, @RequestParam(defaultValue = "") String busca) {
        return ResponseEntity.ok(eventoService.listarEventos(filtro, busca));
    }

    /** Opções de um formulário de evento: `para` = evento, contrato, lancamento, cardapio ou copiar-cardapio. */
    @GetMapping("/eventos/opcoes")
    public ResponseEntity<EventoOpcoesResponseDTO> opcoesEventos(@RequestParam String para) {
        return ResponseEntity.ok(eventoService.opcoes(para));
    }

    /** Ficha do evento pronta para a tela: contrato, parcelas, extras, cardápio e resultado. */
    @GetMapping("/eventos/detalhar/{id}")
    public ResponseEntity<EventoDetalheResponseDTO> detalharEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.detalharEvento(id));
    }

    @PostMapping("/eventos/criar")
    public ResponseEntity<EventoResponseDTO> criarEvento(@RequestBody @Valid EventoRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.criarEvento(req));
    }

    @PutMapping("/eventos/atualizar/{id}")
    public ResponseEntity<EventoResponseDTO> atualizarEvento(@PathVariable Long id, @RequestBody @Valid EventoRequestDTO req) {
        return ResponseEntity.ok(eventoService.atualizarEvento(id, req));
    }

    /** Etapa, margem, cardápio fechado ou reaberto e itens do checklist de envio: só o que vier no corpo muda. */
    @PatchMapping("/eventos/ajustar/{id}")
    public ResponseEntity<EventoResponseDTO> ajustarEvento(@PathVariable Long id, @RequestBody @Valid PatchEventoRequestDTO req) {
        return ResponseEntity.ok(eventoService.ajustarEvento(id, req));
    }

    /** Marca ou desmarca uma etapa do evento à mão, ou dispensa/inclui a degustação. */
    @PatchMapping("/eventos/etapas/ajustar/{id}")
    public ResponseEntity<Void> ajustarEtapa(@PathVariable Long id, @RequestBody @Valid AjustarEtapaRequestDTO req) {
        eventoService.ajustarEtapa(id, req);
        return ResponseEntity.noContent().build();
    }

    /** Exclui o evento com parcelas, extras, cardápio e lançamentos previstos; 409 se houver histórico de caixa ou estoque. */
    @DeleteMapping("/eventos/apagar/{id}")
    public ResponseEntity<Void> apagarEvento(@PathVariable Long id) {
        eventoService.apagarEvento(id);
        return ResponseEntity.noContent().build();
    }

    /** Registra o contrato e gera as parcelas, cada uma com a receita prevista no caixa. */
    @PostMapping("/eventos/contrato/registrar/{id}")
    public ResponseEntity<Void> registrarContrato(@PathVariable Long id,
            @RequestBody @Validated({ Default.class, ContratoRequestDTO.AoRegistrar.class }) ContratoRequestDTO req) {
        eventoService.registrarContrato(id, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Altera os dados do contrato sem refazer as parcelas. */
    @PutMapping("/eventos/contrato/atualizar/{id}")
    public ResponseEntity<Void> atualizarContrato(@PathVariable Long id, @RequestBody @Valid ContratoRequestDTO req) {
        eventoService.atualizarContrato(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/eventos/contrato/apagar/{id}")
    public ResponseEntity<Void> apagarContrato(@PathVariable Long id) {
        eventoService.apagarContrato(id);
        return ResponseEntity.noContent().build();
    }

    /* ---------- Escala de equipe do evento (pessoas do cadastro da equipe) ---------- */

    /** Escala alguém no evento: a pessoa escolhida (equipeId), a de mesmo nome no cadastro da equipe ou uma nova. */
    @PostMapping("/eventos/equipe/criar/{id}")
    public ResponseEntity<PessoaEquipeResponseDTO> criarPessoaEquipe(@PathVariable Long id, @RequestBody @Valid PessoaEquipeRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeEventoService.criar(id, req));
    }

    /** Escala em lote pelo checklist: a lista é a escala inteira (entra quem não estava, sai quem foi desmarcado). */
    @PostMapping("/eventos/equipe/associar/{id}")
    public ResponseEntity<AssociarEquipeResponseDTO> associarEquipe(@PathVariable Long id, @RequestBody @Valid AssociarEquipeRequestDTO req) {
        return ResponseEntity.ok(equipeEventoService.associar(id, req.equipeIds()));
    }

    /** Edita a pessoa na escala; nome, telefone e Pix mudam no cadastro da pessoa. */
    @PutMapping("/eventos/equipe/atualizar/{id}/{escalaId}")
    public ResponseEntity<PessoaEquipeResponseDTO> atualizarPessoaEquipe(@PathVariable Long id, @PathVariable Long escalaId,
            @RequestBody @Valid PessoaEquipeRequestDTO req) {
        return ResponseEntity.ok(equipeEventoService.atualizar(id, escalaId, req));
    }

    /** Tira da escala (a pessoa fica no cadastro da equipe); 409 se a diária já foi paga. */
    @DeleteMapping("/eventos/equipe/apagar/{id}/{escalaId}")
    public ResponseEntity<Void> apagarPessoaEquipe(@PathVariable Long id, @PathVariable Long escalaId) {
        equipeEventoService.apagar(id, escalaId);
        return ResponseEntity.noContent().build();
    }

    /** Diárias da escala ainda fora do caixa viram contas a pagar no dia do evento. */
    @PostMapping("/eventos/equipe/lancar/{id}")
    public ResponseEntity<LancarEquipeResponseDTO> lancarEquipe(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeEventoService.lancar(id));
    }

    /* ---------- Evento como projeto: logística, orçamento e fechamento ---------- */

    /** Substitui o plano de logística e transporte do evento (veículos, motorista, viagens, horários e observações). */
    @PutMapping("/eventos/logistica/salvar/{id}")
    public ResponseEntity<Void> salvarLogistica(@PathVariable Long id, @RequestBody @Valid SalvarLogisticaRequestDTO req) {
        projetoEventoService.salvarLogistica(id, req);
        return ResponseEntity.noContent().build();
    }

    /** Custos previstos fora das fichas (equipe sem escala, transporte, locação e outros); equipe nula mantém o valor gravado. */
    @PutMapping("/eventos/orcamento/salvar/{id}")
    public ResponseEntity<Void> salvarOrcamento(@PathVariable Long id, @RequestBody @Valid SalvarOrcamentoRequestDTO req) {
        projetoEventoService.salvarOrcamento(id, req);
        return ResponseEntity.noContent().build();
    }

    /** Passa o evento para Acertado e guarda o retrato do resultado; 409 se já fechado, 422 se perdido ou cancelado. */
    @PostMapping("/eventos/fechar/{id}")
    public ResponseEntity<EventoResponseDTO> fecharEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.fecharEvento(id));
    }
}
