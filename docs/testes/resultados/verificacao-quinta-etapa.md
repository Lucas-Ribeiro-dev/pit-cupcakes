# Registro da quinta etapa — checkout, pedidos, pagamento simulado e administração

Implementação conforme a [SPEC de conclusão funcional](../../../specs/12-SPEC-CONCLUSAO-FUNCIONAL.md), integrando os módulos existentes de catálogo, identidade, endereços, frete e carrinho em sessão.

## 1. Blocos implementados

1. **Migração e domínio de pedidos ([V4__criar_pedidos_e_pagamento.sql](../../../src/main/resources/db/migration/V4__criar_pedidos_e_pagamento.sql))**:
   - Criação das tabelas `pedido`, `item_pedido`, `endereco_pedido` e `pagamento_simulado`, preservando `V1`, `V2` e `V3` intactas.
   - Restrições de integridade no banco: `uk_pedido_usuario_revisao (usuario_id, revisao_id)`, `chk_pedido_valores`, `chk_pedido_coerencia_pagamento_atendimento` e `chk_pedido_coerencia_modalidade_atendimento`.
   - Carga opcional idempotente de faixas de frete para o perfil `demo` ([DemoShippingRangeLoader](../../../src/main/java/br/com/pit/cupcakes/frete/demo/DemoShippingRangeLoader.java)): `70000000`–`70999999` (`R$ 8,00`) e `71000000`–`71999999` (`R$ 12,00`).
2. **Bloco 1 — Checkout integrado ao carrinho, endereço e frete ([CheckoutController](../../../src/main/java/br/com/pit/cupcakes/pedido/controller/CheckoutController.java), [CheckoutService](../../../src/main/java/br/com/pit/cupcakes/pedido/service/CheckoutService.java))**:
   - `POST /api/conta/checkout/revisao`: exige autenticação, rejeita carrinho vazio ou com itens bloqueados, valida endereço próprio em `ENTREGA` ou frete zero em `RETIRADA`, calcula todos os valores no servidor e armazena `CheckoutRevision` na sessão por 15 minutos.
3. **Bloco 2 — Pagamento simulado e gravação transacional ([OrderController](../../../src/main/java/br/com/pit/cupcakes/pedido/controller/OrderController.java), [CheckoutService](../../../src/main/java/br/com/pit/cupcakes/pedido/service/CheckoutService.java))**:
   - `POST /api/conta/pedidos`: lock pessimista (`FOR UPDATE`) nos produtos em ordem crescente de ID, no endereço e na faixa de frete; revalidação completa contra a revisão da sessão; gravação atômica de `pedido`, `item_pedido`, `endereco_pedido` e `pagamento_simulado`.
   - Idempotência por `(usuario_id, revisao_id)`: repetição, concorrência ou recuperação de resposta perdida retornam `200 OK` com o mesmo pedido; tentativa de reutilizar a mesma revisão com `resultadoSimulado` diferente retorna `409 CONFIRMACAO_DIVERGENTE`.
   - Limpeza do carrinho em sessão ocorre somente após o commit de pedido `APROVADO`; em caso de rollback ou pagamento `RECUSADO` (`NAO_AUTORIZADO`), o carrinho é mantido.
4. **Bloco 3 — Consulta dos próprios pedidos pelo cliente ([OrderController](../../../src/main/java/br/com/pit/cupcakes/pedido/controller/OrderController.java), [OrderService](../../../src/main/java/br/com/pit/cupcakes/pedido/service/OrderService.java))**:
   - `GET /api/conta/pedidos` e `GET /api/conta/pedidos/{id}` filtrados por `usuario_id` da sessão (`404 PEDIDO_NAO_ENCONTRADO` para pedido inexistente ou de terceiros), preservando nome/preço histórico dos itens e endereço congelado da compra.
5. **Bloco 4 — Administração de produtos, atendimento de pedidos e provisionamento seguro ([AdminProductController](../../../src/main/java/br/com/pit/cupcakes/catalogo/controller/AdminProductController.java), [AdminOrderController](../../../src/main/java/br/com/pit/cupcakes/pedido/controller/AdminOrderController.java), [AdminProvisioningService](../../../src/main/java/br/com/pit/cupcakes/identidade/service/AdminProvisioningService.java))**:
   - `GET/POST/PUT /api/admin/produtos`: validação de limites compatíveis com `V1`, controle de versão otimista (`409 PRODUTO_DESATUALIZADO`) e restrição de imagens a recursos estáticos locais `/assets/img/*.svg`.
   - `GET /api/admin/pedidos`, `GET /api/admin/pedidos/{id}` e `PUT /api/admin/pedidos/{id}/status`: transições validadas por modalidade (`ENTREGA` usa `SAIU_PARA_ENTREGA`; `RETIRADA` usa `PRONTO_PARA_RETIRADA`), cancelamento permitido apenas antes de sair para entrega ou concluir, resposta idempotente ao repetir o mesmo estado e bloqueio de estados finais (`NAO_AUTORIZADO`, `CANCELADO`, `CONCLUIDO`).
   - Provisionamento controlado via `ADMIN_EMAIL`, `ADMIN_NAME` e `ADMIN_PASSWORD` na inicialização ([AdminProvisioningRunner](../../../src/main/java/br/com/pit/cupcakes/identidade/service/AdminProvisioningRunner.java), ativo em `@Profile("!test & !test-pg")`), sem promover `CLIENTE` existente nem sobrescrever senha de `ADMIN` existente.
   - Proteção leve contra abuso de login (`LoginAttemptGuard` em [SecurityConfig](../../../src/main/java/br/com/pit/cupcakes/config/SecurityConfig.java)), retornando `429 MUITAS_TENTATIVAS_LOGIN` após tentativas inválidas consecutivas.
6. **Telas HTML/JS vanilla**:
   - Cliente: [checkout.html](../../../src/main/resources/static/paginas/compra/checkout.html), [pedidos.html](../../../src/main/resources/static/paginas/conta/pedidos.html) e [pedido.html](../../../src/main/resources/static/paginas/conta/pedido.html), além da integração em [carrinho.html](../../../src/main/resources/static/paginas/compra/carrinho.html), [minha-conta.html](../../../src/main/resources/static/paginas/conta/minha-conta.html) e navegação.
   - Administração: [admin/index.html](../../../src/main/resources/static/admin/index.html), [admin/produtos.html](../../../src/main/resources/static/admin/produtos.html) e [admin/pedidos.html](../../../src/main/resources/static/admin/pedidos.html).

## 2. Testes automatizados sem PostgreSQL externo (H2 + MockMvc)

Comando executado com `JAVA_HOME` apontando para o JDK 21:

```powershell
# Caso necessario, defina JAVA_HOME apontando para sua instalacao do JDK 21:
# $env:JAVA_HOME = '<CAMINHO_DO_JDK_21>'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd -q '-Dtest=CheckoutOrderAdminApiTest,CartApiTest,AddressValidationTest,AddressApiTest,PostalCodeClientTest,AuthenticationSecurityTest,RegistrationServiceTest,ProductControllerTest,ProductServiceTest,ProductRepositoryTest,DemoDataLoaderTest,PostgresTestDatabaseGuardTest' test
```

Resultado real obtido: **64 testes executados, 0 falhas, 0 erros, 0 ignorados (`BUILD SUCCESS`)**.

| Suite | Testes | Cobertura verificada |
| --- | ---: | --- |
| `CheckoutOrderAdminApiTest` | 9 | 1) Compra `RETIRADA` e `ENTREGA` com cálculo no servidor; 2) Pagamento `RECUSADO` persistido (`NAO_AUTORIZADO`), carrinho mantido e atendimento impedido; 3) Rejeição de carrinho vazio, item bloqueado, endereço alheio, revisão expirada e JSON adulterado; 4) Mudança de preço, disponibilidade, endereço e frete entre revisão e confirmação rejeitada sem pedido parcial; 5) Repetição, duas confirmações concorrentes e resposta perdida gerando apenas um pedido (`201` + `200`) e bloqueio de resultado divergente (`409`); 6) Falha de persistência com rollback completo sem limpar o carrinho; 7) Preservação histórica após edição de produto e exclusão de endereço, além de bloqueio a pedido alheio (`404`); 8) CRUD admin de produtos, separação `401`/`403`, provisionamento seguro e rate-limit de login (`429`); 9) Fluxo completo de atendimento por modalidade, versão, cancelamento e estados finais. |
| `CartApiTest` | 7 | Carrinho em sessão, soma, limites, concorrência, preservação no login e limpeza no logout. |
| `AddressValidationTest`, `AddressApiTest`, `PostalCodeClientTest` | 15 | Endereços por proprietário, versão otimista, cotação de frete e integração ViaCEP controlada. |
| `AuthenticationSecurityTest`, `RegistrationServiceTest` | 13 | Cadastro, normalização de e-mail, BCrypt, sessão, CSRF e permissões. |
| `ProductControllerTest`, `ProductServiceTest`, `ProductRepositoryTest`, `DemoDataLoaderTest`, `PostgresTestDatabaseGuardTest` | 20 | Catálogo público, carga demo e proteção obrigatória do banco de teste `cupcakes_test`. |

## 3. Diagnóstico de falha intermediária e execução final da rodada consolidada (PostgreSQL + Navegador Real)

### 3.1 Diagnóstico e correção do roteiro `executar_rodada_consolidada.ps1`

Na primeira tentativa de executar `.\docs\testes\casos\executar_rodada_consolidada.ps1`, foram identificados e corrigidos três problemas reais:

1. **Divergência de parâmetros no script**: a documentação mencionava `-ServerTarget StoreFlow`, enquanto a primeira versão do script possuía apenas `-SkipMavenTests` e `-StartBrowserServerOnly`. O script [executar_rodada_consolidada.ps1](../casos/executar_rodada_consolidada.ps1) foi atualizado para aceitar `-ServerTarget StoreFlow|Cart` (padrão `StoreFlow`), `-SkipTests`, `-SkipMavenTests` e `-StartBrowserServerOnly`.
2. **Interferência de variáveis `ADMIN_*` em testes (`test-pg`)**: os relatórios Surefire de `15:06` mostraram que `AdminProvisioningRunner` estava sendo acionado durante a inicialização do contexto `test-pg` quando havia variáveis `ADMIN_*` parciais definidas na sessão do terminal (`Provisionamento administrativo incompleto: defina simultaneamente ADMIN_EMAIL, ADMIN_NAME e ADMIN_PASSWORD`). A classe [AdminProvisioningRunner](../../../src/main/java/br/com/pit/cupcakes/identidade/service/AdminProvisioningRunner.java) foi restringida a `@Profile("!test & !test-pg")` e o script passou a limpar `Env:ADMIN_*` na sessão antes dos testes.
3. **Limpeza de fixtures de navegador antes das suítes de catálogo e frete**: quando uma execução anterior de `StoreFlowBrowserServer` era interrompida sem evento de encerramento, registros de `item_pedido` e `faixa_frete` das contas fictícias `fluxo-%@example.com` permaneciam em `cupcakes_test`, causando erro de FK em `ProductPostgresIntegrationTest` e conflito GiST em `OrderAndAdminPostgresIntegrationTest`. Foi criado o método estático `StoreFlowBrowserServer.cleanupFlowFixtures(...)`, invocado tanto na abertura/encerramento do `StoreFlowBrowserServer` quanto no início de `CartPostgresIntegrationTest` e `OrderAndAdminPostgresIntegrationTest`.

### 3.2 Resultados finais de Integração PostgreSQL (`cupcakes_test`, PostgreSQL 18.6)

Após as correções acima, a execução completa de `.\docs\testes\casos\executar_rodada_consolidada.ps1 -ServerTarget StoreFlow` às `15:13`–`15:14` gerou relatórios Surefire limpos: **15 testes PostgreSQL executados, 0 falhas, 0 erros, 0 ignorados (`BUILD SUCCESS`)**, somando **79 testes automatizados aprovados** no projeto (64 H2/MockMvc + 15 PostgreSQL real).

| Suite PostgreSQL (`test-pg`) | Testes | Falhas | Erros | Ignorados | Horário do XML (`target/surefire-reports`) |
| --- | ---: | ---: | ---: | ---: | --- |
| `CartPostgresIntegrationTest` | 1 | 0 | 0 | 0 | `2026-11-10 15:13:42` |
| `ProductPostgresIntegrationTest` | 5 | 0 | 0 | 0 | `2026-11-10 15:13:47` |
| `AddressRestartPostgresTest` | 1 | 0 | 0 | 0 | `2026-11-10 15:13:54` |
| `AddressShippingPostgresTest` | 5 | 0 | 0 | 0 | `2026-11-10 15:13:57` |
| `AuthenticationPostgresIntegrationTest` | 1 | 0 | 0 | 0 | `2026-11-10 15:13:58` |
| `AuthenticationRestartPostgresTest` | 1 | 0 | 0 | 0 | `2026-11-10 15:14:05` |
| `OrderAndAdminPostgresIntegrationTest` | 1 | 0 | 0 | 0 | `2026-11-10 15:14:06` |

### 3.3 Resultados e inspeção visual em Navegador Real (`StoreFlowBrowserServer` na porta `8094`)

Com o `StoreFlowBrowserServer` em execução na porta `8094`, foi executado:

```powershell
python docs/testes/casos/verificar_fluxo_completo.py --base-url http://localhost:8094
```

Resultado registrado em [fluxo-completo-navegador.json](fluxo-completo-navegador.json): **`"execucao": "concluída"`, 21 cenários aprovados, 20 capturas de tela do fluxo principal inspecionadas visualmente sem sobreposição ou rolagem horizontal indevida (`360px` e `1280px`) e 0 erros JavaScript**.

> **Nota sobre a coerência de `fluxo-completo-navegador.json`**: na primeira versão de `verificar_fluxo_completo.py`, o campo `execucao` era calculado por `"concluída" if len(cases) == 19 else "parcial"`, enquanto o roteiro registrava **21 cenários** (pois `CRT13` adiciona 2 registros ao validar `360px` e `1280px`). O script foi corrigido para definir `completed = True` ao concluir todos os cenários do bloco `try`, e a reexecução completa atualizou `fluxo-completo-navegador.json` para `"execucao": "concluída"`, além de gerar as evidências de antes/depois dos apontamentos de usabilidade (`AV01`, `AV02`, `AV04`, `AV05`) descritas em [avaliacao-participantes-e-laudo.md](../../entrega/avaliacao-participantes-e-laudo.md).

| Caso | Descrição verificada em navegador real | Evidência inspecionada |
| --- | --- | --- |
| `CRT01` | Inclusão de produto pelo catálogo e visualização do carrinho como visitante | [HU06_CRT01_adicionar-produto.png](../evidencias/HU06_CRT01_adicionar-produto.png), [HU06_CRT01_carrinho-visitante.png](../evidencias/HU06_CRT01_carrinho-visitante.png) |
| `CRT03` / `CRT08` | Alteração de quantidade, recarga de sessão, remoção e estado vazio | [HU07_CRT03_quantidade.png](../evidencias/HU07_CRT03_quantidade.png), [HU07_CRT03_removido.png](../evidencias/HU07_CRT03_removido.png) |
| `CRT13` | Navegação por teclado, foco visível e responsividade no carrinho em `360px` e `1280px` | [HU06_CRT13_carrinho-360-teclado.png](../evidencias/HU06_CRT13_carrinho-360-teclado.png), [HU06_CRT13_carrinho-1280-teclado.png](../evidencias/HU06_CRT13_carrinho-1280-teclado.png) |
| `CRT09` / `CHK01` | Login preservando carrinho da sessão e redirecionamento ao checkout (`1280px` e `360px`) | [CHK01_checkout-apos-login-1280.png](../evidencias/CHK01_checkout-apos-login-1280.png), [CHK01_checkout-360.png](../evidencias/CHK01_checkout-360.png) |
| `CHK02` | Revisão de `ENTREGA` com endereço salvo e frete (`R$ 8,00`) calculado no servidor | [CHK02_revisao-entrega.png](../evidencias/CHK02_revisao-entrega.png) |
| `CHK03` | Pagamento simulado `RECUSADO` (`NAO_AUTORIZADO`) gravado e carrinho preservado | [CHK03_pagamento-recusado.png](../evidencias/CHK03_pagamento-recusado.png) |
| `CHK04` | Nova revisão e pagamento simulado `APROVADO` (`RECEBIDO`) limpando o carrinho | [CHK04_pagamento-aprovado.png](../evidencias/CHK04_pagamento-aprovado.png) |
| `PED01` / `PED02` | Listagem de `Meus pedidos` (`1280px` e `360px`) e detalhes do pedido com itens e endereço históricos | [PED01_meus-pedidos-1280.png](../evidencias/PED01_meus-pedidos-1280.png), [PED01_meus-pedidos-360.png](../evidencias/PED01_meus-pedidos-360.png), [PED02_detalhe-pedido.png](../evidencias/PED02_detalhe-pedido.png) |
| `ADM01` | Bloqueio visual e de API das telas administrativas para conta `CLIENTE` | [ADM01_cliente-acesso-negado.png](../evidencias/ADM01_cliente-acesso-negado.png) |
| `ADM02` | Cadastro de produto pelo `ADMIN` e listagem em `1280px` e `360px` | [ADM02_admin-produtos-1280.png](../evidencias/ADM02_admin-produtos-1280.png), [ADM02_admin-produtos-360.png](../evidencias/ADM02_admin-produtos-360.png) |
| `ADM03` | Atendimento de pedido pelo `ADMIN` (`RECEBIDO` → `EM_PREPARACAO` → `SAIU_PARA_ENTREGA` → `CONCLUIDO`) em `1280px` e `360px` | [ADM03_admin-atendimento-1280.png](../evidencias/ADM03_admin-atendimento-1280.png), [ADM03_admin-atendimento-360.png](../evidencias/ADM03_admin-atendimento-360.png) |
| `PED03` | Acompanhamento pelo cliente constatando o status final `Concluído (CONCLUIDO)` | [PED03_acompanhamento-concluido.png](../evidencias/PED03_acompanhamento-concluido.png) |

## 4. Parâmetros suportados por `executar_rodada_consolidada.ps1`

O roteiro [executar_rodada_consolidada.ps1](../casos/executar_rodada_consolidada.ps1) aceita os seguintes parâmetros opcionais:

- Sem parâmetros (`.\docs\testes\casos\executar_rodada_consolidada.ps1`): solicita credenciais de `cupcakes_test` via `Get-Credential` (se ausentes no processo), executa as 7 suítes PostgreSQL e inicia o `StoreFlowBrowserServer` na porta `8094`.
- `-ServerTarget StoreFlow` (padrão) ou `-ServerTarget Cart`: seleciona entre `StoreFlowBrowserServer` (porta `8094`) e `CartBrowserServer` (porta `8093`).
- `-SkipTests` / `-SkipMavenTests` / `-StartBrowserServerOnly`: pula a execução do Maven Surefire e inicia diretamente o servidor isolado de navegador.
