# Validação da segunda entrega

Use uma instância descartável de MySQL ou MariaDB. Importe [schema.sql](../database/schema.sql), configure as variáveis de ambiente como no [README](../README.md) e rode:

```powershell
mvn clean package
java -jar target/splitshare.jar
```

Depois de importar o esquema, execute [teste_banco.sql](../database/teste_banco.sql) em uma única sessão SQL. O teste mostra três participantes e parcela de R$ 300,00 para o aluguel e dois participantes e parcela de R$ 75,00 para o mercado. O `ROLLBACK` desfaz as inserções do teste.

| Requisito | Operação pelo menu | Resultado esperado |
| --- | --- | --- |
| RF01 | Cadastre três moradores com e-mails distintos pela opção 1, liste pela 2, edite um pela 3 e tente repetir um e-mail. | A lista mostra os dados gravados e a edição. O e-mail duplicado é recusado. |
| RF02 | Com os três moradores cadastrados, lance uma despesa fixa de R$ 100,00 pela opção 5. | Os três participam. As parcelas são R$ 33,34, R$ 33,33 e R$ 33,33. |
| RF03 | Lance uma despesa variável de R$ 150,00 pela opção 6 e informe dois IDs. Tente repetir um ID em outro lançamento. | Cada selecionado recebe R$ 75,00. O lançamento com ID repetido é recusado. |
| RF04 | Encerre com a opção 0, abra o JAR novamente e liste as despesas pela opção 7. | O programa recupera despesas, vencimentos, totais e parcelas do banco. |
| RF05 | Edite uma despesa fixa e uma variável pela opção 8. Na variável, troque um participante. Recuse uma exclusão e depois confirme-a com `SIM` na opção 9. | Totais e parcelas refletem as novas informações. A despesa só some após a confirmação, junto com seus vínculos. |
| RF01 | Tente excluir um morador vinculado pela opção 4. Depois de excluir as despesas correspondentes, tente de novo. | A primeira tentativa é recusada. Após resolver os vínculos, a exclusão funciona. |
| RF01 | Complete 15 moradores e tente cadastrar um 16º com e-mail inédito. Liste pela opção 2. Depois exclua um morador sem vínculo e repita o cadastro. | O 15º é salvo. A tentativa seguinte exibe o aviso de limite, e a lista continua com 15. A exclusão libera uma vaga. |
| RF03 | Com 15 moradores cadastrados, crie uma despesa variável com todos os 15 IDs. | Todos entram no rateio. A soma das parcelas corresponde ao total. |

Para conferir o limite no banco, execute `SELECT COUNT(*) FROM splitshare.morador;` após o cadastro recusado. A aplicação conta os registros já existentes. Se um banco antigo já tiver mais de 15 moradores, eles continuam disponíveis para consulta, edição e exclusão, e novos cadastros só entram quando houver uma vaga.
