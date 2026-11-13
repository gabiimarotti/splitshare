# Diagrama de classes

```mermaid
classDiagram
    class Despesa {
        <<abstract>>
        -int id
        -String descricao
        -BigDecimal valorTotal
        -LocalDate dataVencimento
        +calcularRateio() BigDecimal
        +getTipoDespesa() String
        +getMoradoresEnvolvidos() List~Morador~
        +calcularRateiosIndividuais() Map
    }
    class DespesaFixa {
        -List~Morador~ moradoresEnvolvidos
        +calcularRateio() BigDecimal
    }
    class DespesaVariavel {
        -List~Morador~ moradoresEnvolvidos
        +adicionarMorador(Morador morador)
        +removerMorador(int idMorador) boolean
        +calcularRateio() BigDecimal
    }
    class Morador {
        -int id
        -String nome
        -String email
        -String telefone
    }
    class MoradorDAO {
        +inserir(Morador morador) int
        +listar() List~Morador~
        +buscarPorId(int id) Optional~Morador~
        +atualizar(Morador morador) boolean
        +excluir(int id) boolean
    }
    class DespesaDAO {
        +inserirFixa(String descricao, BigDecimal valor, LocalDate vencimento) DespesaFixa
        +inserirVariavel(DespesaVariavel despesa) int
        +listar() List~Despesa~
        +buscarPorId(int id) Optional~Despesa~
        +atualizar(Despesa despesa) boolean
        +excluir(int id) boolean
    }
    Despesa <|-- DespesaFixa
    Despesa <|-- DespesaVariavel
    DespesaFixa --> "1..*" Morador : moradores no lançamento
    DespesaVariavel --> "1..15" Morador : moradores selecionados
    MoradorDAO ..> Morador
    DespesaDAO ..> Despesa
```

As multiplicidades se referem a despesas salvas. Cada morador pode participar de várias despesas e tem cadastro próprio. O RF01 limita o cadastro geral a 15 moradores e o RF03 limita cada despesa variável a 15 participantes.

A implementação trocou o `double` e o vetor da concepção por `BigDecimal` e listas tipadas. O Java distribui os centavos e os DAOs gravam os registros em transações JDBC. O MySQL mantém a integridade referencial. Conforme o [DER](diagrama-do-banco.png), a tabela `despesa_morador` registra participações no rateio, e o projeto não registra pagamentos. As regras de rateio ficam no Java, sem triggers ou procedimentos armazenados.
