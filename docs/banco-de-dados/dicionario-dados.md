# Dicionário de dados — catálogo, identidade, endereços, frete e pedidos

Especificação da camada de persistência para a loja virtual PIT Cupcakes (migrações Flyway V1 a V4). Nenhuma migração anterior foi alterada.

## 1. Tabela: `produto`

Armazena as informações dos produtos (cupcakes) exibidos na loja virtual.

| Coluna | Tipo SQL | Nulo? | Padrão | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` / `BIGINT` | Não | Gerado automaticamente | Identificador primário único do produto |
| `nome` | `VARCHAR(150)` | Não | - | Nome descritivo comercial do cupcake |
| `descricao` | `TEXT` | Não | - | Descrição textual detalhada dos ingredientes e sabor |
| `preco` | `NUMERIC(10, 2)` | Não | - | Preço unitário de venda em reais (BRL), com precisão de duas casas decimais |
| `imagem_caminho` | `VARCHAR(255)` | Sim | `NULL` | Caminho relativo do arquivo de imagem do produto no servidor estático |
| `disponivel` | `BOOLEAN` | Não | `TRUE` | Indicador de disponibilidade para exibição e venda |
| `versao` | `BIGINT` | Não | `0` | Controle de concorrência otimista (JPA `@Version`) |

## 2. Restrições e Índices

- **Chave Primária**: `PRIMARY KEY (id)`
- **Restrição de Checagem**: `CONSTRAINT chk_produto_preco_positivo CHECK (preco > 0)` — Garante que nenhum produto seja cadastrado ou atualizado com valor zero ou negativo.
- **Índice Secundário**: `idx_produto_disponivel` sobre a coluna `disponivel` — Otimiza consultas filtradas por produtos disponíveis.

## 3. Script Flyway

A criação da tabela é versionada através do arquivo de migração:
[V1__criar_tabela_produto.sql](../../src/main/resources/db/migration/V1__criar_tabela_produto.sql)

V1 e a persistência do catálogo em `cupcakes` foram aprovadas pelo responsável. Os snapshots [antes](../testes/resultados/persistencia-antes.json) e [depois](../testes/resultados/persistencia-depois.json) têm quatro produtos e histórico Flyway idênticos. A comparação foi incorporada sem repetir o teste.

## 4. Tabela `usuario`

Definida na nova [V2](../../src/main/resources/db/migration/V2__criar_tabela_usuario.sql); V1 permanece intacta. A aplicação real de V2 e as restrições de identidade foram aprovadas nos relatórios PostgreSQL registrados na [segunda etapa](../testes/resultados/verificacao-segunda-etapa.md).

| Coluna | Tipo SQL | Nulo? | Padrão | Descrição |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | Não | Identidade | Chave primária |
| `nome` | `VARCHAR(120)` | Não | — | Nome sem espaços nas extremidades, de 2 a 120 caracteres validado pelo serviço |
| `email_normalizado` | `VARCHAR(254)` | Não | — | E-mail sem espaços nas extremidades e em minúsculas com Locale.ROOT |
| `senha_hash` | `VARCHAR(255)` | Não | — | Hash BCrypt adaptativo com prefixo `{bcrypt}`; nunca devolvido pela API |
| `perfil` | `VARCHAR(20)` | Não | — | CLIENTE ou ADMIN; cadastro público sempre define CLIENTE |
| `criado_em` | `TIMESTAMPTZ` | Não | `CURRENT_TIMESTAMP` | Instante de criação; entidade fornece Instant na inserção |

- `uk_usuario_email_normalizado`: UNIQUE cria o índice único de e-mail. O serviço verifica duplicação e converte a violação concorrente dessa restrição em HTTP 409.
- `chk_usuario_perfil`: CHECK limita o perfil a CLIENTE/ADMIN. Nenhum administrador ou usuário demo é criado pela migração.
- Cadastro é transacional. A senha literal tem de 8 a 64 pontos de código e no máximo 72 bytes UTF-8; não é normalizada.
- Sessões são mantidas pelo contêiner, fora desta tabela; sua persistência entre reinícios não é requisito. Não há tabela de carrinho (mantido em `HttpSession`); as tabelas de pedidos e pagamento simulado estão definidas na seção 7 (`V4`).

## 5. Tabela `endereco`

Nova [V3](../../src/main/resources/db/migration/V3__criar_endereco_faixa_frete.sql), sem alteração de V1/V2. Vários endereços por usuário, sem padrão automático.

| Coluna | Tipo SQL | Nulo? | Descrição |
| --- | --- | --- | --- |
| id | BIGINT GENERATED ALWAYS AS IDENTITY | Não | Chave primária |
| usuario_id | BIGINT | Não | FK usuario(id); exclusão de usuário referenciado é restringida |
| destinatario | VARCHAR(120) | Não | Nome do destinatário |
| cep | VARCHAR(8) | Não | Oito dígitos ASCII, preservando zeros iniciais |
| logradouro | VARCHAR(160) | Não | Logradouro revisável |
| numero | VARCHAR(20) | Não | Texto; aceita S/N |
| complemento | VARCHAR(120) | Sim | Informação adicional |
| bairro | VARCHAR(100) | Não | Bairro |
| cidade | VARCHAR(100) | Não | Cidade ou região administrativa |
| uf | VARCHAR(2) | Não | UF brasileira em maiúsculas |
| versao | BIGINT, DEFAULT 0 | Não | Versão não negativa, JPA @Version |

Banco reforça tamanho, obrigatoriedade, textos não vazios, formato do CEP, UF e FK. Serviço apara textos e valida limites por pontos de código. Índice `idx_endereco_usuario_id(usuario_id,id)` atende à propriedade e ordenação. Leitura, alteração e exclusão filtram ID e proprietário da sessão; API não expõe usuário. PUT exige versão; DELETE exige query versao. Escritas concorrentes usam o predicado de versão JPA, além da comparação inicial, e retornam 409 em conflito.

## 6. Tabela `faixa_frete`

| Coluna | Tipo SQL | Nulo? | Descrição |
| --- | --- | --- | --- |
| id | BIGINT GENERATED ALWAYS AS IDENTITY | Não | Chave primária |
| cep_inicio | VARCHAR(8) | Não | Limite inferior inclusivo, oito dígitos |
| cep_fim | VARCHAR(8) | Não | Limite superior inclusivo, oito dígitos |
| valor | NUMERIC(12,2) | Não | Taxa não negativa |
| ativa | BOOLEAN, DEFAULT FALSE | Não | Habilita faixa na cotação |

CHECK exige início <= fim. Restrição EXCLUDE GiST sobre `int8range(cep_inicio::bigint,cep_fim::bigint,'[]')`, parcial para faixas ativas, impede interseções inclusive entre transações concorrentes. Usa suporte nativo a intervalos; não exige extensão btree_gist. Faixas inativas podem se sobrepor. Comparação textual de CEPs funciona porque todos têm oito dígitos.

A migração não insere faixas. Todas as faixas valem somente para endereços com UF DF; não há filtro pelo nome da cidade. RETIRADA retorna 0.00 sem endereço. ENTREGA exige endereço próprio e uma única faixa ativa; sem faixas retorna ENTREGA_NAO_CONFIGURADA, sem correspondência retorna FORA_DA_AREA_DE_ENTREGA. Valores são strings com duas casas na API. Cotação futura deve ser recalculada.

Configuração de demonstração e comercial de Brasília–DF: no perfil `demo`, `DemoShippingRangeLoader` registra de forma idempotente duas faixas acadêmicas de demonstração quando não há faixas ativas (`70000000`–`70999999` a `8.00` e `71000000`–`71999999` a `12.00`). Para configuração manual fora do perfil `demo`, o [script controlado](configurar-frete.sql) aceita variáveis psql `startPostalCode`, `endPostalCode` e `amount` em transação validada pelas restrições do banco. Fixtures dos testes automatizados são exclusivas de `cupcakes_test`.

## 7. Tabelas de Pedidos e Pagamento Simulado (`V4`)

Definidas na migração [V4__criar_pedidos_e_pagamento.sql](../../src/main/resources/db/migration/V4__criar_pedidos_e_pagamento.sql), preservando integralmente V1, V2 e V3. O carrinho permanece em sessão (`HttpSession`) até a confirmação transacional do pedido.

### 7.1 Tabela `pedido`

Armazena o cabeçalho do pedido, valores consolidados no servidor, estado do atendimento e chave de idempotência da revisão (`Order`).

| Coluna | Tipo SQL | Nulo? | Padrão | Descrição |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | Não | Identidade | Chave primária do pedido (`PRIMARY KEY`) |
| `usuario_id` | `BIGINT` | Não | — | FK `REFERENCES usuario(id)` (restrição padrão `NO ACTION` / `RESTRICT`) |
| `revisao_id` | `VARCHAR(64)` | Não | — | Identificador da revisão confirmada (`CHECK (btrim(revisao_id) <> '')`) |
| `modalidade` | `VARCHAR(20)` | Não | — | `ENTREGA` ou `RETIRADA` (`CHECK (modalidade IN ('ENTREGA', 'RETIRADA'))`) |
| `status_atendimento` | `VARCHAR(30)` | Não | — | `RECEBIDO`, `EM_PREPARACAO`, `PRONTO_PARA_RETIRADA`, `SAIU_PARA_ENTREGA`, `CONCLUIDO`, `CANCELADO` ou `NAO_AUTORIZADO` |
| `subtotal` | `NUMERIC(12, 2)` | Não | — | Soma dos subtotais dos itens (`CHECK (subtotal >= 0)`) |
| `frete` | `NUMERIC(12, 2)` | Não | — | Valor de entrega (`CHECK (frete >= 0)`) |
| `total` | `NUMERIC(12, 2)` | Não | — | Soma exata `subtotal + frete` (`CHECK (total >= 0)`) |
| `criado_em` | `TIMESTAMPTZ` | Não | `CURRENT_TIMESTAMP` | Instante de criação do pedido |
| `versao` | `BIGINT` | Não | `0` | Controle de concorrência otimista (`CHECK (versao >= 0)`, JPA `@Version`) |

- **Idempotência**: `CONSTRAINT uk_pedido_usuario_revisao UNIQUE (usuario_id, revisao_id)` impede pedidos duplicados para a mesma revisão em cliques repetidos, requisições concorrentes ou reenvio após perda de resposta.
- **Soma exata do total**: `CONSTRAINT chk_pedido_total_soma CHECK (total = subtotal + frete)`.
- **Frete zero na retirada**: `CONSTRAINT chk_pedido_retirada_frete_zero CHECK (modalidade <> 'RETIRADA' OR frete = 0)`.
- **Coerência modalidade × atendimento**: `CONSTRAINT chk_pedido_status_modalidade` impede `PRONTO_PARA_RETIRADA` em pedidos de `ENTREGA` e impede `SAIU_PARA_ENTREGA` em pedidos de `RETIRADA`. A coerência com o pagamento simulado (`APROVADO` inicia em `RECEBIDO`; `RECUSADO` permanece em `NAO_AUTORIZADO`) é garantida transacionalmente em `CheckoutService` e `OrderService`.
- **Índices**: `idx_pedido_usuario_criado_em (usuario_id, criado_em DESC, id DESC)` e `idx_pedido_criado_em (criado_em DESC, id DESC)`.

### 7.2 Tabela `item_pedido`

Preserva a fotografia histórica dos produtos comprados no momento da confirmação (`OrderItem`), independente de edições posteriores no catálogo.

| Coluna | Tipo SQL | Nulo? | Descrição |
| --- | --- | --- | --- |
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | Não | Chave primária (`PRIMARY KEY`) |
| `pedido_id` | `BIGINT` | Não | FK `REFERENCES pedido(id)` |
| `produto_id` | `BIGINT` | Não | FK `REFERENCES produto(id)` |
| `nome_produto` | `VARCHAR(150)` | Não | Nome do produto congelado no momento da compra (`CHECK (btrim(nome_produto) <> '')`) |
| `preco_unitario` | `NUMERIC(12, 2)` | Não | Preço unitário congelado no momento da compra (`CHECK (preco_unitario > 0)`) |
| `quantidade` | `INTEGER` | Não | Quantidade adquirida (`CHECK (quantidade BETWEEN 1 AND 99)`) |

- **Restrição e índice**: `CONSTRAINT uk_item_pedido_produto UNIQUE (pedido_id, produto_id)` e `idx_item_pedido_produto_id (produto_id)`. O subtotal da linha (`preco_unitario * quantidade`) é calculado no servidor em `OrderService` ao montar os DTOs de leitura.

### 7.3 Tabela `endereco_pedido`

Preserva a fotografia histórica do endereço de entrega vinculado ao pedido (`OrderAddress`, relação 1:1 opcional apenas para modalidade `ENTREGA`, mapeada via `@MapsId`). Edições ou remoções posteriores em `endereco` não alteram pedidos já gravados.

| Coluna | Tipo SQL | Nulo? | Descrição |
| --- | --- | --- | --- |
| `pedido_id` | `BIGINT` | Não | `PRIMARY KEY REFERENCES pedido(id)` |
| `destinatario` | `VARCHAR(120)` | Não | Nome do destinatário no momento do pedido (`CHECK (btrim(destinatario) <> '')`) |
| `cep` | `VARCHAR(8)` | Não | Oito dígitos numéricos (`CHECK (cep ~ '^[0-9]{8}$')`) |
| `logradouro` | `VARCHAR(160)` | Não | Logradouro congelado (`CHECK (btrim(logradouro) <> '')`) |
| `numero` | `VARCHAR(20)` | Não | Número ou `S/N` congelado (`CHECK (btrim(numero) <> '')`) |
| `complemento` | `VARCHAR(120)` | Sim | Complemento opcional congelado |
| `bairro` | `VARCHAR(100)` | Não | Bairro congelado (`CHECK (btrim(bairro) <> '')`) |
| `cidade` | `VARCHAR(100)` | Não | Cidade congelada (`CHECK (btrim(cidade) <> '')`) |
| `uf` | `VARCHAR(2)` | Não | UF congelada (`CHECK` das 27 UFs brasileiras) |

### 7.4 Tabela `pagamento_simulado`

Registra a transação acadêmica de pagamento simulado (`SimulatedPayment`, relação 1:1 com `pedido`), sem armazenar dados de cartão, PIX ou documento real.

| Coluna | Tipo SQL | Nulo? | Padrão | Descrição |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | Não | Identidade | Chave primária (`PRIMARY KEY`) |
| `pedido_id` | `BIGINT` | Não | — | FK `REFERENCES pedido(id)` |
| `resultado` | `VARCHAR(20)` | Não | — | `APROVADO` ou `RECUSADO` (`CHECK (resultado IN ('APROVADO', 'RECUSADO'))`) |
| `criado_em` | `TIMESTAMPTZ` | Não | `CURRENT_TIMESTAMP` | Instante de criação do registro de pagamento simulado |

- **Restrição de unicidade**: `CONSTRAINT uk_pagamento_simulado_pedido UNIQUE (pedido_id)` garante no máximo um registro de pagamento simulado por pedido.
