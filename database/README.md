# Banco de dados SplitShare

O arquivo `schema.sql` cria o banco MySQL `splitshare` e as tabelas `morador`, `despesa` e `despesa_morador`, seguindo o DER da primeira entrega.

## Como executar

Requer **MySQL 8.0.16 ou superior**. No MySQL Workbench, conecte-se a um servidor MySQL, abra `database/schema.sql` e execute o script inteiro. Confira:

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
- A aplicação Java deverá gravar despesa e participantes na **mesma transação**, rejeitando despesas sem participantes.
- A exclusão de despesas remove vínculos; a exclusão de moradores com despesas vinculadas é bloqueada para não alterar silenciosamente rateios antigos.
- A divisão exata dos centavos deverá ser feita no Java.
- O script no GitHub **não instala nem executa** um servidor MySQL: é preciso executá-lo no ambiente local.

Referências: requisitos RF01–RF05 no README principal e diagramas em `docs/`.
