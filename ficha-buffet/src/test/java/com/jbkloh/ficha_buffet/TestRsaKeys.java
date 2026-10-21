package com.jbkloh.ficha_buffet;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Gera um par de chaves RSA temporário para os testes que sobem o contexto
 * completo (@SpringBootTest). O RSAKeyProperties lê as chaves do disco, e as
 * chaves reais não ficam no repositório. O par é gerado uma vez por execução.
 */
public final class TestRsaKeys {

    private static Path publicKeyPath;
    private static Path privateKeyPath;

    private TestRsaKeys() {}

    public static void registrar(DynamicPropertyRegistry registry) {
        gerarSeNecessario();
        registry.add("RSA_PUBLIC_KEY_PATH", publicKeyPath::toString);
        registry.add("RSA_PRIVATE_KEY_PATH", privateKeyPath::toString);
    }

    private static synchronized void gerarSeNecessario() {
        if (publicKeyPath != null) {
            return;
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            Path dir = Files.createTempDirectory("ficha-test-keys");
            dir.toFile().deleteOnExit();

            publicKeyPath = escreverPem(dir.resolve("public.pem"), "PUBLIC KEY", keyPair.getPublic().getEncoded());
            privateKeyPath = escreverPem(dir.resolve("private.pem"), "PRIVATE KEY", keyPair.getPrivate().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar as chaves RSA de teste", e);
        }
    }

    private static Path escreverPem(Path path, String tipo, byte[] encoded) throws Exception {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(encoded);
        String pem = "-----BEGIN " + tipo + "-----\n" + base64 + "\n-----END " + tipo + "-----\n";
        Files.writeString(path, pem, StandardCharsets.UTF_8);
        path.toFile().deleteOnExit();
        return path;
    }
}
