package com.jbkloh.ficha_buffet.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.LoginRequestDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.PermissoesUsuarioEntity;
import com.jbkloh.ficha_buffet.model.UsuarioEntity;
import com.jbkloh.ficha_buffet.repositories.PermissoesUsuarioRepository;
import com.jbkloh.ficha_buffet.repositories.UsuarioRepository;
import com.jbkloh.ficha_buffet.service.TokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AutenticacaoController {

    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final PermissoesUsuarioRepository permissoesUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequestDTO req) {
        var authToken = new UsernamePasswordAuthenticationToken(req.username(), req.password());
        Authentication authentication = authenticationManager.authenticate(authToken);
        UsuarioEntity usuario = (UsuarioEntity) authentication.getPrincipal();
        String jwtToken = tokenService.gerarToken(usuario);
        ResponseCookie cookie = ResponseCookie.from("accessToken", jwtToken)
                .httpOnly(true)
                .secure(cookieSecure) // true em produção (HTTPS) via property cookie.secure
                .path("/")
                .maxAge(tokenService.getExpirationSeconds()) // mesma validade do JWT — nunca deixa o cookie sobreviver ao token
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0) // instrui o browser a apagar o cookie imediatamente
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/inserirRoleAdmin")
    public ResponseEntity<Void> inserirAdminBD() {
        // Verifica se a permissão ROLE_ADMIN já está cadastrada antes de salvar
        
            PermissoesUsuarioEntity roleAdmin = new PermissoesUsuarioEntity();
            roleAdmin.setRole("ROLE_ADMIN");
            permissoesUsuarioRepository.save(roleAdmin);

            return ResponseEntity.ok().build();
    }

    @PostMapping("/criarUsuarioAdmin")
    public ResponseEntity<Void> criarAdmin(@RequestBody @Valid LoginRequestDTO req) {
        // 1. Busca a role ROLE_ADMIN no banco de dados
        PermissoesUsuarioEntity roleAdmin = permissoesUsuarioRepository.findByRole("ROLE_ADMIN")
            .orElseThrow(() -> new AppException("A permissão ROLE_ADMIN não está cadastrada no banco. Execute /inserirRoleAdmin primeiro.", HttpStatus.BAD_REQUEST));

        // 2. Verifica se o usuário admin já existe para evitar duplicidade
        if (usuarioRepository.findByUsername(req.username()).isPresent()) {
            throw new AppException("Usuário com o username informado já existe.", HttpStatus.BAD_REQUEST);
        }

        // 3. Instancia o usuário e criptografa a senha com BCrypt
        UsuarioEntity user = new UsuarioEntity();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setPermissoes(Set.of(roleAdmin));

        // 4. Salva o usuário administrador no banco de dados
        usuarioRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
