CREATE TABLE IF NOT EXISTS venda (
    id BIGSERIAL PRIMARY KEY,
    id_produto BIGINT NOT NULL,
    nome_produto VARCHAR(120) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    valor_unitario NUMERIC(12, 2) NOT NULL,
    valor_total NUMERIC(14, 2) NOT NULL,
    usuario VARCHAR(160) NOT NULL,
    data_venda TIMESTAMP WITH TIME ZONE NOT NULL
);
