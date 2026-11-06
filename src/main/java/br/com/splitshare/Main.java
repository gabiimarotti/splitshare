package br.com.splitshare;

import br.com.splitshare.console.ConsoleSplitShare;
import br.com.splitshare.dao.ConexaoJdbc;
import br.com.splitshare.dao.DespesaDAO;
import br.com.splitshare.dao.MoradorDAO;
import br.com.splitshare.model.Despesa;
import br.com.splitshare.model.DespesaFixa;
import br.com.splitshare.model.DespesaVariavel;
import br.com.splitshare.model.Morador;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/** Entrada da aplicação com persistência; --demo executa a demonstração dos modelos. */
public class Main {
    public static void main(String[] args) {
        int codigo = executar(args, new Scanner(System.in), new PrintWriter(System.out, true));
        if (codigo != 0) {
            System.exit(codigo);
        }
    }

    static int executar(String[] args, Scanner entrada, PrintWriter saida) {
        if (args.length == 1 && args[0].equals("--demo")) {
            demonstrar(saida);
            saida.flush();
            return 0;
        }
        if (args.length > 0) {
            saida.println("Uso: java -jar target/splitshare.jar [--demo | --help]");
            saida.println("Sem argumentos, inicia o menu conectado ao MySQL.");
            saida.flush();
            return args.length == 1 && args[0].equals("--help") ? 0 : 2;
        }
        try {
            ConexaoJdbc banco = ConexaoJdbc.doAmbiente();
            new ConsoleSplitShare(new MoradorDAO(banco), new DespesaDAO(banco), entrada, saida).executar();
            return 0;
        } catch (IllegalStateException configuracaoIncompleta) {
            saida.println(configuracaoIncompleta.getMessage());
            saida.println("Consulte src/README.md para configurar o acesso ao MySQL.");
            saida.flush();
            return 1;
        }
    }

    private static void demonstrar(PrintWriter saida) {
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
            saida.println(despesa.getDescricao() + " (" + despesa.getTipoDespesa() + ")");
            saida.println("  Total: R$ " + despesa.getValorTotal());
            despesa.calcularRateiosIndividuais().forEach((morador, valor) ->
                    saida.println("  " + morador.getNome() + ": R$ " + valor));
        }
        saida.println("Demonstracao local: dados nao foram gravados no banco.");
    }
}
