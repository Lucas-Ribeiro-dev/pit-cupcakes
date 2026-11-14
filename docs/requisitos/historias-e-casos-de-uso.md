# Histórias de usuário e casos de uso consolidados

Registro consolidado das histórias de usuário e dos casos de uso essenciais implementados na loja virtual **PIT Cupcakes** (Etapas 1 a 5), preservando os identificadores originais das especificações técnicas em [specs](../../specs/) e dos artefatos de evidência em [docs/testes/evidencias](../testes/evidencias/).

## 1. Histórias de usuário implementadas e mapeamento das SPECs

> **Nota sobre preservação de IDs e agrupamento**: Os identificadores `HU01` a `HU08`, `HU17` e `HU18` são preservados exatamente como definidos nas especificações (`07-SPEC-CATALOGO`, `09-SPEC-AUTENTICACAO`, `10-SPEC-ENDERECOS-FRETE` e `11-SPEC-CARRINHO`) e nos nomes das evidências (`HU01_*`, `HU02_*`, `HU03_*`, `HU04_*`, `HU05_*`, `HU06_*`, `HU07_*`, `ADR01_*`–`ADR09_*`, `FRT04_*`). As histórias da quinta etapa ([12-SPEC-CONCLUSAO-FUNCIONAL](../../specs/12-SPEC-CONCLUSAO-FUNCIONAL.md), correspondentes à faixa intermediária `HU09`–`HU16` do escopo geral) estão organizadas nos quatro blocos funcionais abaixo e vinculadas aos prefixos de verificação `CHK`, `PED` e `ADM`.

| ID Original / Bloco | SPEC de referência | Ator | História de usuário | Critérios essenciais verificados no código e evidências |
| --- | --- | --- | --- | --- |
| **HU01** | [07-SPEC-CATALOGO](../../specs/07-SPEC-CATALOGO.md) | Visitante / Cliente | Visualizar o catálogo de cupcakes disponíveis e indisponíveis | Ordenação crescente por ID, preços formatados em BRL a partir do servidor, selo de indisponibilidade, tratamento de catálogo vazio e falha de rede recuperável (`HU01_TC01_*`, `HU01_TC07_*`, `HU01_TC09_*`). |
| **HU02** | [07-SPEC-CATALOGO](../../specs/07-SPEC-CATALOGO.md) | Visitante / Cliente | Consultar detalhes de um cupcake específico | Exibição de descrição, preço, disponibilidade, imagem estática local ou substituta, validação de ID inválido/inexistente (`400`/`404`) e botão de adição ao carrinho quando disponível (`HU02_TC03_*` a `HU02_TC09_*`). |
| **HU03** | [09-SPEC-AUTENTICACAO](../../specs/09-SPEC-AUTENTICACAO.md) | Visitante | Criar conta de cliente | Validação de nome (`2–120` caracteres), e-mail normalizado único (`Locale.ROOT`), senha literal de `8–64` pontos de código (máx. `72` bytes UTF-8) com `{bcrypt}`, perfil fixo `CLIENTE` e rejeição de campos extras (`HU03_AUTH01_*`, `HU03_AUTH12_*`). |
| **HU04** | [09-SPEC-AUTENTICACAO](../../specs/09-SPEC-AUTENTICACAO.md) | Cliente / Admin | Entrar na conta e manter sessão autenticada | Login via formulário com renovação de ID da `HttpSession`, cookie `HttpOnly` e `SameSite=Lax` (`Secure` em `prod`), proteção CSRF, preservação do carrinho visitante e limite de tentativas (`LoginAttemptGuard`, `429`) (`HU04_AUTH04_*` a `HU04_AUTH12_*`). |
| **HU05** | [09-SPEC-AUTENTICACAO](../../specs/09-SPEC-AUTENTICACAO.md) | Cliente / Admin | Encerrar sessão com segurança (logout) | `POST /api/autenticacao/logout` protegido por CSRF invalida a `HttpSession`, remove o cookie `JSESSIONID` e descarta o carrinho temporário da sessão encerrada (`HU05_AUTH08_*`). |
| **HU06** | [11-SPEC-CARRINHO](../../specs/11-SPEC-CARRINHO.md) | Visitante / Cliente | Adicionar cupcakes ao carrinho e consultar itens e subtotal na sessão | Inclusão com soma em produto repetido, faixa de confirmação `"Produto adicionado ao carrinho."` com link `"Conferir itens"`, limite de `1–99` unidades por item e `50` produtos distintos, e recálculo no servidor (`HU06_CRT01_*`, `HU06_CRT13_*`). |
| **HU07** | [11-SPEC-CARRINHO](../../specs/11-SPEC-CARRINHO.md) | Visitante / Cliente | Alterar quantidades e remover itens do carrinho | Substituição de quantidade (`PUT`), remoção idempotente (`DELETE`), marcação de linhas bloqueadas quando produto fica indisponível/removido sem incluí-las no subtotal (`HU07_CRT03_*`). |
| **HU08** | [10-SPEC-ENDERECOS-FRETE](../../specs/10-SPEC-ENDERECOS-FRETE.md) / [12-SPEC-CONCLUSAO-FUNCIONAL](../../specs/12-SPEC-CONCLUSAO-FUNCIONAL.md) | Cliente | Consultar frete por faixa de CEP (`ENTREGA`) ou `RETIRADA` e revisar valores no checkout | Cálculo exclusivo no servidor (`RETIRADA = 0.00`; `ENTREGA` para UF `DF` em faixa ativa sem sobreposição GiST), geração de `CheckoutRevision` (`revisaoId`) válida por 10 minutos com `subtotal`, `frete` e `total` (`FRT04_*`, `CHK01_*`, `CHK02_*`). |
| **HU17** | [10-SPEC-ENDERECOS-FRETE](../../specs/10-SPEC-ENDERECOS-FRETE.md) | Cliente | Cadastrar e listar endereços de entrega vinculados à própria conta com auxílio opcional do ViaCEP | Propriedade estrita pelo usuário autenticado da sessão, consulta opcional ao ViaCEP com timeout curto e fallback manual completo sem bloquear o salvamento (`ADR01_*`, `ADR05_*`, `ADR06_*`, `ADR07_*`, `ADR09_*`). |
| **HU18** | [10-SPEC-ENDERECOS-FRETE](../../specs/10-SPEC-ENDERECOS-FRETE.md) | Cliente | Editar e remover endereços próprios com controle otimista de versão | Exigência de `versao` (`@Version`) em `PUT` e `DELETE`, retornando `409 ENDERECO_DESATUALIZADO` sem perda do texto em edição, e isolamento contra acesso alheio (`404`) (`ADR04_*`). |
| **Bloco Checkout / Pagamento Simulado** *(agrupa `HU09`–`HU10` / `CHK01`–`CHK04`)* | [12-SPEC-CONCLUSAO-FUNCIONAL](../../specs/12-SPEC-CONCLUSAO-FUNCIONAL.md) | Cliente | Confirmar pedido a partir da revisão ativa escolhendo pagamento acadêmico simulado (`APROVADO` ou `RECUSADO`) | Lock pessimista (`FOR UPDATE`) ordenado em produtos, endereço e faixa; gravação atômica de `pedido`, `item_pedido`, `endereco_pedido` e `pagamento_simulado`; idempotência por `(usuario_id, revisao_id)`; limpeza do carrinho apenas após commit `APROVADO` (`CHK03_*`, `CHK04_*`). |
| **Bloco Meus Pedidos** *(agrupa `HU11`–`HU12` / `PED01`–`PED03`)* | [12-SPEC-CONCLUSAO-FUNCIONAL](../../specs/12-SPEC-CONCLUSAO-FUNCIONAL.md) | Cliente | Consultar histórico, detalhes congelados e andamento dos próprios pedidos | Listagem em ordem decrescente de criação; preservação histórica de nomes/preços dos itens e endereço da compra mesmo após edição no catálogo ou exclusão do endereço; bloqueio a pedidos de terceiros (`404`) (`PED01_*`, `PED02_*`, `PED03_*`). |
| **Bloco Admin Catálogo** *(agrupa `HU13`–`HU14` / `ADM01`–`ADM02`)* | [12-SPEC-CONCLUSAO-FUNCIONAL](../../specs/12-SPEC-CONCLUSAO-FUNCIONAL.md) | Administrador | Cadastrar e editar produtos do catálogo com controle de acesso e versão | Restrição a `ROLE_ADMIN` (`401` visitante, `403` cliente), validação de preço (`0.01` a `99999999.99`), caminhos de imagem restritos a `/assets/img/*.svg` e concorrência otimista (`409 PRODUTO_DESATUALIZADO`) (`ADM01_*`, `ADM02_*`). |
| **Bloco Admin Atendimento** *(agrupa `HU15`–`HU16` / `ADM03`)* | [12-SPEC-CONCLUSAO-FUNCIONAL](../../specs/12-SPEC-CONCLUSAO-FUNCIONAL.md) | Administrador | Atender pedidos e atualizar status de atendimento conforme modalidade | Transições controladas por modalidade (`ENTREGA`: `SAIU_PARA_ENTREGA`; `RETIRADA`: `PRONTO_PARA_RETIRADA`), bloqueio de pedidos `NAO_AUTORIZADO`, regras de cancelamento e estados finais (`CONCLUIDO`, `CANCELADO`) com `@Version` (`ADM03_*`). |

---

## 2. Casos de uso essenciais — Fluxos principais e alternativos

As descrições curtas abaixo complementam o diagrama de casos de uso em [diagramas-uml.md](../modelagem/diagramas-uml.md).

### UC1 — Consultar catálogo e detalhes (`HU01`, `HU02`)
- **Ator**: Visitante ou Cliente Autenticado.
- **Fluxo principal**: O usuário acessa `/index.html`, visualiza os cupcakes ordenados por ID com preço e disponibilidade vindos de `GET /api/produtos` e abre `/produto.html?id=N` (`GET /api/produtos/{id}`) para consultar descrição completa e imagem.
- **Fluxos alternativos / exceções**:
  - *A1 — Produto indisponível ou sem imagem*: exibe selo de indisponível, desabilita adição ao carrinho e aplica imagem substituta padrão (`/assets/img/cupcake-tradicional.svg`).
  - *A2 — ID inválido ou inexistente*: IDs não numéricos/não positivos são barrados no cliente e retornam `400 ID_INVALIDO` na API; produto ausente retorna `404 PRODUTO_NAO_ENCONTRADO` com opção de voltar ao catálogo.
  - *A3 — Falha de rede*: exibe mensagem de erro distinta de catálogo vazio e botão **Tentar novamente**.

### UC2 — Gerenciar carrinho em sessão (`HU06`, `HU07`)
- **Ator**: Visitante ou Cliente Autenticado.
- **Fluxo principal**: O usuário adiciona um cupcake disponível (`POST /api/carrinho/itens`), recebe confirmação visual `"Produto adicionado ao carrinho."` com link `"Conferir itens"`, consulta `/paginas/compra/carrinho.html` (`GET /api/carrinho`), ajusta quantidades (`PUT`) ou remove linhas (`DELETE`) com subtotal recalculado no servidor.
- **Fluxos alternativos / exceções**:
  - *A1 — Limites excedidos*: quantidades fora de `1..99` retornam `400 QUANTIDADE_INVALIDA`; mais de `50` produtos distintos na sessão retornam `400 LIMITE_CARRINHO_ATINGIDO` sem alteração parcial.
  - *A2 — Produto tornou-se indisponível ou foi removido*: o servidor marca a linha como bloqueada (`PRODUTO_INDISPONIVEL` ou `PRODUTO_NAO_ENCONTRADO`), exclui seu valor do subtotal e exige sua remoção antes do checkout.
  - *A3 — Erro de rede na inclusão*: o cliente não repete `POST` automaticamente para evitar soma duplicada e orienta a conferência do carrinho.

### UC3 — Cadastrar conta, autenticar e encerrar sessão (`HU03`, `HU04`, `HU05`)
- **Ator**: Visitante, Cliente Autenticado ou Administrador.
- **Fluxo principal**: O visitante cria conta em `/paginas/conta/cadastro.html` (`POST /api/usuarios`), entra em `/paginas/conta/login.html` (`POST /api/autenticacao/login`) renovando o ID da sessão e preservando o carrinho, e encerra a sessão com `POST /api/autenticacao/logout`.
- **Fluxos alternativos / exceções**:
  - *A1 — E-mail já cadastrado*: `POST /api/usuarios` retorna `409 EMAIL_JA_CADASTRADO` (inclusive em corrida concorrente via `uk_usuario_email_normalizado`).
  - *A2 — Credenciais inválidas ou excesso de tentativas*: login inválido retorna `401 CREDENCIAIS_INVALIDAS` com mensagem genérica; mais de 10 falhas na janela de 300s por `IP + email` retornam `429 MUITAS_TENTATIVAS_LOGIN`.

### UC4 — Gerenciar endereços e cotar frete (`HU17`, `HU18`, `HU08`)
- **Ator**: Cliente Autenticado.
- **Fluxo principal**: Em `/paginas/conta/enderecos.html`, o cliente informa o CEP (com auxílio opcional de `GET /api/conta/cep/{cep}` via ViaCEP), revisa/preenche os campos, salva o endereço (`POST /api/conta/enderecos`), edita/remove com controle de `versao` e consulta o frete (`POST /api/conta/frete/cotacao`).
- **Fluxos alternativos / exceções**:
  - *A1 — Falha ou lentidão do ViaCEP*: erros `404`/`502`/`504` são informados na tela sem limpar campos já editados e mantendo o preenchimento manual habilitado.
  - *A2 — Conflito de versão*: `PUT` ou `DELETE` com `versao` antiga retorna `409 ENDERECO_DESATUALIZADO`, preservando o texto digitado no formulário.
  - *A3 — Endereço fora da área ou entrega sem faixas*: retorna `422 FORA_DA_AREA_DE_ENTREGA` ou `422 ENTREGA_NAO_CONFIGURADA` e sugere retirada gratuita sem selecioná-la automaticamente.

### UC5 — Revisar checkout e simular pagamento (`HU08`, `CHK01`–`CHK04`)
- **Ator**: Cliente Autenticado.
- **Fluxo principal**: Em `/paginas/compra/checkout.html`, o cliente escolhe `RETIRADA` ou `ENTREGA` (selecionando endereço salvo), gera a revisão (`POST /api/conta/checkout/revisao`) com validade de 10 minutos e confirma o pedido (`POST /api/conta/pedidos`) escolhendo `APROVADO` ou `RECUSADO`.
- **Fluxos alternativos / exceções**:
  - *A1 — Pagamento simulado recusado (`RECUSADO`)*: grava o pedido com status `NAO_AUTORIZADO` e pagamento `RECUSADO` para registro histórico, mantendo o carrinho intacto para nova revisão.
  - *A2 — Mudança de preço, disponibilidade, endereço ou frete após a revisão*: a confirmação detecta a divergência sob lock pessimista e retorna `409 REVISAO_DESATUALIZADA` (ou `422`/`409` específico) sem gravar pedido parcial.
  - *A3 — Reenvio por perda de resposta ou clique duplo*: `(usuario_id, revisao_id)` garante idempotência, retornando `200 OK` com o pedido já criado se o `resultadoSimulado` for o mesmo, ou `409 CONFIRMACAO_DIVERGENTE` se divergir.

### UC6 — Consultar próprios pedidos (`PED01`–`PED03`)
- **Ator**: Cliente Autenticado.
- **Fluxo principal**: O cliente acessa `/paginas/conta/pedidos.html` (`GET /api/conta/pedidos`) para listar seus pedidos do mais recente ao mais antigo e abre `/paginas/conta/pedido.html?id=N` (`GET /api/conta/pedidos/{id}`) para ver itens históricos, endereço congelado, pagamento simulado e status de atendimento.
- **Fluxos alternativos / exceções**:
  - *A1 — Pedido inexistente ou pertencente a outro cliente*: o servidor filtra por `id` e `usuario_id` da sessão, retornando `404 PEDIDO_NAO_ENCONTRADO` sem revelar a existência de pedidos de terceiros.

### UC7 — Cadastrar e editar produtos (`ADM01`, `ADM02`)
- **Ator**: Administrador (`ROLE_ADMIN`).
- **Fluxo principal**: Em `/admin/produtos.html`, o administrador lista todos os produtos (`GET /api/admin/produtos`), cadastra novos cupcakes (`POST /api/admin/produtos`) e edita nome, descrição, preço, imagem local (`/assets/img/*.svg`) e disponibilidade (`PUT /api/admin/produtos/{id}`) enviando a `versao` atual.
- **Fluxos alternativos / exceções**:
  - *A1 — Acesso sem permissão*: visitante recebe `401 NAO_AUTENTICADO`; usuário `CLIENTE` recebe `403 ACESSO_NEGADO`.
  - *A2 — Conflito de edição concorrente*: se outro administrador alterou o produto, retorna `409 PRODUTO_DESATUALIZADO` e preserva os dados em tela.

### UC8 — Atender pedidos e atualizar status (`ADM03`)
- **Ator**: Administrador (`ROLE_ADMIN`).
- **Fluxo principal**: Em `/admin/pedidos.html`, o administrador consulta os pedidos (`GET /api/admin/pedidos` e `GET /api/admin/pedidos/{id}`) e avança o atendimento (`PUT /api/admin/pedidos/{id}/status`) conforme a modalidade (`ENTREGA`: `RECEBIDO` → `EM_PREPARACAO` → `SAIU_PARA_ENTREGA` → `CONCLUIDO`; `RETIRADA`: `RECEBIDO` → `EM_PREPARACAO` → `PRONTO_PARA_RETIRADA` → `CONCLUIDO`).
- **Fluxos alternativos / exceções**:
  - *A1 — Cancelamento administrativo*: permitido em `RECEBIDO`/`EM_PREPARACAO` (e em `PRONTO_PARA_RETIRADA` na modalidade retirada), bloqueado após `SAIU_PARA_ENTREGA` ou `CONCLUIDO`.
  - *A2 — Transição incompatível ou pedido não autorizado*: tentativas de avançar pedidos `NAO_AUTORIZADO`, `CANCELADO` ou `CONCLUIDO`, ou usar status de entrega em pedido de retirada, retornam `409 TRANSICAO_STATUS_INVALIDA`.
