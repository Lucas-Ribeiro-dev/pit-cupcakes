# PIT Cupcakes — Loja Virtual Completa (Catálogo, Conta, Endereços, Frete, Carrinho, Checkout, Pedidos e Administração)

Loja virtual acadêmica desenvolvida em Java 21, Spring Boot 3.4.2, Maven Wrapper 3.9.9, Spring Data JPA, Spring Security, Bean Validation, PostgreSQL, Flyway (`V1` a `V4`) e HTML/CSS/JavaScript vanilla.

## Estado funcional do projeto

Todos os módulos previstos nas especificações ([07-SPEC-CATALOGO](specs/07-SPEC-CATALOGO.md), [09-SPEC-AUTENTICACAO](specs/09-SPEC-AUTENTICACAO.md), [10-SPEC-ENDERECOS-FRETE](specs/10-SPEC-ENDERECOS-FRETE.md), [11-SPEC-CARRINHO](specs/11-SPEC-CARRINHO.md) e [12-SPEC-CONCLUSAO-FUNCIONAL](specs/12-SPEC-CONCLUSAO-FUNCIONAL.md)) estão implementados:

1. **Catálogo público e detalhes** (`/index.html`, `/produto.html?id=N`, `GET /api/produtos`, `GET /api/produtos/{id}`).
2. **Cadastro de cliente, autenticação, sessão, CSRF e proteção contra abuso de login** (`/paginas/conta/cadastro.html`, `/paginas/conta/login.html`, `/paginas/conta/minha-conta.html`, `/api/usuarios`, `/api/autenticacao/**`).
3. **Endereços do cliente, consulta opcional ViaCEP e cotação de frete por faixa ativa em Brasília–DF** (`/paginas/conta/enderecos.html`, `/api/conta/enderecos/**`, `/api/conta/cep/{cep}`, `POST /api/conta/frete/cotacao`).
4. **Carrinho de compras em sessão (`HttpSession`)** (`/paginas/compra/carrinho.html`, `/api/carrinho/**`), preservado no login e limpo no logout ou após confirmação de pedido aprovado.
5. **Checkout integrado, pagamento acadêmico simulado (`APROVADO` / `RECUSADO`), gravação transacional com idempotência e histórico de pedidos do cliente** (`/paginas/compra/checkout.html`, `/paginas/conta/pedidos.html`, `/paginas/conta/pedido.html`, `POST /api/conta/checkout/revisao`, `POST /api/conta/pedidos`, `GET /api/conta/pedidos`, `GET /api/conta/pedidos/{id}`).
6. **Administração de produtos e atendimento de pedidos com controle de permissão (`ROLE_ADMIN`) e concorrência otimista (`versao`)** (`/admin/index.html`, `/admin/produtos.html`, `/admin/pedidos.html`, `/api/admin/produtos/**`, `/api/admin/pedidos/**`).

## Preparação e execução local (`cupcakes`)

São necessários JDK 21 e PostgreSQL acessível em `localhost:5432` com os bancos `cupcakes` (desenvolvimento) e `cupcakes_test` (exclusivo para testes automatizados e verificação).

No PowerShell, configure `JAVA_HOME`, as credenciais locais do banco `cupcakes` e, opcionalmente, o provisionamento controlado de um administrador para testar o painel `/admin`:

```powershell
# Caso necessario, defina JAVA_HOME apontando para sua instalacao do JDK 21:
# $env:JAVA_HOME = '<CAMINHO_DO_JDK_21>'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$dbCred = Get-Credential -Message 'Credenciais locais do banco cupcakes'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/cupcakes'
$env:DB_USERNAME = $dbCred.UserName
$env:DB_PASSWORD = $dbCred.GetNetworkCredential().Password

# Opcional: provisionar conta administrativa na inicializacao (informe os tres ou nenhum)
$adminCred = Get-Credential -Message 'Credenciais do administrador local (UserName = e-mail)'
$env:ADMIN_EMAIL = $adminCred.UserName
$env:ADMIN_NAME = 'Administrador PIT Cupcakes'
$env:ADMIN_PASSWORD = $adminCred.GetNetworkCredential().Password

# Perfil local,demo carrega de forma idempotente os 4 cupcakes iniciais e 2 faixas de frete em Brasilia-DF (70000000-70999999 e 71000000-71999999)
$env:SPRING_PROFILES_ACTIVE = 'local,demo'
.\mvnw.cmd spring-boot:run
```

Na inicialização, o Flyway aplica as migrações pendentes até a [V4__criar_pedidos_e_pagamento.sql](src/main/resources/db/migration/V4__criar_pedidos_e_pagamento.sql) sem alterar `V1`, `V2` ou `V3`.

### Principais páginas da aplicação

- **Vitrine e compra**: [Catálogo](http://localhost:8080/index.html) | [Carrinho](http://localhost:8080/paginas/compra/carrinho.html) | [Checkout](http://localhost:8080/paginas/compra/checkout.html)
- **Conta do cliente**: [Entrar](http://localhost:8080/paginas/conta/login.html) | [Criar conta](http://localhost:8080/paginas/conta/cadastro.html) | [Minha conta](http://localhost:8080/paginas/conta/minha-conta.html) | [Meus endereços](http://localhost:8080/paginas/conta/enderecos.html) | [Meus pedidos](http://localhost:8080/paginas/conta/pedidos.html)
- **Administração (`ROLE_ADMIN`)**: [Painel admin](http://localhost:8080/admin/index.html) | [Produtos admin](http://localhost:8080/admin/produtos.html) | [Pedidos admin](http://localhost:8080/admin/pedidos.html)

## Contratos de Checkout, Pedidos e Administração (Etapa 5)

Todas as rotas `/api/conta/**` exigem sessão autenticada; todas as rotas `/api/admin/**` exigem perfil `ADMIN` (`401` para visitante, `403` para `CLIENTE`). Mutações (`POST`, `PUT`, `DELETE`) exigem cabeçalho CSRF.

| Endpoint | Entrada | Sucesso |
| --- | --- | --- |
| `POST /api/conta/checkout/revisao` | `modalidade` (`ENTREGA` com `enderecoId` e `versaoEndereco`, ou `RETIRADA`) | `200 OK`; `revisaoId`, itens validados, `subtotalItens`, `valorFrete`, `total` |
| `POST /api/conta/pedidos` | `revisaoId`, `resultadoSimulado` (`APROVADO` ou `RECUSADO`) | `201 Created` na criação; `200 OK` em repetição idempotente |
| `GET /api/conta/pedidos` | — | `200 OK`; resumos dos pedidos do cliente autenticado |
| `GET /api/conta/pedidos/{id}` | ID positivo do próprio cliente | `200 OK`; detalhes, itens históricos, endereço congelado e pagamento simulado |
| `GET /api/admin/produtos` | — | `200 OK`; todos os produtos ordenados por ID com `versao` |
| `POST /api/admin/produtos` | `nome`, `descricao`, `preco`, `imagemCaminho` opcional, `disponivel` | `201 Created`; produto criado |
| `PUT /api/admin/produtos/{id}` | Dados do produto e `versao` atual | `200 OK`; produto atualizado (`409` se desatualizado) |
| `GET /api/admin/pedidos` | — | `200 OK`; todos os pedidos ordenados do mais recente ao mais antigo |
| `GET /api/admin/pedidos/{id}` | ID positivo | `200 OK`; detalhes completos para atendimento (sem hash de senha) |
| `PUT /api/admin/pedidos/{id}/status` | `statusAtendimento` e `versao` atual | `200 OK`; transição validada por modalidade e estado (`409` em conflito) |

## Testes automatizados e rodada consolidada (`cupcakes_test`)

### 1. Suíte rápida sem PostgreSQL externo (H2 + MockMvc)

```powershell
# Caso necessario, defina JAVA_HOME apontando para sua instalacao do JDK 21:
# $env:JAVA_HOME = '<CAMINHO_DO_JDK_21>'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd '-Dtest=CheckoutOrderAdminApiTest,CartApiTest,AddressValidationTest,AddressApiTest,PostalCodeClientTest,AuthenticationSecurityTest,RegistrationServiceTest,ProductControllerTest,ProductServiceTest,ProductRepositoryTest,DemoDataLoaderTest,PostgresTestDatabaseGuardTest' test
```

Essa suíte executa **64 testes automatizados** (incluindo os 9 cenários integrados de `CheckoutOrderAdminApiTest`) com **0 falhas, 0 erros e 0 ignorados**.

### 2. Rodada única consolidada de PostgreSQL (`cupcakes_test`) e navegador real

A rodada consolidada contra PostgreSQL 18.6 (`cupcakes_test`) e navegador real (`Microsoft Edge` na porta `8094`) foi executada e aprovada com **15 testes PostgreSQL (`0` falhas)** e **21 cenários em navegador real (`0` erros JavaScript)**, totalizando **79 testes automatizados aprovados** no projeto (detalhes em [verificacao-quinta-etapa.md](docs/testes/resultados/verificacao-quinta-etapa.md) e [fluxo-completo-navegador.json](docs/testes/resultados/fluxo-completo-navegador.json)).

Para repetir a rodada consolidada localmente (sem parâmetros obrigatórios; `-ServerTarget StoreFlow` é o padrão, e `-SkipTests` ou `-StartBrowserServerOnly` podem ser usados para iniciar apenas o servidor em `8094`):

```powershell
.\docs\testes\casos\executar_rodada_consolidada.ps1
```

Com o servidor isolado em execução na porta `8094`, execute em outro terminal o roteiro Playwright do fluxo completo:

```powershell
python docs/testes/casos/verificar_fluxo_completo.py --base-url http://localhost:8094
```

## Documentação do projeto

- [Arquitetura e limites transacionais](docs/arquitetura/visao-geral.md) e [modelo de carrinho em sessão](docs/arquitetura/carrinho-em-sessao.md)
- [Dicionário de dados (V1 a V4)](docs/banco-de-dados/dicionario-dados.md) e [decisão técnica ViaCEP](docs/decisoes/DT01-viacep.md)
- [Histórias de usuário e casos de uso](docs/requisitos/historias-e-casos-de-uso.md) e [diagramas UML (classes, casos de uso, sequência e atividade)](docs/modelagem/diagramas-uml.md)
- [Manual de uso, operação e publicação](docs/manual/manual-usuario-e-publicacao.md)
- [Formulário de avaliação para 5 participantes e modelo de laudo](docs/entrega/avaliacao-participantes-e-laudo.md)
- Registros de verificação por etapa: [1ª etapa](docs/testes/resultados/verificacao-primeira-etapa.md), [2ª etapa](docs/testes/resultados/verificacao-segunda-etapa.md), [3ª etapa](docs/testes/resultados/verificacao-terceira-etapa.md), [4ª etapa](docs/testes/resultados/verificacao-quarta-etapa.md) e [5ª etapa](docs/testes/resultados/verificacao-quinta-etapa.md)
