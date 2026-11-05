CREATE TABLE usuario (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email_normalizado VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_email_normalizado UNIQUE (email_normalizado),
    CONSTRAINT chk_usuario_perfil CHECK (perfil IN ('CLIENTE', 'ADMIN'))
);
