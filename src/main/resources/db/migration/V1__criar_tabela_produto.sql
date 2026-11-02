CREATE TABLE produto (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    preco NUMERIC(10, 2) NOT NULL,
    imagem_caminho VARCHAR(255),
    disponivel BOOLEAN NOT NULL DEFAULT TRUE,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_produto_preco_positivo CHECK (preco > 0)
);

CREATE INDEX idx_produto_disponivel ON produto (disponivel);

