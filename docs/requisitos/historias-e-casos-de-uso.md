# Histórias de usuário e casos de uso consolidados

Registro enxuto das histórias de usuário implementadas na loja virtual PIT Cupcakes (Etapas 1 a 5), coerente com as especificações técnicas em [specs](../../specs/).

## Histórias de usuário implementadas

| ID | Ator | História | Critérios essenciais verificados no código |
| --- | --- | --- | --- |
| **HU01** | Visitante / Cliente | Visualizar o catálogo de cupcakes disponíveis e indisponíveis | Ordenação por ID, preços formatados no servidor, selo de indisponibilidade e tratamento de catálogo vazio. |
| **HU02** | Visitante / Cliente | Consultar detalhes de um cupcake específico | Exibição de descrição, preço, disponibilidade, imagem local ou substituta e ação de adição ao carrinho quando disponível. |
| **HU03** | Visitante | Criar conta de cliente | Validação de nome, e-mail normalizado único (`Locale.ROOT`), senha literal de 8–64 pontos de código (máx. 72 bytes UTF-8) com `{bcrypt}` e perfil `CLIENTE` fixo. |
| **HU04** | Cliente / Admin | Entrar na conta e manter sessão autenticada | Sessão HTTP com renovação de ID no login, cookie `HttpOnly` e `SameSite=Lax`, proteção CSRF e limite de tentativas consecutivas (`LoginAttemptGuard`). |
| **HU05** | Cliente / Admin | Encerrar sessão com segurança | `POST /api/autenticacao/logout` com CSRF invalida a sessão, apaga o cookie `JSESSIONID` e limpa o carrinho temporário. |
| **HU06** | Cliente | Cadastrar, revisar, editar e remover endereços de entrega | Propriedade estrita por `usuario_id`, bloqueio otimista por `versao`, apoio opcional do ViaCEP sem bloquear preenchimento manual. |
| **HU07** | Cliente | Consultar frete por endereço salvo ou retirada na loja | Cálculo exclusivo no servidor (`RETIRADA = 0.00`; `ENTREGA` para UF `DF` conforme faixa ativa em `faixa_frete`). |
| **HU08** | Visitante / Cliente | Montar e revisar carrinho de compras na sessão | Limite de 1–99 unidades por item e 50 produtos distintos; preservação no login; bloqueio automático de itens indisponíveis ou removidos. |
| **HU09** | Cliente | Revisar checkout com endereço ou retirada e totais do servidor | Exige sessão autenticada e carrinho válido sem linhas bloqueadas; gera `revisaoId` vinculado à fotografia de itens, modalidade, endereço e frete. |
| **HU10** | Cliente | Confirmar pedido com pagamento acadêmico simulado (`APROVADO` ou `RECUSADO`) | Persistência transacional com lock pessimista (`FOR UPDATE`), idempotência por `(usuario_id, revisao_id)`, fotografia histórica em `item_pedido` e `endereco_pedido`, e limpeza do carrinho apenas após commit de pedido aprovado. |
| **HU11** | Cliente | Consultar histórico e detalhes dos próprios pedidos | Listagem em ordem decrescente de criação; exibição de itens históricos, endereço congelado, pagamento simulado e status de atendimento; bloqueio de pedidos alheios (`404`). |
| **HU12** | Administrador | Cadastrar e editar produtos do catálogo | Acesso exclusivo `ROLE_ADMIN`; validação de preço (`0.01` a `99999999.99`), imagem restrita a `/assets/img/*.svg` e controle de concorrência por `versao`. |
| **HU13** | Administrador | Atender pedidos e atualizar status de atendimento | Fluxos separados para `ENTREGA` e `RETIRADA`, bloqueio de pedidos recusados (`NAO_AUTORIZADO`), regras de cancelamento e estados finais (`CONCLUIDO`, `CANCELADO`). |

