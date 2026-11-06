CREATE TABLE endereco (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    destinatario VARCHAR(120) NOT NULL CHECK (btrim(destinatario) <> ''),
    cep VARCHAR(8) NOT NULL CHECK (cep ~ '^[0-9]{8}$'),
    logradouro VARCHAR(160) NOT NULL CHECK (btrim(logradouro) <> ''),
    numero VARCHAR(20) NOT NULL CHECK (btrim(numero) <> ''),
    complemento VARCHAR(120),
    bairro VARCHAR(100) NOT NULL CHECK (btrim(bairro) <> ''),
    cidade VARCHAR(100) NOT NULL CHECK (btrim(cidade) <> ''),
    uf VARCHAR(2) NOT NULL CHECK (uf IN ('AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    versao BIGINT NOT NULL DEFAULT 0 CHECK (versao >= 0)
);
CREATE INDEX idx_endereco_usuario_id ON endereco(usuario_id, id);

CREATE TABLE faixa_frete (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cep_inicio VARCHAR(8) NOT NULL CHECK (cep_inicio ~ '^[0-9]{8}$'),
    cep_fim VARCHAR(8) NOT NULL CHECK (cep_fim ~ '^[0-9]{8}$'),
    valor NUMERIC(12,2) NOT NULL CHECK (valor >= 0),
    ativa BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (cep_inicio <= cep_fim),
    EXCLUDE USING gist (int8range(cep_inicio::bigint, cep_fim::bigint, '[]') WITH &&) WHERE (ativa)
);
