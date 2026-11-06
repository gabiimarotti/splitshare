package br.com.splitshare;

import br.com.splitshare.model.Despesa;
import br.com.splitshare.model.DespesaFixa;
import br.com.splitshare.model.DespesaVariavel;
import br.com.splitshare.model.Morador;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Demonstracao local das regras de rateio. Ainda nao realiza persistencia MySQL. */
public class Main {
    public static void main(String[] args) {
        Morador ana = new Morador(1, "Ana", "ana@example.invalid", "44999990001");
        Morador bia = new Morador(2, "Bia", "bia@example.invalid", "44999990002");
        Morador leo = new Morador(3, "Leo", "leo@example.invalid", "44999990003");

        Despesa fixa = new DespesaFixa("Aluguel", new BigDecimal("900.00"),
                LocalDate.of(2026, 11, 10), List.of(ana, bia, leo));
        DespesaVariavel variavel = new DespesaVariavel("Mercado", new BigDecimal("150.00"),
                LocalDate.of(2026, 11, 12));
        variavel.adicionarMorador(ana);
        variavel.adicionarMorador(bia);

        // O mesmo tipo abstrato usa implementacoes distintas via polimorfismo.
        for (Despesa despesa : List.of(fixa, variavel)) {
            System.out.println(despesa.getDescricao() + " (" + despesa.getTipoDespesa() + ")");
            System.out.println("  Total: R$ " + despesa.getValorTotal());
            despesa.calcularRateiosIndividuais().forEach((morador, valor) ->
                    System.out.println("  " + morador.getNome() + ": R$ " + valor));
        }
        System.out.println("Demonstracao local: dados nao foram gravados no banco.");
    }
}
