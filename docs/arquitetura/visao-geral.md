# Arquitetura do Sistema — PIT Cupcakes

Visão arquitetural consolidada da aplicação após a conclusão funcional da quinta etapa ([12-SPEC-CONCLUSAO-FUNCIONAL.md](../../specs/12-SPEC-CONCLUSAO-FUNCIONAL.md)).

## 1. Organização em Módulos

A aplicação segue arquitetura monolítica modular em Java 21 e Spring Boot 3.4.2, com separação por domínio funcional em `br.com.pit.cupcakes`:

- `catalogo`: consulta pública (`ProductController`, `ProductService`), administração de produtos (`AdminProductController`, `AdminProductService`), entidade `Product` (`@Version`) e carga opcional `DemoDataLoader`.
- `identidade`: cadastro de clientes (`RegistrationController`, `RegistrationService`), sessão e CSRF (`AuthenticationController`), provisionamento seguro de administrador (`AdminProvisioningService`, `AdminProvisioningRunner`) e `User`.
- `endereco` e `frete`: CRUD de endereços por proprietário (`AddressController`, `AddressService`), cliente ViaCEP com timeout curto (`ViaCepClient`), cotação de entrega/retirada (`ShippingController`, `ShippingService`, `ShippingRange`) e faixas de demonstração (`DemoShippingRangeLoader`).
- `carrinho`: carrinho em `HttpSession` (`CartController`, `CartService`), sincronizado por sessão, armazenando apenas `produtoId -> quantidade` e recalculando preços e disponibilidade a cada leitura ([carrinho-em-sessao.md](carrinho-em-sessao.md)).
- `pedido`: revisão de checkout (`CheckoutController`, `CheckoutService`, `CheckoutRevision`), confirmação transacional e idempotência (`OrderController`, `OrderService`), atendimento administrativo (`AdminOrderController`) e entidades históricas (`Order`, `OrderItem`, `OrderAddress`, `SimulatedPayment`).
- `config` e `compartilhado`: segurança HTTP (`SecurityConfig`, `LoginAttemptGuard`) e tratamento padronizado de erros (`GlobalExceptionHandler`, `ErrorResponse`).

## 2. Integridade Transacional, Concorrência e Idempotência

1. **Cálculo exclusivo no servidor**: nenhum preço unitário, subtotal, taxa de frete, total ou proprietário enviado pelo navegador é aceito nos DTOs de entrada (`CheckoutRevisionRequest`, `ConfirmOrderRequest`). Campos JSON desconhecidos retornam `400 DADOS_INVALIDOS`.
2. **Revisão vinculada à sessão (`revisaoId`)**: `POST /api/conta/checkout/revisao` valida o carrinho e o endereço/frete, gerando um `CheckoutRevision` em sessão com validade de 15 minutos e fotografia dos itens, preços, versão do endereço e valor do frete.
3. **Lock pessimista na confirmação**: `CheckoutService` executa a gravação dentro de `TransactionTemplate` adquirindo locks pessimistas (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`) nos produtos em ordem crescente de ID (`findAllByIdInOrderByIdAscForUpdate`), no endereço do cliente (`findByIdAndOwnerIdForUpdate`) e na faixa de frete ativa (`matchingForUpdate`), prevenindo condições de corrida e deadlocks.
4. **Idempotência e recuperação de resposta perdida**: a tabela `pedido` possui restrição única `uk_pedido_usuario_revisao (usuario_id, revisao_id)`. Se duas requisições concorrentes ou uma repetição após perda de resposta usarem o mesmo `revisaoId` com o mesmo `resultadoSimulado`, o servidor retorna `200 OK` com o pedido já gravado (mesmo após o carrinho ter sido esvaziado). Se o `resultadoSimulado` divergir do pedido já gravado, retorna `409 CONFIRMACAO_DIVERGENTE`.
5. **Limpeza pós-commit do carrinho**: o carrinho em sessão e o atributo de revisão ativa só são limpos **após** o commit bem-sucedido de um pedido `APROVADO`. Em caso de falha de persistência (rollback) ou pagamento `RECUSADO`, o carrinho permanece intacto para nova tentativa.
6. **Preservação histórica**: `item_pedido` copia nome e preço unitário do produto no momento da compra; `endereco_pedido` copia todos os campos do endereço. Alterações posteriores no catálogo ou exclusão do endereço pelo cliente não alteram pedidos concluídos.

## 3. Segurança e Controle de Acesso

- **Perfis**: `CLIENTE` (criado no cadastro público) e `ADMIN` (provisionado de forma controlada por variáveis de ambiente na inicialização, sem promoção automática de contas `CLIENTE` nem sobrescrita silenciosa de senha).
- **Autorização**: `/api/admin/**` exige `ROLE_ADMIN` (`401` para visitante, `403` para `CLIENTE`); `/api/conta/**` exige sessão autenticada (`401` para visitante).
- **CSRF e Sessão**: mutações (`POST`, `PUT`, `DELETE`) exigem token CSRF; cookies de sessão usam `HttpOnly=true`, `SameSite=Lax` e `Secure=true` no perfil `prod`.
- **Proteção contra abuso de login**: `LoginAttemptGuard` limita tentativas falhas consecutivas por combinação `IP + email normalizado` (padrão: 10 tentativas em janela de 300 segundos, retornando `429 MUITAS_TENTATIVAS_LOGIN`).

## 4. Limitações Conhecidas e Escopo Acadêmico

- **Pagamento simulado**: não há integração com adquirentes, PIX ou cartões reais; a interface e os registros informam explicitamente que a operação é acadêmica.
- **Faixas de frete acadêmicas**: as faixas do perfil `demo` (`70000000`–`70999999` e `71000000`–`71999999`) destinam-se à demonstração em Brasília–DF; cobertura comercial real requer carga controlada via script SQL.
- **Sessão em instância única**: o carrinho em `HttpSession` e o `LoginAttemptGuard` em memória atendem execução em instância única; ambientes distribuídos exigiriam armazenamento compartilhado (ex.: Redis/Spring Session).

