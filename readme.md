# api-vendas · branch `v10-auth-reativo`

Evolução da branch `v9` com **autenticação JWT (access + refresh)**, **serviços reativos (WebFlux + R2DBC)**, **WebClient** entre microsserviços, **um PostgreSQL por serviço** e **testes com Testcontainers**.

## Arquitetura

```
Cliente ──► gateway :8085 (valida JWT) ──► auth-service     :8084  Spring MVC + Spring Data JDBC ─► authdb     :5433
                                       ├─► produtos-service :8081  WebFlux + R2DBC              ─► produtosdb :5434
                                       ├─► clientes-service :8083  WebFlux + R2DBC              ─► clientesdb :5435
                                       └─► vendas-service   :8082  WebFlux + R2DBC + WebClient  ─► vendasdb   :5436
                                                                    └── WebClient ──► produtos-service
eureka-server :8761 (descoberta)   ·   config-server :8888 (lê config-repo/)
```

O token é validado no gateway **e** em cada serviço protegido.

## Tecnologia: JWT

- Assinatura HMAC-SHA256 (`jjwt 0.12.6`). Mesmo segredo `jwt.secret` no auth-service (que assina) e no gateway e nos serviços (que validam).
- **Access token:** 2 min. **Refresh token:** 30 min, salvo no banco e **rotacionado** a cada uso.
- Refresh token não é aceito como access token, e vice-versa.
- Usuária demo: `mariana.motta@al.infnet.edu.br` / `admin123`.

## Como executar (IntelliJ + Maven)

1. **File → Open** na pasta raiz (o `pom.xml` agregador importa os 7 módulos).
2. Suba os bancos:
   ```bash
   docker compose up -d
   ```
3. Rode, em ordem, as configurações de `.run/`: **`A - Infra (eureka + config)`** e depois **`B - Microsservicos + gateway`**.

Para rodar tudo no Docker: `docker compose --profile app up -d --build`
Testes (com o Docker aberto): `mvn clean verify` ou a configuração **`Todos os testes`**.

## Endpoints (via gateway, porta 8085)

| Público | Protegido (`Authorization: Bearer <accessToken>`) |
|---|---|
| `POST /api/auth/register` | `GET/POST /api/produtos`, `GET /api/produtos/{id}` |
| `POST /api/auth/login` | `GET/POST /api/clientes`, `GET /api/clientes/{id}` |
| `POST /api/auth/refresh` | `GET/POST /api/vendas`, `GET /api/vendas/{id}`, `GET /api/vendas/minhas` |
| `POST /api/auth/logout` | |

Sem token, ou com token expirado, inválido ou de refresh → **401**.

## Exemplos

```bash
curl -X POST http://localhost:8085/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"mariana.motta@al.infnet.edu.br","senha":"admin123"}'

curl http://localhost:8085/api/clientes -H "Authorization: Bearer <accessToken>"

curl -X POST http://localhost:8085/api/auth/refresh -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'

curl -X POST http://localhost:8085/api/vendas -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" -d '{"idProduto":1,"quantidade":2}'
```

O roteiro completo da demonstração está em **`requests.http`** (roda no HTTP Client do IntelliJ e guarda os tokens sozinho): sem token → login inválido → login → rota protegida → refresh → novo token → refresh antigo rejeitado.

## Testes (Testcontainers + PostgreSQL)

| Módulo | Cobertura |
|---|---|
| auth-service | `@DataJdbcTest` dos repositórios; fluxo de login, refresh, rotação e logout |
| produtos / clientes | `@DataR2dbcTest` com `StepVerifier`; rotas WebFlux protegidas com `WebTestClient` |
| vendas-service | `@DataR2dbcTest`; fluxo com WebClient chamando um produtos-service falso e repassando o token |
| gateway | `TokenFilter`: rotas públicas, token ausente, expirado ou adulterado |

> **Windows:** os `pom.xml` e as configurações de `.run/` passam `-Djdk.net.unixdomain.tmpdir=<modulo>/target`. Isso evita o erro `Unable to establish loopback connection` quando o caminho do usuário tem espaço.
