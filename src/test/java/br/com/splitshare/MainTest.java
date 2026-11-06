package br.com.splitshare;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void demonstracaoContinuaDisponivelSemBanco() {
        StringWriter texto = new StringWriter();
        int codigo = Main.executar(new String[] {"--demo"}, new Scanner(""), new PrintWriter(texto));
        assertEquals(0, codigo);
        assertTrue(texto.toString().contains("Ana: R$ 300.00"));
        assertTrue(texto.toString().contains("Bia: R$ 75.00"));
    }

    @Test
    void ajudaEArgumentosInvalidosRetornamCodigosDistintos() {
        StringWriter texto = new StringWriter();
        assertEquals(0, Main.executar(new String[] {"--help"}, new Scanner(""), new PrintWriter(texto)));
        assertTrue(texto.toString().contains("java -jar target/splitshare.jar"));
        assertEquals(2, Main.executar(new String[] {"--invalido"}, new Scanner(""), new PrintWriter(texto)));
    }
}
