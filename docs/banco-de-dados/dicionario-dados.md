# Dicionário de dados — catálogo, identidade, endereços e frete

Especificação da camada de persistência para a primeira fatia do catálogo de cupcakes.

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
- Sessões são mantidas pelo contêiner, fora desta tabela; sua persistência entre reinícios não é requisito. Não há tabela de carrinho ou compras.

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

Configuração comercial de Brasília–DF: **pendente de aprovação de faixas e valores**. O [script controlado](configurar-frete.sql) aceita variáveis psql `startPostalCode`, `endPostalCode` e `amount`; deve ser usado apenas após definição comercial, com conexão local autorizada e sem segredos em arquivos. Cada execução insere uma faixa ativa em transação e é validada pelas restrições do banco. Para ajustes, usar novo script transacional revisado com ID explícito; nunca editar migrações aplicadas. Não há API administrativa. Fixtures dos testes são fictícias e exclusivas de cupcakes_test; não definem cobertura real.

