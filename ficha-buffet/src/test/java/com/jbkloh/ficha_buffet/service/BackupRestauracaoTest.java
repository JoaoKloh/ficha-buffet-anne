package com.jbkloh.ficha_buffet.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.jbkloh.ficha_buffet.TestRsaKeys;
import com.jbkloh.ficha_buffet.dtos.req.BackupArquivoDTO;
import com.jbkloh.ficha_buffet.dtos.req.ClienteRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.RestaurarBackupRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.RestaurarBackupResponseDTO;
import com.jbkloh.ficha_buffet.repositories.ClienteRepository;
import com.jbkloh.ficha_buffet.repositories.RegistroSistemaRepository;

/** Restauração de backup: recria o que falta com o id original e, no modo "todos", desfaz alterações. Sem transação de teste: cada registro tem a sua. */
@SpringBootTest
class BackupRestauracaoTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    @Autowired private BackupService backupService;
    @Autowired private ClienteService clienteService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private RegistroSistemaRepository registroRepository;

    @AfterEach
    void limpar() {
        clienteRepository.deleteAll();
        registroRepository.deleteAll();
    }

    @Test
    void restaurar_recriaOQueFaltaComOMesmoIdEVoltaOQueMudou() {
        ClienteResponseDTO ana = clienteService.criarCliente(new ClienteRequestDTO("Ana", "PF", "", "", "", ""));
        ClienteResponseDTO bia = clienteService.criarCliente(new ClienteRequestDTO("Bia", "PJ", "", "", "", "indicacao"));
        assertThat(backupService.registrar().total()).isEqualTo(2);
        assertThat(backupService.resumo().ultimo().contagens()).containsEntry("clientes", 2);
        BackupArquivoDTO arquivo = backupService.exportar();

        // Depois do backup: Ana some, Bia muda de nome e entra a Carla.
        clienteRepository.deleteById(ana.id());
        clienteService.atualizarCliente(bia.id(), new ClienteRequestDTO("Bia alterada", "PJ", "", "", "", "indicacao"));
        ClienteResponseDTO carla = clienteService.criarCliente(new ClienteRequestDTO("Carla", "PF", "", "", "", ""));

        assertThat(backupService.analisar(arquivo).linhas()).filteredOn(l -> l.colecao().equals("clientes")).singleElement().satisfies(l -> {
            assertThat(l.novos()).isEqualTo(1);
            assertThat(l.diferentes()).isEqualTo(1);
            assertThat(l.soNoSistema()).isEqualTo(1);
        });

        RestaurarBackupResponseDTO faltantes = backupService.restaurar(new RestaurarBackupRequestDTO("backup.json", "faltantes", arquivo));
        assertThat(faltantes).extracting(RestaurarBackupResponseDTO::total, RestaurarBackupResponseDTO::gravados, RestaurarBackupResponseDTO::conferidos)
            .containsExactly(1, 1, 1);
        assertThat(clienteRepository.findById(ana.id())).get().extracting(c -> c.getNome()).isEqualTo("Ana");
        assertThat(clienteRepository.findById(bia.id())).get().extracting(c -> c.getNome()).isEqualTo("Bia alterada");

        RestaurarBackupResponseDTO todos = backupService.restaurar(new RestaurarBackupRequestDTO("backup.json", "todos", arquivo));
        assertThat(todos.gravados()).isEqualTo(1);
        assertThat(todos.falhas()).isEmpty();
        assertThat(clienteRepository.findById(bia.id())).get().extracting(c -> c.getNome()).isEqualTo("Bia");
        // Nunca apaga, e a sequência continua depois do maior id.
        assertThat(clienteRepository.existsById(carla.id())).isTrue();
        assertThat(clienteService.criarCliente(new ClienteRequestDTO("Duda", "PF", "", "", "", "")).id()).isGreaterThan(carla.id());
        assertThat(registroRepository.findById("restauracao")).get().extracting(r -> r.getDados().get("modo")).isEqualTo("todos");
    }

    @Test
    void registroQueNaoPodeSerGravado_viraFalhaSemDerrubarOsDemais() {
        ClienteResponseDTO ana = clienteService.criarCliente(new ClienteRequestDTO("Ana", "PF", "", "", "", ""));
        BackupArquivoDTO original = backupService.exportar();
        Map<String, Map<String, Map<String, Object>>> colecoes = new HashMap<>(original.colecoes());
        // Evento de um cliente que não existe: a chave estrangeira recusa.
        Map<String, Object> evento = new HashMap<>(Map.of("codigo", "99-001", "nome", "Órfão", "clienteId", 987654, "tipo", "outro", "status", "lead", "dataEvento", "",
            "horaInicio", "", "margem", 0));
        colecoes.put("eventos", Map.of("555", evento));
        Map<String, Map<String, Object>> clientes = new HashMap<>(colecoes.get("clientes"));
        clientes.put(String.valueOf(ana.id() + 100), new HashMap<>(Map.of("nome", "Nova", "tipo", "PF")));
        colecoes.put("clientes", clientes);

        RestaurarBackupResponseDTO r = backupService.restaurar(new RestaurarBackupRequestDTO("", "faltantes", new BackupArquivoDTO(original.app(), original.exportadoEm(), colecoes)));

        assertThat(r.total()).isEqualTo(2);
        assertThat(r.gravados()).isEqualTo(1);
        assertThat(r.falhas()).singleElement().satisfies(f -> {
            assertThat(f.colecao()).isEqualTo("eventos");
            assertThat(f.id()).isEqualTo("555");
        });
        assertThat(clienteRepository.findById(ana.id() + 100)).get().extracting(c -> c.getNome()).isEqualTo("Nova");
    }
}
