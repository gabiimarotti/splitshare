# SplitShare em Java

## Compilar e validar os modelos

Na raiz do repositório, com JDK 17 ou superior e Maven 3.9 ou superior:

```text
mvn clean test
java -cp target/classes br.com.splitshare.Main
```

`mvn clean test` compila o código e executa as verificações da Etapa 2.
`Main` continua sendo a demonstração de rateio com dados fictícios.

## Configurar o MySQL

1. Inicie um servidor **MySQL 8.0.16 ou superior**. Importe
   [schema.sql](../database/schema.sql) pelo MySQL Workbench ou pelo phpMyAdmin
   conectado a esse servidor.
2. Use uma conta MySQL com permissões `SELECT`, `INSERT`, `UPDATE` e `DELETE`
   no banco `splitshare`.
3. Configure as variáveis no ambiente que inicia o Java. Exemplo no PowerShell:

   ```powershell
   $env:SPLITSHARE_DB_URL = 'jdbc:mysql://127.0.0.1:3306/splitshare'
   $env:SPLITSHARE_DB_USER = 'splitshare_app'
   $env:SPLITSHARE_DB_PASSWORD = Read-Host 'Senha do MySQL' -MaskInput
   ```

   `-MaskInput` requer PowerShell 7. No Windows PowerShell 5.1, use
   `[System.Net.NetworkCredential]::new('', (Read-Host 'Senha do MySQL' -AsSecureString)).Password`
   como valor de `SPLITSHARE_DB_PASSWORD`. O Connector/J é incluído pelo Maven.
   Para um servidor remoto com TLS configurado, use `sslMode=VERIFY_IDENTITY`
   e a cadeia de confiança apropriada na conexão.

## Operações disponíveis na Etapa 3

Instancie `MoradorDAO` e `DespesaDAO` passando `ConexaoJdbc.doAmbiente()`.
A camada chamadora recebe `SQLException` quando o banco rejeita uma operação.

| Requisito | Operação |
| --- | --- |
| RF01 | `MoradorDAO` cadastra, busca, lista, edita e exclui moradores. O e-mail é único. Moradores vinculados a despesas ficam protegidos contra exclusão. |
| RF02 | `inserirFixa(descricao, valor, vencimento)` usa todos os moradores do banco no momento do cadastro. O conjunto é preservado nas edições e nas leituras posteriores. |
| RF03 | `inserirVariavel(despesa)` grava de 1 a 15 moradores cadastrados, selecionados no modelo. |
| RF04 | `listar()` e `buscarPorId(id)` retornam despesas com total, vencimento e participantes. Use `calcularRateio()` para a cota base e `calcularRateiosIndividuais()` para as cotas exatas. |
| RF05 | `atualizar(despesa)` edita descrição, valor e vencimento e permite trocar participantes de despesas variáveis. O tipo é preservado. `excluir(id)` remove a despesa e os vínculos. |

A criação e a edição de despesas gravam os dados e os vínculos em uma única
transação. O DER guarda os IDs dos participantes. Nas despesas persistidas, os
IDs são lidos em ordem crescente, que determina quem recebe o centavo extra
quando a divisão é inexata. Os modelos preservam a classe abstrata, a herança
e o polimorfismo com `@Override`.

## Validar Java e MySQL juntos

1. Em um MySQL de teste, importe uma cópia de
   [schema.sql](../database/schema.sql) trocando `splitshare` por
   `splitshare_test` nas instruções `CREATE DATABASE` e `USE`.
2. Configure uma conta de teste com as quatro permissões no banco de teste.
   Use um **banco descartável e exclusivo para a suíte**. Os testes apagam os
   registros de `splitshare_test` antes e depois de cada caso.

   ```powershell
   $env:SPLITSHARE_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3306/splitshare_test'
   $env:SPLITSHARE_TEST_DB_USER = 'splitshare_test'
   $env:SPLITSHARE_TEST_DB_PASSWORD = Read-Host 'Senha do MySQL de teste' -MaskInput
   mvn clean verify -Pmysql
   ```

3. Confira `target/surefire-reports` e `target/failsafe-reports`. A integração
   verifica CRUD, seleção de participantes, rateio, restrições do banco e
   rollback quando um vínculo falha. O perfil `mysql` exige um banco acessível
   e as três variáveis de teste.

Para validar apenas o SQL, siga o [guia do banco](../database/README.md).
