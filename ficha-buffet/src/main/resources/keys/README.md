# Chaves RSA (assinatura dos JWTs)

`public.pem` e `private.pem` não são versionados (ver `.gitignore`) — cada
ambiente deve ter seu próprio par. Para gerar um novo par local:

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_pkcs1_tmp.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -in private_pkcs1_tmp.pem -out private.pem -nocrypt
openssl rsa -in private.pem -pubout -out public.pem
rm private_pkcs1_tmp.pem
```

Importante: `RSAKeyProperties` lê a chave privada com `PKCS8EncodedKeySpec`, então
ela precisa estar em formato PKCS#8 (`-----BEGIN PRIVATE KEY-----`), não PKCS#1
(`-----BEGIN RSA PRIVATE KEY-----`) — por isso o passo `pkcs8 -topk8` acima é
obrigatório; `openssl genpkey`/`genrsa` sozinho gera PKCS#1.

Em produção, gere um par dedicado e nunca reutilize o par de desenvolvimento.
