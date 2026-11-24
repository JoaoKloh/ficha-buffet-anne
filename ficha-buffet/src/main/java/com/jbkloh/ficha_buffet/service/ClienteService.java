package com.jbkloh.ficha_buffet.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Eventos;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.ClienteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteListaItemResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.ClienteEntity;
import com.jbkloh.ficha_buffet.repositories.ClienteRepository;
import com.jbkloh.ficha_buffet.repositories.EventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EventoRepository eventoRepository;
    private final DadosService dadosService;

    /** Clientes em ordem alfabética, filtrados por nome, telefone, e-mail ou documento, com o resumo dos eventos. */
    @Transactional(readOnly = true)
    public List<ClienteListaItemResponseDTO> listarClientes(String busca) {
        Dados d = dadosService.carregar();
        String q = Formato.busca(busca);
        String hoje = Formato.hoje();
        return d.clientes.stream()
            .filter(c -> q.isEmpty() || Formato.contem(String.join(" ", c.nome(), c.telefone(), c.email(), c.documento()), q))
            .sorted(Formato.porNome(ClienteResponseDTO::nome))
            .map(c -> comResumo(d, c, hoje)).toList();
    }

    /** Um cliente com o mesmo resumo de eventos da lista (a ficha do evento abre o cadastro dele). */
    @Transactional(readOnly = true)
    public ClienteListaItemResponseDTO detalharCliente(Long id) {
        Dados d = dadosService.carregar();
        ClienteResponseDTO cliente = d.clientes.stream().filter(c -> id.equals(c.id())).findFirst()
            .orElseThrow(() -> naoExiste(id));
        return comResumo(d, cliente, Formato.hoje());
    }

    private static ClienteListaItemResponseDTO comResumo(Dados d, ClienteResponseDTO c, String hoje) {
        List<EventoResponseDTO> eventos = d.eventos.stream().filter(e -> c.id().equals(e.clienteId())).toList();
        String proximo = eventos.stream().filter(e -> Eventos.ativo(e) && !Formato.vazio(e.dataEvento()) && e.dataEvento().compareTo(hoje) >= 0)
            .map(EventoResponseDTO::dataEvento).min(String::compareTo).orElse(null);
        return new ClienteListaItemResponseDTO(c, eventos.size(), proximo);
    }

    private static AppException naoExiste(Long id) {
        return new AppException("O cliente de id " + id + " não existe.", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public ClienteResponseDTO criarCliente(ClienteRequestDTO req) {
        ClienteEntity cliente = new ClienteEntity();
        aplicar(req, cliente);
        return GestaoMapper.cliente(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponseDTO atualizarCliente(Long id, ClienteRequestDTO req) {
        ClienteEntity cliente = buscar(id);
        aplicar(req, cliente);
        return GestaoMapper.cliente(cliente);
    }

    /** Só sem eventos: excluir apagaria a referência dos eventos. */
    @Transactional
    public void apagarCliente(Long id) {
        ClienteEntity cliente = buscar(id);
        if (eventoRepository.existsByClienteId(id)) {
            throw new AppException("Este cliente tem eventos e não pode ser excluído.", HttpStatus.CONFLICT);
        }
        clienteRepository.delete(cliente);
    }

    private void aplicar(ClienteRequestDTO req, ClienteEntity cliente) {
        cliente.setNome(req.nome().trim());
        cliente.setTipo(req.tipo() == null ? "PF" : req.tipo());
        cliente.setDocumento(Formato.texto(req.documento()).trim());
        cliente.setTelefone(Formato.texto(req.telefone()).trim());
        cliente.setEmail(Formato.texto(req.email()).trim());
        cliente.setOrigem(Formato.texto(req.origem()));
    }

    private ClienteEntity buscar(Long id) {
        return clienteRepository.findById(id).orElseThrow(() -> naoExiste(id));
    }
}
