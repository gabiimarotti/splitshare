package br.com.splitshare.console;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EntradaConsoleTest {
    @Test
    void aceitaVirgulaEPontoSemPerderCentavos() {
        assertEquals(new BigDecimal("150.25"), EntradaConsole.converterValor("150,25"));
        assertEquals(new BigDecimal("0.01"), EntradaConsole.converterValor("0.01"));
        assertEquals(new BigDecimal("99999999.99"), EntradaConsole.converterValor("99999999,99"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "1,001", "1.000,00", "1e2", "NaN", "100000000"})
    void rejeitaValoresAmbiguosOuForaDoDominio(String valor) {
        assertThrows(IllegalArgumentException.class, () -> EntradaConsole.converterValor(valor));
    }

    @Test
    void aceitaDiaBissextoEDataLimiteDoBanco() {
        assertEquals(LocalDate.of(2028, 2, 29), EntradaConsole.converterData("29/02/2028"));
        assertEquals(LocalDate.of(1000, 1, 1), EntradaConsole.converterData("01/01/1000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"31/02/2026", "29/02/2026", "01/13/2026", "2026-11-10", "01/01/0999", ""})
    void rejeitaDatasInvalidas(String data) {
        assertThrows(IllegalArgumentException.class, () -> EntradaConsole.converterData(data));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "abc", "2147483648", ""})
    void rejeitaIdsInvalidos(String id) {
        assertThrows(IllegalArgumentException.class, () -> EntradaConsole.converterId(id));
    }

    @Test
    void enterMantemValorECancelamentoEFimDeEntradaSaoDistintos() {
        EntradaConsole entrada = new EntradaConsole(new Scanner("\n/cancelar\n"), new PrintWriter(new StringWriter()));
        assertEquals("Ana", entrada.editarTexto("Nome", "Ana"));
        assertThrows(EntradaConsole.OperacaoCancelada.class, () -> entrada.texto("E-mail"));
        assertThrows(NoSuchElementException.class, () -> entrada.texto("Nome"));
    }

    @Test
    void mensagensDeBancoOrientamSemRevelarConteudoDaExcecao() {
        String segredo = "senha-na-url-ou-no-erro";
        for (int codigo : new int[] {1062, 1217, 1451, 1216, 1452, 1045, 1049, 1146, 0}) {
            String mensagem = ConsoleSplitShare.mensagemBanco(new SQLException(segredo, "HY000", codigo));
            assertFalse(mensagem.contains(segredo));
            assertFalse(mensagem.isBlank());
        }
        assertTrue(ConsoleSplitShare.mensagemBanco(new SQLException("", "23000", 1451)).contains("participa de despesas"));
        assertEquals(ConsoleSplitShare.mensagemBanco(new SQLException("", "23000", 1451)),
                ConsoleSplitShare.mensagemBanco(new SQLException("", "23000", 1217)));
        assertEquals(ConsoleSplitShare.mensagemBanco(new SQLException("", "23000", 1452)),
                ConsoleSplitShare.mensagemBanco(new SQLException("", "23000", 1216)));
    }
}
