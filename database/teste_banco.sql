-- SplitShare | Teste manual transacional. Execute apos schema.sql.
-- Dados de teste sao desfeitos com ROLLBACK.
USE splitshare;
START TRANSACTION;
INSERT INTO morador(nome,email,telefone_celular) VALUES ('Teste A','splitshare.teste.a@example.invalid','44999990001');
SET @a = LAST_INSERT_ID();
INSERT INTO morador(nome,email,telefone_celular) VALUES ('Teste B','splitshare.teste.b@example.invalid','44999990002');
SET @b = LAST_INSERT_ID();
INSERT INTO morador(nome,email,telefone_celular) VALUES ('Teste C','splitshare.teste.c@example.invalid','44999990003');
SET @c = LAST_INSERT_ID();
INSERT INTO despesa(descricao,valor_total,data_vencimento,tipo_despesa)
VALUES ('Aluguel teste',900.00,'2026-11-10','Fixa');
SET @fixa = LAST_INSERT_ID();
INSERT INTO despesa_morador(id_despesa,id_morador) VALUES (@fixa,@a),(@fixa,@b),(@fixa,@c);
INSERT INTO despesa(descricao,valor_total,data_vencimento,tipo_despesa)
VALUES ('Mercado teste',150.00,'2026-11-12','Variavel');
SET @variavel = LAST_INSERT_ID();
INSERT INTO despesa_morador(id_despesa,id_morador) VALUES (@variavel,@a),(@variavel,@b);
-- Esperado: Aluguel 3 participantes, parcela 300; Mercado 2, parcela 75.
SELECT d.descricao, d.tipo_despesa, d.valor_total,
 COUNT(dm.id_morador) AS participantes,
 ROUND(d.valor_total / NULLIF(COUNT(dm.id_morador),0),2) AS parcela_aproximada
FROM despesa d JOIN despesa_morador dm ON dm.id_despesa=d.id_despesa
WHERE d.id_despesa IN (@fixa,@variavel)
GROUP BY d.id_despesa,d.descricao,d.tipo_despesa,d.valor_total
ORDER BY d.id_despesa;
ROLLBACK;
