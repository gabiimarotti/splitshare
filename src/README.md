# Etapa 2: Classes de domínio Java (SplitShare)

Implementação dos modelos Java previstos no diagrama de classes da primeira entrega:

- `Morador`: dados pessoais e validações básicas (RF01).
- `Despesa`: classe **abstrata** com valores monetários `BigDecimal` e método abstrato `calcularRateio()`.
- `DespesaFixa`: **herança** e `@Override` para dividir entre a lista de todos os moradores do momento do cadastro (RF02).
- `DespesaVariavel`: **herança**, `@Override` e participantes selecionados (RF03).
- `Main`: demonstração local do **polimorfismo** com valores fictícios.

## Executar (JDK 17 ou superior)

No terminal Linux/macOS, a partir da raiz do repositório:

```bash
mkdir -p out
javac --release 17 -d out $(find src/main/java -name '*.java')
java -cp out br.com.splitshare.Main
javac --release 17 -cp out -d out $(find src/test/java -name '*.java')
java -cp out br.com.splitshare.model.ModelosTest
```

No PowerShell (Windows), a partir da raiz do repositório:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac --release 17 -d out (Get-ChildItem src/main/java -Recurse -Filter *.java).FullName
java -cp out br.com.splitshare.Main
javac --release 17 -cp out -d out (Get-ChildItem src/test/java -Recurse -Filter *.java).FullName
java -cp out br.com.splitshare.model.ModelosTest
```

## Como funciona o rateio

- `calcularRateio()` retorna a parcela base, arredondada **para baixo** a dois decimais.
- `calcularRateiosIndividuais()` distribui os centavos restantes entre os primeiros moradores da lista; a soma sempre bate com o total.
- Os participantes da despesa fixa são copiados no momento de sua criação: adicionar um novo morador depois não muda as despesas antigas.
- A lista variável permite `adicionarMorador()` e `removerMorador()`, com bloqueio de IDs repetidos.
- O uso de `BigDecimal` mantém compatibilidade com `DECIMAL(10,2)` do MySQL.

**Limitação atual:** nesta etapa os dados existem apenas em memória para testar a modelagem. A persistência MySQL, os DAOs (`MoradorDAO` e `DespesaDAO`) e o CRUD ficarão para a próxima etapa. Não confundir `Main` com aplicação concluída.
