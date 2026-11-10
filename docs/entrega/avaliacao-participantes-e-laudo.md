# Avaliações Recebidas, Retestes de Usabilidade e Laudo de Qualidade

Documento consolidado de avaliação com participantes e laudo técnico de qualidade do sistema **PIT Cupcakes**.

- Os relatos e as datas foram informados pelo responsável a partir das cinco sessões aplicadas.
- Para proteção de dados pessoais no repositório público, os avaliadores estão identificados apenas por código (`AV01` a `AV05` / `Participante 1` a `Participante 5`). A correspondência nominal permanece exclusivamente no arquivo local não versionado `docs/entrega/anexo-nominal-participantes.local.md` (ignorado pelo Git).
- Não foram acrescentadas notas numéricas, tempos de sessão, taxas percentuais ou respostas não coletadas no modelo da disciplina.
- As verificações técnicas e capturas de tela referem-se ao estado do código auditado nesta revisão; as imagens produzidas pela equipe são evidências técnicas de verificação e reteste, e não capturas históricas feitas pelos participantes.

---

## 1. Escopo das Tarefas Avaliadas

O fluxo submetido à avaliação prática abrange as funcionalidades centrais da loja virtual:

1. **Catálogo e Carrinho**: consulta da vitrine de cupcakes, visualização de detalhes, adição de itens ao carrinho e ajuste de quantidades.
2. **Cadastro, Autenticação e Endereço**: criação de conta de cliente, login com preservação do carrinho da sessão, preenchimento de endereço em Brasília–DF (com busca automática opcional por CEP via ViaCEP ou digitação manual) e cotação de frete.
3. **Checkout e Simulação de Pagamento (`APROVADO` e `RECUSADO`)**: escolha da modalidade (`ENTREGA` ou `RETIRADA`), geração da revisão com valores calculados no servidor e confirmação do pedido com pagamento acadêmico simulado aprovado ou recusado.
4. **Consulta de Pedidos e Atendimento**: verificação do histórico em **Meus pedidos**, visualização dos detalhes congelados da compra e acompanhamento das transições de status realizadas pela administração.

---

## 2. Registro dos Relatos Recebidos (`AV01` a `AV05`)

### 2.1 Quadro-síntese (sem identificação nominal no repositório público)

| Registro | Avaliador | Data informada | Síntese do apontamento |
| --- | --- | --- | --- |
| `AV01` | Participante 1 | `04/11/2026` | Confirmação de inclusão no carrinho pouco perceptível; não identificou a recusa simulada |
| `AV02` | Participante 2 | `06/11/2026` | Teclado alfanumérico no campo de CEP em aparelho móvel |
| `AV03` | Participante 3 | `07/11/2026` | Sugestão de estimativa de entrega; sem falha funcional relatada |
| `AV04` | Participante 4 | `09/11/2026` | Não conseguiu provocar recusa; cancelamento pelo cliente não testado |
| `AV05` | Participante 5 | `10/11/2026` | Sugestão de filtros; não identificou a recusa simulada |

### 2.2 Formulários individuais preservados (conteúdo recebido)

> Os blocos abaixo preservam estritamente os relatos e datas informados pelo responsável, sem adição de notas, métricas não coletadas ou conversão de cenários não testados em aprovados.

#### Bloco Individual — `AV01` (`Participante 1`)
- **Código do registro**: `AV01`
- **Avaliador**: Participante 1 *(identificação nominal mantida apenas no anexo local fora do Git)*
- **Data informada**: `04/11/2026`
- **Relato registrado**: Ao adicionar produto ao carrinho, considerou a confirmação visual pouco perceptível; durante a etapa de pagamento, não identificou como acionar a opção de recusa simulada.
- **Campos adicionais (notas / duração / métricas)**: Não coletados no relato original.

#### Bloco Individual — `AV02` (`Participante 2`)
- **Código do registro**: `AV02`
- **Avaliador**: Participante 2 *(identificação nominal mantida apenas no anexo local fora do Git)*
- **Data informada**: `06/11/2026`
- **Relato registrado**: Relatou abertura de teclado alfanumérico ao tocar no campo de CEP durante o cadastro de endereço em aparelho móvel, sugerindo abertura direta do teclado numérico.
- **Campos adicionais (notas / duração / métricas)**: Não coletados no relato original.

#### Bloco Individual — `AV03` (`Participante 3`)
- **Código do registro**: `AV03`
- **Avaliador**: Participante 3 *(identificação nominal mantida apenas no anexo local fora do Git)*
- **Data informada**: `07/11/2026`
- **Relato registrado**: Concluiu a navegação sem relatar falha funcional e sugeriu exibir uma estimativa de prazo de entrega junto ao cálculo de frete/revisão.
- **Campos adicionais (notas / duração / métricas)**: Não coletados no relato original.

#### Bloco Individual — `AV04` (`Participante 4`)
- **Código do registro**: `AV04`
- **Avaliador**: Participante 4 *(identificação nominal mantida apenas no anexo local fora do Git)*
- **Data informada**: `09/11/2026`
- **Relato registrado**: Relatou que não conseguiu provocar a recusa do pagamento simulado no checkout; informou também que o cancelamento de pedido pelo próprio cliente não foi testado.
- **Situação do cenário não testado**: Mantido como **não testado pelo participante** e **fora do escopo funcional do perfil cliente** (o cancelamento implementado no sistema é exclusivo da área administrativa em estados permitidos).
- **Campos adicionais (notas / duração / métricas)**: Não coletados no relato original.

#### Bloco Individual — `AV05` (`Participante 5`)
- **Código do registro**: `AV05`
- **Avaliador**: Participante 5 *(identificação nominal mantida apenas no anexo local fora do Git)*
- **Data informada**: `10/11/2026`
- **Relato registrado**: Sugeriu a inclusão de filtros de busca/categoria no catálogo e relatou não ter identificado claramente a opção de recusa simulada no checkout.
- **Campos adicionais (notas / duração / métricas)**: Não coletados no relato original.

---

## 3. Registro do Reteste Final e Tratamento dos Apontamentos

### 3.1 Distinção entre componentes simulados e fluxo interno real

- **Fluxo interno real validado**: autenticação por sessão HTTP (`HttpSession`), proteção CSRF, controle de acesso por perfil (`CLIENTE` / `ADMIN`), persistência transacional em PostgreSQL 18.6 (`cupcakes_test` com `PostgresTestDatabaseGuard` e migrações Flyway `V1` a `V4`), cálculo de subtotal/frete/total no servidor, lock pessimista (`FOR UPDATE`) na confirmação do pedido e controle otimista de versão (`versao`).
- **Componentes simulados ou acadêmicos por definição de escopo**:
  - **Pagamento (`APROVADO` / `RECUSADO`)**: simulação acadêmica explícita controlada pelo parâmetro `resultadoSimulado` enviado ao servidor (`POST /api/conta/pedidos`), sem gateway financeiro externo nem coleta de dados de cartão.
  - **Faixas de frete**: faixas acadêmicas de demonstração para Brasília–DF (`70000000`–`70999999` a `R$ 8,00` e `71000000`–`71999999` a `R$ 12,00`).
  - **Consulta ViaCEP**: integração HTTP real no uso da aplicação com fallback manual, e cliente controlado (`stub`) nos testes automatizados para reprodutibilidade.

### 3.2 Tabela técnica de verificação e reteste por apontamento (`AV01` a `AV05`)

> As verificações do estado anterior às melhorias de usabilidade desta revisão foram capturadas no commit base `7018e1c` (`feat: implementa carrinho em sessão, checkout, pedidos e administração`). As melhorias de destaque visual (`AV01`, `AV04`, `AV05`) foram aplicadas e retestadas em navegador real (`Microsoft Edge`, `StoreFlowBrowserServer` na porta `8094` sobre PostgreSQL `cupcakes_test`) no commit subsequente da branch `master`.

| Registro | Cenário e escopo verificado | Esperado | Obtido antes da melhoria (`7018e1c`) | Classificação | Alteração efetivamente realizada nesta revisão | Evidências técnicas (antes / depois) | Resultado do reteste |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `AV01` | Percepção da confirmação ao adicionar produto ao carrinho no catálogo (`/index.html`) e nos detalhes (`/produto.html`). | Retorno imediato, legível em `1280px` e `360px`, com foco/leitura acessível e atalho para conferência dos itens. | O código já exibia o alerta verde `"Produto adicionado ao carrinho."` e atualizava o contador do cabeçalho (`Carrinho (N)`), sem informar o nome do produto adicionado, sem atalho direto no banner e sem trazer o aviso para a área visível quando o usuário clicava em um cartão mais abaixo na página. | **Melhoria** *(funcionalidade já existia em `7018e1c`; aprimorada a percepção visual)* | Em [acoes-carrinho.js](../../src/main/resources/static/assets/js/cliente/acoes-carrinho.js), [catalogo.js](../../src/main/resources/static/assets/js/cliente/catalogo.js) e [style.css](../../src/main/resources/static/assets/css/style.css): inclusão do nome do produto na mensagem (`Produto adicionado ao carrinho: <nome>.`), link de ação `"Conferir itens"` dentro do alerta, atualização imediata do texto do botão clicado (`✓ Adicionado ao carrinho`) e rolagem suave (`scrollIntoView`) do alerta com borda destacada `.alert-cart-highlight`. | Antes: [AV01_antes_confirmacao-carrinho.png](../testes/evidencias/AV01_antes_confirmacao-carrinho.png)<br>Depois: [AV01_depois_confirmacao-carrinho.png](../testes/evidencias/AV01_depois_confirmacao-carrinho.png) e [HU06_CRT01_adicionar-produto.png](../testes/evidencias/HU06_CRT01_adicionar-produto.png) | **Aprovado no reteste em navegador desktop (`1280px` e `360px`)** |
| `AV02` | Abertura de teclado numérico no campo CEP (`/paginas/conta/enderecos.html`). | Campo CEP deve expor atributos semânticos para teclado numérico e manter máscara/validação de 8 dígitos. | Desde o commit `0e6514b` (`feat: adiciona endereços e cotação de frete`), o campo `#cep` em [enderecos.html](../../src/main/resources/static/paginas/conta/enderecos.html) já declara `type="text" inputmode="numeric" autocomplete="postal-code" maxlength="9"`. | **Não reproduzido na marcação HTML (`inputmode="numeric"` já presente)** / **Pendente de reteste em aparelho móvel físico** | Nenhuma alteração de código atribuída como correção, pois `inputmode="numeric"` já estava presente em `0e6514b` e `7018e1c`. Verificada a presença dos atributos no DOM em viewport de `360px`. | Inspeção técnica em `360px`: [AV02_cep-inputmode-360.png](../testes/evidencias/AV02_cep-inputmode-360.png) | **Atributo HTML confirmado; validação do teclado nativo permanece pendente de aparelho móvel real** |
| `AV03` | Sugestão de estimativa de prazo de entrega junto ao frete. | Manter escopo da especificação (cotação de valor por faixa de CEP ou retirada gratuita, sem cálculo logístico de prazo). | O sistema calcula e exibe o valor do frete (`R$ 8,00` / `R$ 12,00` nas faixas de demonstração ou `R$ 0,00` para retirada), sem prazo estimado de entrega. | **Melhoria futura / Fora do escopo** | Nenhuma alteração funcional implementada (mantido o escopo de `10-SPEC-ENDERECOS-FRETE` e `12-SPEC-CONCLUSAO-FUNCIONAL`). | Revisão de entrega: [CHK02_revisao-entrega.png](../testes/evidencias/CHK02_revisao-entrega.png) | **Escopo preservado; registrado como melhoria futura** |
| `AV04` | 1) Clareza e funcionamento da opção de recusa simulada no checkout (`/paginas/compra/checkout.html`);<br>2) Cancelamento de pedido pelo próprio cliente. | 1) Permitir selecionar claramente `APROVADO` ou `RECUSADO` após gerar a revisão, gravando tentativa `NAO_AUTORIZADO` e preservando o carrinho;<br>2) Cancelamento restrito ao administrador. | 1) No commit `7018e1c`, o passo 2 ficava oculto até clicar em `"Gerar revisão de valores"` sem aviso prévio no passo 1, e os rádios `APROVADO`/`RECUSADO` tinham aparência idêntica; o resultado recusado usava o mesmo estilo visual do aprovado;<br>2) Cancelamento pelo cliente não existe por definição de escopo. | 1) **Não reproduzido funcionalmente** (recusa grava `NAO_AUTORIZADO` e preserva carrinho) + **Melhoria de clareza visual aplicada**;<br>2) **Fora do escopo** (cancelamento pelo cliente). | Em [checkout.html](../../src/main/resources/static/paginas/compra/checkout.html), [checkout.js](../../src/main/resources/static/assets/js/cliente/checkout.js) e [style.css](../../src/main/resources/static/assets/css/style.css): adicionada nota orientativa no Passo 1 explicando que a escolha entre pagamento **APROVADO** e **RECUSADO** abre no Passo 2 após gerar a revisão; cartões de opção com bordas e subtítulos distintos para aprovação e recusa; painel de resultado com classe `.result-box-refused` (destaque âmbar/alerta) para recusa e `.result-box-approved` (destaque verde) para aprovação. | Escolha antes: [AV04_antes_checkout-escolha-pagamento.png](../testes/evidencias/AV04_antes_checkout-escolha-pagamento.png)<br>Escolha depois: [AV04_depois_checkout-escolha-pagamento.png](../testes/evidencias/AV04_depois_checkout-escolha-pagamento.png)<br>Resultado antes: [AV04_antes_resultado-recusa.png](../testes/evidencias/AV04_antes_resultado-recusa.png)<br>Resultado depois: [AV04_depois_resultado-recusa.png](../testes/evidencias/AV04_depois_resultado-recusa.png) e [CHK03_pagamento-recusado.png](../testes/evidencias/CHK03_pagamento-recusado.png) | **Aprovado no reteste de escolha e confirmação de recusa (`1280px` e `360px`); cancelamento pelo cliente mantido fora do escopo** |
| `AV05` | 1) Sugestão de filtros no catálogo;<br>2) Clareza da recusa simulada no checkout (`AV01`/`AV04`). | 1) Catálogo público lista produtos ordenados por ID conforme `07-SPEC-CATALOGO`;<br>2) Opção de recusa simulada claramente identificável no checkout. | 1) Catálogo sem filtros por categoria (conforme especificação);<br>2) Escolha de recusa simulada compartilhava o mesmo ponto de visibilidade relatado em `AV01` e `AV04`. | 1) **Melhoria futura / Fora do escopo** (filtros no catálogo);<br>2) **Melhoria aplicada** (clareza da recusa simulada). | 1) Filtros mantidos como evolução futura fora do escopo;<br>2) Aplicadas e validadas as melhorias visuais de orientação e contraste das opções `APROVADO`/`RECUSADO` descritas em `AV04`. | [AV04_depois_checkout-escolha-pagamento.png](../testes/evidencias/AV04_depois_checkout-escolha-pagamento.png) e [AV04_depois_resultado-recusa.png](../testes/evidencias/AV04_depois_resultado-recusa.png) | **Melhoria da recusa aprovada no reteste; filtros registrados como evolução futura** |

---

## 4. Laudo de Qualidade Consolidado

O sistema **PIT Cupcakes** foi submetido à verificação integrada de catálogo, conta, autenticação, endereços, carrinho em sessão, frete por faixa de CEP, checkout, pagamento acadêmico simulado, consulta de pedidos pelo cliente e administração de produtos e atendimento. A validação combinou testes automatizados de regras e contratos (H2/MockMvc), testes de integração contra banco PostgreSQL 18.6 isolado (`cupcakes_test` protegido por `PostgresTestDatabaseGuard`) e execução automatizada de ponta a ponta em navegador real (`Microsoft Edge` sobre `StoreFlowBrowserServer` na porta `8094`).

### 4.1 Resultados técnicos e correções do processo de teste

1. **Suíte automatizada (79 testes aprovados, 0 falhas, 0 erros, 0 ignorados)**:
   - **64 testes de API, regras de negócio e regressão** em memória/MockMvc (`CheckoutOrderAdminApiTest`, `CartApiTest`, `AddressValidationTest`, `AddressApiTest`, `PostalCodeClientTest`, `AuthenticationSecurityTest`, `RegistrationServiceTest`, `ProductControllerTest`, `ProductServiceTest`, `ProductRepositoryTest`, `DemoDataLoaderTest`, `PostgresTestDatabaseGuardTest`).
   - **15 testes de integração em PostgreSQL real (`cupcakes_test`)** (`CartPostgresIntegrationTest`, `ProductPostgresIntegrationTest`, `AddressRestartPostgresTest`, `AddressShippingPostgresTest`, `AuthenticationPostgresIntegrationTest`, `AuthenticationRestartPostgresTest`, `OrderAndAdminPostgresIntegrationTest`).
   - As execuções de H2/MockMvc e de PostgreSQL são distintas e complementares; esse total comprova os cenários verificados, sem representar cobertura irrestrita de 100% de linhas de código.
2. **Saneamento do ambiente de testes (ocorrências internas do processo de verificação, independentes dos avaliadores)**:
   - Alinhamento dos parâmetros aceitos por [executar_rodada_consolidada.ps1](../testes/casos/executar_rodada_consolidada.ps1) (`-ServerTarget StoreFlow|Cart`, `-SkipTests`, `-SkipMavenTests`, `-StartBrowserServerOnly`).
   - Restrição de [AdminProvisioningRunner](../../src/main/java/br/com/pit/cupcakes/identidade/service/AdminProvisioningRunner.java) a `@Profile("!test & !test-pg")` e limpeza preventiva de variáveis `ADMIN_*` no script de testes para impedir falha de contexto quando o terminal local possuir variáveis parciais.
   - Limpeza determinística de dados fictícios (`StoreFlowBrowserServer.cleanupFlowFixtures(...)`) para evitar resíduos de chaves estrangeiras ou faixas de frete entre execuções interrompidas.
3. **Verificação e correção do registro estruturado de navegador ([fluxo-completo-navegador.json](../testes/resultados/fluxo-completo-navegador.json))**:
   - Foi diagnosticada a causa pela qual `fluxo-completo-navegador.json` marcava `"execucao": "parcial"` mesmo com todos os cenários aprovados: o script [verificar_fluxo_completo.py](../testes/casos/verificar_fluxo_completo.py) continha a verificação fixa `len(cases) == 19`, enquanto o fluxo completo registra **21 cenários** (pois `CRT13` é verificado e registrado separadamente em `360px` e `1280px`).
   - O critério de encerramento do script foi corrigido para marcar conclusão ao final do bloco `try` após a aprovação de todos os cenários, e a rodada completa foi reexecutada contra `http://localhost:8094`, gerando [fluxo-completo-navegador.json](../testes/resultados/fluxo-completo-navegador.json) com **`"execucao": "concluída"`, 21 cenários aprovados, 20 capturas de fluxo e 0 erros JavaScript**.

### 4.2 Síntese da avaliação de usabilidade e escopo

- **Inclusão no carrinho (`AV01`)**: embora a versão base (`7018e1c`) já apresentasse a mensagem `"Produto adicionado ao carrinho."`, o reteste confirmou que o retorno visual ficava distante quando o usuário acionava botões na parte inferior da vitrine. Foi aplicada melhoria com nome do cupcake adicionado, atalho `"Conferir itens"`, feedback no próprio botão clicado e rolagem suave até o alerta destacado.
- **Teclado numérico no CEP (`AV02`)**: o código já declarava `inputmode="numeric"` desde a entrega de endereços (`0e6514b`). Como a verificação por código e por viewport de `360px` em navegador desktop não comprova o comportamento do teclado virtual nativo de sistemas Android/iOS, a validação em aparelho celular físico permanece explicitamente registrada como pendência.
- **Escolha e resultado do pagamento simulado (`AV04`, `AV05`)**: o funcionamento transacional da recusa (`NAO_AUTORIZADO` persistido e itens mantidos no carrinho) já estava operacional no commit `7018e1c`. Para eliminar a dúvida relatada pelos participantes sobre onde provocar a recusa, foi adicionada orientação explícita no Passo 1 do checkout, diferenciação visual entre os cartões de rádio **APROVADO** e **RECUSADO** no Passo 2 e distinção cromática entre os painéis de resultado aprovado (`.result-box-approved`) e recusado (`.result-box-refused`).
- **Sugestões fora do escopo (`AV03`, `AV04`, `AV05`)**: estimativa de prazo de entrega (`AV03`), filtros de busca por categoria no catálogo (`AV05`) e cancelamento de pedido pelo próprio cliente (`AV04`) foram registrados como melhorias futuras fora do escopo atual das especificações.

### 4.3 Parecer técnico final

O parecer técnico é **favorável à aprovação funcional e de qualidade no ambiente local verificado**, com todos os 79 testes automatizados e 21 cenários de navegador aprovados e com as melhorias de usabilidade de `AV01`, `AV04` e `AV05` implementadas e retestadas. Permanecem registradas de forma transparente apenas as pendências que exigem hardware móvel físico (`AV02`) e a etapa posterior de implantação em hospedagem gratuita HTTPS.

---

## 5. Índice de Evidências Técnicas

Caminhos relativos à raiz do repositório:

| ID | Caminho da evidência | O que comprova |
| --- | --- | --- |
| `EV01` | [docs/testes/evidencias/HU06_CRT01_adicionar-produto.png](../testes/evidencias/HU06_CRT01_adicionar-produto.png) | Confirmação de inclusão no carrinho durante o fluxo integrado completo |
| `EV02` | [docs/testes/evidencias/CHK03_pagamento-recusado.png](../testes/evidencias/CHK03_pagamento-recusado.png) | Recusa simulada (`NAO_AUTORIZADO`) gravada e preservação do carrinho |
| `EV03` | [docs/testes/evidencias/CHK04_pagamento-aprovado.png](../testes/evidencias/CHK04_pagamento-aprovado.png) | Pagamento simulado aprovado (`RECEBIDO`) e confirmação do pedido |
| `EV04` | [docs/testes/evidencias/CHK02_revisao-entrega.png](../testes/evidencias/CHK02_revisao-entrega.png) | Revisão de entrega, composição de valores no servidor e opções de simulação |
| `EV05` | [docs/testes/evidencias/PED02_detalhe-pedido.png](../testes/evidencias/PED02_detalhe-pedido.png) | Detalhes do pedido registrado com itens e endereço históricos |
| `EV06` | [docs/testes/evidencias/ADM01_cliente-acesso-negado.png](../testes/evidencias/ADM01_cliente-acesso-negado.png) | Restrição da área administrativa ao perfil `CLIENTE` (`403`) |
| `EV07` | [docs/testes/evidencias/ADM03_admin-atendimento-1280.png](../testes/evidencias/ADM03_admin-atendimento-1280.png) | Fluxo administrativo de atendimento até `CONCLUIDO` |
| `EV08` | [docs/testes/evidencias/PED03_acompanhamento-concluido.png](../testes/evidencias/PED03_acompanhamento-concluido.png) | Consulta do estado final pelo cliente |
| `EV09` | [docs/testes/resultados/persistencia-antes.json](../testes/resultados/persistencia-antes.json) e [persistencia-depois.json](../testes/resultados/persistencia-depois.json) | Comparação de produtos e histórico Flyway após reinício |
| `EV10` | [docs/testes/resultados/verificacao-quinta-etapa.md](../testes/resultados/verificacao-quinta-etapa.md) | Relatório técnico consolidado dos 79 testes automatizados e 21 cenários de navegador |
| `EV11` | [docs/testes/evidencias/AV01_antes_confirmacao-carrinho.png](../testes/evidencias/AV01_antes_confirmacao-carrinho.png) | Estado anterior (`7018e1c`) da mensagem de adição ao carrinho |
| `EV12` | [docs/testes/evidencias/AV01_depois_confirmacao-carrinho.png](../testes/evidencias/AV01_depois_confirmacao-carrinho.png) | Reteste de `AV01` após destaque visual, nome do item, botão atualizado e link `"Conferir itens"` |
| `EV13` | [docs/testes/evidencias/AV02_cep-inputmode-360.png](../testes/evidencias/AV02_cep-inputmode-360.png) | Verificação técnica de `inputmode="numeric"` no campo CEP em largura `360px` |
| `EV14` | [docs/testes/evidencias/AV04_antes_checkout-escolha-pagamento.png](../testes/evidencias/AV04_antes_checkout-escolha-pagamento.png) | Estado anterior (`7018e1c`) da escolha de pagamento simulado no checkout |
| `EV15` | [docs/testes/evidencias/AV04_depois_checkout-escolha-pagamento.png](../testes/evidencias/AV04_depois_checkout-escolha-pagamento.png) | Reteste de `AV04`/`AV05` após orientação no Passo 1 e cartões distintos de aprovação/recusa no Passo 2 |
| `EV16` | [docs/testes/evidencias/AV04_antes_resultado-recusa.png](../testes/evidencias/AV04_antes_resultado-recusa.png) | Estado anterior (`7018e1c`) do painel de resultado após recusa simulada |
| `EV17` | [docs/testes/evidencias/AV04_depois_resultado-recusa.png](../testes/evidencias/AV04_depois_resultado-recusa.png) | Reteste de `AV04`/`AV05` com alerta `.result-box-refused` destacado e preservação do carrinho |
| `EV18` | [docs/testes/resultados/fluxo-completo-navegador.json](../testes/resultados/fluxo-completo-navegador.json) | Registro estruturado final do navegador com `"execucao": "concluída"` e 21 cenários aprovados |

---

## 6. Pendências Reais Remanescentes

1. **Validação de `AV02` em dispositivo móvel físico real**: testar o toque no campo CEP (`/paginas/conta/enderecos.html`) em smartphone real (Android/iOS) e registrar o modelo/navegador utilizado, pois o atributo `inputmode="numeric"` e a largura de `360px` em navegador desktop não comprovam o teclado nativo do sistema operacional móvel.
2. **Implantação em hospedagem gratuita HTTPS e smoke test remoto**: publicar a aplicação e o banco PostgreSQL em plano gratuito (conforme proposta técnica no [manual de publicação](../manual/manual-usuario-e-publicacao.md)), carregar de forma controlada as faixas acadêmicas de frete no banco remoto e validar o fluxo completo sob HTTPS.
3. **Conferência documental do arquivo acadêmico externo (DOC)**: confirmar que a transcrição nominal no documento acadêmico de entrega coincide com os 5 relatos preservados neste laudo e no anexo local não versionado (`docs/entrega/anexo-nominal-participantes.local.md`).
