package com.jbkloh.ficha_buffet.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.AnalisarBackupRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.BackupArquivoDTO;
import com.jbkloh.ficha_buffet.dtos.req.RestaurarBackupRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.AnaliseBackupResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.BackupResumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.RestaurarBackupResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.UltimoBackupDTO;
import com.jbkloh.ficha_buffet.service.BackupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Backup e restauração pelo próprio sistema. */
@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final BackupService backupService;

    @GetMapping("/resumo")
    public ResponseEntity<BackupResumoResponseDTO> resumo() {
        return ResponseEntity.ok(backupService.resumo());
    }

    /** Backup completo (.json): a cópia exata, que permite restaurar. */
    @GetMapping("/exportar")
    public ResponseEntity<BackupArquivoDTO> exportar() {
        return ResponseEntity.ok(backupService.exportar());
    }

    /** Registra que um backup completo acabou de ser salvo. */
    @PostMapping("/registrar")
    public ResponseEntity<UltimoBackupDTO> registrar() {
        return ResponseEntity.status(HttpStatus.CREATED).body(backupService.registrar());
    }

    /** Planilha de consulta (.xlsx), como download. */
    @GetMapping("/planilha")
    public ResponseEntity<byte[]> planilha() {
        return ResponseEntity.ok().contentType(XLSX).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"anne-gestao-backup.xlsx\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store").body(backupService.planilha());
    }

    /** Confere o arquivo de backup contra o sistema, sem gravar nada. */
    @PostMapping("/analisar")
    public ResponseEntity<AnaliseBackupResponseDTO> analisar(@RequestBody @Valid AnalisarBackupRequestDTO req) {
        return ResponseEntity.ok(backupService.analisar(req.arquivo()));
    }

    /** Cria o que falta e, no modo "todos", volta os registros diferentes. Nunca apaga. */
    @PostMapping("/restaurar")
    public ResponseEntity<RestaurarBackupResponseDTO> restaurar(@RequestBody @Valid RestaurarBackupRequestDTO req) {
        return ResponseEntity.ok(backupService.restaurar(req));
    }
}
