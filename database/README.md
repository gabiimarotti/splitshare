# Banco de dados SplitShare

O arquivo `schema.sql` cria o banco MySQL `splitshare` e as tabelas `morador`, `despesa` e `despesa_morador`, seguindo o DER da primeira entrega.

## Como executar

Requer **MySQL 8.0.16 ou superior**. No MySQL Workbench ou phpMyAdmin, conecte-se a um servidor MySQL e importe `database/schema.sql`. Confira com o comando abaixo.

```sql
USE splitshare;
SHOW TABLES;
```

Resultado esperado: `morador`, `despesa`, `despesa_morador`.

Depois, execute `database/teste_banco.sql` para inserir dados temporários, conferir duas divisões de despesas e desfazer os testes com `ROLLBACK`.

## Decisões importantes

- O banco usa `DECIMAL(10,2)` para dinheiro; na aplicação Java, prefira `BigDecimal`.
- Despesas fixas devem vincular todos os moradores cadastrados **no momento do cadastro da despesa**.
- Despesas variáveis devem vincular apenas os moradores selecionados.
- O `DespesaDAO` grava despesa e participantes na **mesma transação** e rejeita despesas sem participantes.
- A exclusão de despesas remove vínculos; a exclusão de moradores com despesas vinculadas é bloqueada para não alterar silenciosamente rateios antigos.
- Os modelos Java calculam a divisão exata dos centavos.
- O script no GitHub **não instala nem executa** um servidor MySQL: é preciso executá-lo no ambiente local.

Referências: requisitos RF01–RF05 no README principal e diagramas em `docs/`.

Para conectar o Java e rodar a suíte JDBC, consulte o [guia da Etapa 3](../src/README.md).
