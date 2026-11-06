# Validação das Etapas 4 e 5

Validação realizada em **06/11/2026**, no fuso America/Sao_Paulo.

## Implementação entregue

A aplicação inicia um menu de terminal conectado ao MySQL. O menu oferece
cadastro, listagem, edição e exclusão de moradores e despesas, seleção dos
participantes das despesas variáveis e exibição das parcelas individuais.
A distribuição é `target/splitshare.jar`, com o driver JDBC incluído.

O código mantém os modelos de domínio da Etapa 2 e usa os DAOs da Etapa 3.
A classe abstrata, a herança e o polimorfismo com `@Override` continuam no
cálculo de rateio. O DER continua representado por `morador`, `despesa` e
`despesa_morador`.

## Ambiente e comandos executados

- Windows 11, JDK Microsoft 25.0.4, compilação com `--release 17`.
- Maven 3.9.16 e MySQL Community Server 8.0.46.
- MySQL temporário com acesso local e conta de teste limitada a
  `SELECT`, `INSERT`, `UPDATE` e `DELETE`.
- Credenciais aleatórias fornecidas somente por variáveis de ambiente.
- `mvn clean test` e `mvn clean verify -Pmysql` concluídos com sucesso.
- Execução do JAR com `--demo` e validação do erro de configuração ausente.

O teste usou uma instância temporária separada, encerrada ao final. Para usar
a aplicação com dados permanentes, configure o servidor e as variáveis do
[guia de execução](../src/README.md).

## Resultados

| Suíte | Casos JUnit | Resultado |
| --- | --- | --- |
| EntradaConsoleTest | 24 | Aprovados |
| MainTest | 2 | Aprovados |
| RegressoesModeloTest | 1, que executa as 28 verificações originais | Aprovado |
| PersistenciaMySqlIT | 9 | Aprovados |
| Total Maven | 36 | Zero falhas, erros ou testes ignorados |

A contagem de 36 inclui o caso que executa as 28 verificações originais.
Os relatórios detalhados são gerados em `target/surefire-reports` e
`target/failsafe-reports`.

| Requisito | Evidência de validação |
| --- | --- |
| RF01 | Cadastro, listagem, edição e exclusão pelo menu, e-mail único, preservação dos dados após reiniciar e bloqueio de exclusão de morador vinculado |
| RF02 | Seleção automática de todos os moradores, cálculo exato e preservação dos participantes históricos ao editar a despesa fixa |
| RF03 | Seleção por ID, participantes distintos e existentes, aceitação de 15 moradores e rejeição de 16 |
| RF04 | Leitura do banco por novos processos Java, exibição de totais e parcelas, soma das cotas igual ao valor total |
| RF05 | Edição das despesas fixas e variáveis, troca de participantes nas variáveis, confirmação de exclusão e remoção dos vínculos |

Os testes de integração executam o JAR em novos processos e consultam o
banco para verificar o resultado. Também cobrem entrada inválida,
cancelamento durante edição, Enter para manter campos, encerramento da entrada,
IDs inexistentes e rollback de inclusão e edição quando um vínculo falha.

## SQL e empacotamento

`database/schema.sql` e `database/teste_banco.sql` foram executados no MySQL.
O teste retornou três participantes com parcela de R$ 300,00 no aluguel e
dois participantes com parcela de R$ 75,00 no mercado. Após o rollback,
a contagem de despesas foi zero. Ao final da suíte JDBC, o banco de teste
também ficou com zero moradores, despesas e vínculos.

O Maven Shade emitiu um aviso sobre os manifestos presentes nos JARs de origem.
A configuração define o ponto de entrada `br.com.splitshare.Main`, e a
execução do JAR resultante foi validada pelos testes de integração.

## Correção identificada pelos testes

A conta de permissões restritas recebeu o código 1217 ao tentar excluir um
morador vinculado. O menu inicialmente exibia uma mensagem genérica para essa
variante. O tratamento foi ampliado para os códigos 1216/1217 e 1451/1452,
e a suíte completa passou depois da correção. Esses códigos alternativos
estão documentados no rastreador oficial do MySQL.
[Referência do MySQL](https://bugs.mysql.com/bug.php?id=112589)

## Entrega acadêmica

O escopo técnico foi conferido contra RF01–RF05 no README, o DER e o diagrama
de classes do repositório, além dos critérios de POO e persistência recuperados
do histórico da entrega.

Na consulta ao histórico da branch principal, os 13 commits retornados estavam
atribuídos à conta `gabiimarotti`. A equipe deve conferir e registrar as
contribuições reais das demais integrantes conforme as exigências da AEP,
revisar o PR e efetuar a submissão na plataforma da instituição.
