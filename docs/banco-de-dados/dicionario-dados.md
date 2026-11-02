# Dicionário de Dados - Tabela Produto

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

O dicionário descreve o esquema previsto. A execução PostgreSQL de teste comprovou a migração em `cupcakes_test`; a leitura do histórico em `cupcakes` e a persistência após reinício devem ser acompanhadas no [plano e resultados](../testes/resultados/plano-e-resultados.md).

