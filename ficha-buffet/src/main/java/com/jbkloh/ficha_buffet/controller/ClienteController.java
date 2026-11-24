package com.jbkloh.ficha_buffet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.ClienteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteListaItemResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteResponseDTO;
import com.jbkloh.ficha_buffet.service.ClienteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    /** Clientes em ordem alfabética com o resumo dos eventos; `busca` filtra por nome, telefone, e-mail ou documento. */
    @GetMapping("/retornarTodos")
    public ResponseEntity<List<ClienteListaItemResponseDTO>> retornarTodos(@RequestParam(defaultValue = "") String busca) {
        return ResponseEntity.ok(clienteService.listarClientes(busca));
    }

    /** Um cliente com o resumo dos eventos (mesmo formato da lista). */
    @GetMapping("/detalhar/{id}")
    public ResponseEntity<ClienteListaItemResponseDTO> detalharCliente(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.detalharCliente(id));
    }

    @PostMapping("/criar")
    public ResponseEntity<ClienteResponseDTO> criarCliente(@RequestBody @Valid ClienteRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criarCliente(req));
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizarCliente(@PathVariable Long id, @RequestBody @Valid ClienteRequestDTO req) {
        return ResponseEntity.ok(clienteService.atualizarCliente(id, req));
    }

    @DeleteMapping("/apagar/{id}")
    public ResponseEntity<Void> apagarCliente(@PathVariable Long id) {
        clienteService.apagarCliente(id);
        return ResponseEntity.noContent().build();
    }
}
