CREATE TABLE pedido (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    revisao_id VARCHAR(64) NOT NULL CHECK (btrim(revisao_id) <> ''),
    modalidade VARCHAR(20) NOT NULL CHECK (modalidade IN ('ENTREGA', 'RETIRADA')),
    status_atendimento VARCHAR(30) NOT NULL CHECK (status_atendimento IN (
        'RECEBIDO',
        'EM_PREPARACAO',
        'PRONTO_PARA_RETIRADA',
        'SAIU_PARA_ENTREGA',
        'CONCLUIDO',
        'CANCELADO',
        'NAO_AUTORIZADO'
    )),
    subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    frete NUMERIC(12, 2) NOT NULL CHECK (frete >= 0),
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0 CHECK (versao >= 0),
    CONSTRAINT uk_pedido_usuario_revisao UNIQUE (usuario_id, revisao_id),
    CONSTRAINT chk_pedido_total_soma CHECK (total = subtotal + frete),
    CONSTRAINT chk_pedido_retirada_frete_zero CHECK (modalidade <> 'RETIRADA' OR frete = 0),
    CONSTRAINT chk_pedido_status_modalidade CHECK (
        NOT (modalidade = 'ENTREGA' AND status_atendimento = 'PRONTO_PARA_RETIRADA')
        AND NOT (modalidade = 'RETIRADA' AND status_atendimento = 'SAIU_PARA_ENTREGA')
    )
);

CREATE INDEX idx_pedido_usuario_criado_em ON pedido (usuario_id, criado_em DESC, id DESC);
CREATE INDEX idx_pedido_criado_em ON pedido (criado_em DESC, id DESC);

CREATE TABLE item_pedido (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido(id),
    produto_id BIGINT NOT NULL REFERENCES produto(id),
    nome_produto VARCHAR(150) NOT NULL CHECK (btrim(nome_produto) <> ''),
    preco_unitario NUMERIC(12, 2) NOT NULL CHECK (preco_unitario > 0),
    quantidade INTEGER NOT NULL CHECK (quantidade BETWEEN 1 AND 99),
    CONSTRAINT uk_item_pedido_produto UNIQUE (pedido_id, produto_id)
);

CREATE INDEX idx_item_pedido_produto_id ON item_pedido (produto_id);

CREATE TABLE endereco_pedido (
    pedido_id BIGINT PRIMARY KEY REFERENCES pedido(id),
    destinatario VARCHAR(120) NOT NULL CHECK (btrim(destinatario) <> ''),
    cep VARCHAR(8) NOT NULL CHECK (cep ~ '^[0-9]{8}$'),
    logradouro VARCHAR(160) NOT NULL CHECK (btrim(logradouro) <> ''),
    numero VARCHAR(20) NOT NULL CHECK (btrim(numero) <> ''),
    complemento VARCHAR(120),
    bairro VARCHAR(100) NOT NULL CHECK (btrim(bairro) <> ''),
    cidade VARCHAR(100) NOT NULL CHECK (btrim(cidade) <> ''),
    uf VARCHAR(2) NOT NULL CHECK (uf IN ('AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO'))
);

CREATE TABLE pagamento_simulado (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido(id),
    resultado VARCHAR(20) NOT NULL CHECK (resultado IN ('APROVADO', 'RECUSADO')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pagamento_simulado_pedido UNIQUE (pedido_id)
);

