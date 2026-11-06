# Executar e validar o SplitShare

## Preparar o ambiente

Use **JDK 17 ou superior**, **Maven 3.9 ou superior** e **MySQL 8.0.16 ou superior**.
Confira `java -version` e `mvn -version`. Ambos devem apontar para um JDK compatível.
Se usar o IntelliJ, configure o SDK do projeto e o JDK do Maven com essa versão.

Execute os comandos na raiz do repositório. O Maven baixa o Connector/J e as
dependências de teste na primeira execução.

## Compilar e abrir a demonstração

```text
mvn clean package
java -jar target/splitshare.jar --demo
```

O arquivo `target/splitshare.jar` inclui o driver MySQL e inicia com `java -jar`.
A opção `--demo` executa os exemplos de rateio da Etapa 2. A opção `--help`
mostra os argumentos aceitos.

## Configurar o banco de uso

1. Inicie o MySQL e importe [database/schema.sql](../database/schema.sql) pelo
   MySQL Workbench ou pelo phpMyAdmin conectado ao servidor.
2. Configure uma conta com `SELECT`, `INSERT`, `UPDATE` e `DELETE` em
   `splitshare.*`. A criação do banco e das tabelas é feita pela conta que
   executa o esquema.
3. Informe a conexão, o usuário e a senha por variáveis de ambiente. Exemplo
   para PowerShell 5.1 ou superior, com a senha solicitada de forma oculta:

```powershell
$env:SPLITSHARE_DB_URL = 'jdbc:mysql://127.0.0.1:3306/splitshare'
$env:SPLITSHARE_DB_USER = 'splitshare_app'
$env:SPLITSHARE_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha do MySQL' -AsSecureString)).Password
java -jar target/splitshare.jar
```

Substitua o usuário, o endereço e a porta pelos da sua instalação.
Em Linux/macOS, configure as mesmas variáveis com `export`; em uma IDE,
configure-as no ambiente da execução de `br.com.splitshare.Main`.
As variáveis precisam estar disponíveis no processo que inicia o Java.
A aplicação lê diretamente o ambiente. Arquivos `.env` exigem carregamento
externo. Guarde as credenciais somente no ambiente local.

Para MySQL remoto com TLS, configure `sslMode=VERIFY_IDENTITY` e a cadeia
de confiança correspondente na conexão. A validação desta entrega foi feita
com MySQL 8.0.46. O XAMPP pode fornecer MariaDB, cuja compatibilidade precisa
ser verificada no ambiente usado.

## Usar o menu

| Opção | Ação | Requisito |
| --- | --- | --- |
| 1 | Cadastrar morador com nome, e-mail e celular | RF01 |
| 2 | Listar moradores com seus IDs e dados | RF01 |
| 3 | Editar morador pelo ID | RF01 |
| 4 | Excluir morador pelo ID, após confirmar com SIM | RF01 |
| 5 | Cadastrar despesa fixa para todos os moradores atuais | RF02 |
| 6 | Cadastrar despesa variável e informar os IDs dos participantes | RF03 |
| 7 | Listar despesas, vencimentos, totais e parcelas de cada morador | RF04 |
| 8 | Editar descrição, valor e vencimento; nas variáveis, editar também os participantes | RF05 |
| 9 | Excluir despesa e seus vínculos, após confirmar com SIM | RF05 |
| 0 | Encerrar a aplicação | — |

- Use valores como `150,00` ou `150.00`, sem separador de milhares.
- Use datas no formato `dd/mm/aaaa`, como `10/11/2026`.
- Na despesa variável, informe de 1 a 15 IDs distintos separados por vírgula.
  A lista de moradores disponíveis aparece antes da seleção.
- Ao editar, pressione Enter para manter o valor apresentado.
- Digite `/cancelar` em qualquer campo para voltar ao menu.
- Entradas inválidas retornam ao menu com uma orientação. Os dados são
  enviados ao DAO depois do preenchimento e da validação da operação.
- A exclusão exige a palavra `SIM`. Qualquer outra resposta cancela.
- Após uma falha de comunicação com o banco, consulte a listagem antes de
  repetir uma gravação, pois uma perda de conexão pode ocorrer após o commit.

As despesas fixas guardam os moradores do momento do cadastro. Incluir novos
moradores posteriormente ou editar uma despesa fixa conserva esse conjunto.
O tipo da despesa é definido no cadastro. Moradores com despesas vinculadas
têm a exclusão bloqueada pelo banco.

Os modelos calculam as parcelas com `BigDecimal`. A leitura do banco ordena
os moradores pelo ID e distribui os centavos restantes a partir do menor ID.
O total das parcelas corresponde exatamente ao valor da despesa.

## Testar sem MySQL

```text
mvn clean test
```

Executa as verificações originais dos modelos e os testes de valores, datas,
IDs, cancelamento, mensagens e argumentos da aplicação.

## Testar com MySQL

Use um banco **descartável e exclusivo para os testes**, chamado
`splitshare_test`. A suíte apaga seus moradores e despesas antes e depois
de cada caso.

1. Importe uma cópia de [schema.sql](../database/schema.sql), substituindo o
   nome `splitshare` por `splitshare_test` nas instruções `CREATE DATABASE`
   e `USE`.
2. Crie uma conta de teste com `SELECT`, `INSERT`, `UPDATE` e `DELETE`
   em `splitshare_test.*`.
3. Configure as variáveis de teste e execute:

```powershell
$env:SPLITSHARE_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3306/splitshare_test'
$env:SPLITSHARE_TEST_DB_USER = 'splitshare_test'
$env:SPLITSHARE_TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha do MySQL de teste' -AsSecureString)).Password
mvn clean verify -Pmysql
```

O perfil compila, testa e empacota a aplicação antes de executar a integração.
Os testes do menu iniciam o próprio `target/splitshare.jar` em processos Java
separados, usando as credenciais do banco de teste nesses processos.
A execução cobre CRUD, leitura após reiniciar, rateio, limites, cancelamento,
entradas inválidas, restrições e rollback de operações com múltiplos vínculos.

Confira os resultados em `target/surefire-reports` e
`target/failsafe-reports`. A ausência do banco ou das variáveis faz o perfil
`mysql` falhar. Para o teste manual dos scripts SQL, siga o
[guia do banco](../database/README.md).

## Roteiro de apresentação

Em um banco reservado para a demonstração, cadastre Ana, Bia e Leo pela opção 1.
Anote os IDs exibidos. Cadastre um aluguel fixo de R$ 900,00 pela opção 5 e um
mercado de R$ 150,00 para Ana e Bia pela opção 6. A opção 7 deve exibir R$ 300,00
por morador no aluguel e R$ 75,00 para cada participante do mercado.

Encerre com 0, abra o JAR novamente e consulte as despesas. Edite o mercado
para R$ 180,00 e confira as parcelas de R$ 90,00. Tente excluir Ana enquanto
ela participa das despesas e confira o bloqueio. Exclua as despesas e depois
exclua a moradora.

## Organização e entrega

- `model` contém a classe abstrata `Despesa`, suas subclasses e `Morador`.
  O cálculo usa herança e polimorfismo com `@Override`.
- `dao` contém as consultas parametrizadas, os vínculos do DER e as transações.
- `console` contém o menu, a leitura e a apresentação dos dados.
- `Main` inicia o menu ou a demonstração.
- `src/test` contém os testes de domínio, entrada, persistência e aplicação.

O relatório técnico está em [validação das Etapas 4 e 5](../docs/validacao-etapas-4-5.md).
A equipe deve conferir as contribuições individuais no GitHub e realizar a
submissão institucional da segunda entrega.
