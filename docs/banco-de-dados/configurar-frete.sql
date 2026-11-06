\set ON_ERROR_STOP on
BEGIN;
INSERT INTO faixa_frete (cep_inicio, cep_fim, valor, ativa)
VALUES (:'startPostalCode', :'endPostalCode', :'amount'::numeric, true);
COMMIT;
