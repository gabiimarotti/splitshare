package br.com.splitshare.console;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;

final class EntradaConsole {
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private final Scanner leitor;
    private final PrintWriter saida;

    EntradaConsole(Scanner leitor, PrintWriter saida) {
        this.leitor = leitor;
        this.saida = saida;
    }

    String texto(String pergunta) {
        saida.print(pergunta + " > ");
        saida.flush();
        String valor = leitor.nextLine().trim();
        if (valor.equalsIgnoreCase("/cancelar")) {
            throw new OperacaoCancelada();
        }
        return valor;
    }

    String editarTexto(String campo, String atual) {
        String valor = texto(campo + " [" + atual + "] (Enter para manter, /cancelar para voltar ao menu)");
        return valor.isEmpty() ? atual : valor;
    }

    int id(String pergunta) {
        return converterId(texto(pergunta));
    }

    static int converterId(String texto) {
        try {
            int id = Integer.parseInt(texto.trim());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignorada) {

        }
        throw new IllegalArgumentException("Informe um ID inteiro positivo.");
    }

    static BigDecimal converterValor(String texto) {
        if (!texto.matches("[0-9]{1,8}([.,][0-9]{1,2})?")) {
            throw new IllegalArgumentException("Use um valor como 150,00, sem separador de milhares, com até 2 casas decimais.");
        }
        BigDecimal valor = new BigDecimal(texto.replace(',', '.'));
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        return valor;
    }

    static LocalDate converterData(String texto) {
        try {
            LocalDate data = LocalDate.parse(texto, DATA);
            if (data.getYear() >= 1000 && data.getYear() <= 9999) {
                return data;
            }
        } catch (DateTimeParseException ignorada) {

        }
        throw new IllegalArgumentException("Informe uma data válida em dd/mm/aaaa, com ano entre 1000 e 9999.");
    }

    static final class OperacaoCancelada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
