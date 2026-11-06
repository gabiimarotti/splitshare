package br.com.splitshare.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Testes sem dependencias externas: executar pelo metodo main. */
public class ModelosTest {
    private static int testes = 0;

    public static void main(String[] args) {
        Morador ana = new Morador(1, "Ana", "ana@example.invalid", "44999990001");
        Morador bia = new Morador(2, "Bia", "bia@example.invalid", "44999990002");
        Morador leo = new Morador(3, "Leo", "leo@example.invalid", "44999990003");
        List<Morador> listaOriginal = new ArrayList<>(List.of(ana, bia, leo));

        Despesa fixa = new DespesaFixa("Aluguel", new BigDecimal("900.00"),
                LocalDate.of(2026, 11, 10), listaOriginal);
        conferir(new BigDecimal("300.00").equals(fixa.calcularRateio()), "Rateio fixo");
        conferir("Fixa".equals(fixa.getTipoDespesa()), "Tipo de despesa fixa");
        listaOriginal.add(new Morador(4, "Lia", "lia@example.invalid", "44999990004"));
        conferir(fixa.getMoradoresEnvolvidos().size() == 3, "Snapshot dos moradores fixos");

        DespesaVariavel variavel = new DespesaVariavel("Mercado", new BigDecimal("150.00"),
                LocalDate.of(2026, 11, 12));
        variavel.adicionarMorador(ana);
        variavel.adicionarMorador(bia);
        conferir(new BigDecimal("75.00").equals(variavel.calcularRateio()), "Rateio variavel");
        conferir(variavel.getQuantidadeAtual() == 2, "Participantes selecionados");
        conferir("Variavel".equals(variavel.getTipoDespesa()), "Tipo de despesa variavel");
        conferir(variavel.removerMorador(2) && variavel.getQuantidadeAtual() == 1,
                "Remocao do participante");
        conferir(new BigDecimal("150.00").equals(variavel.calcularRateio()),
                "Recalculo depois da remocao");
        conferir(!variavel.removerMorador(3), "Nao remove participante ausente");
        falhaEsperada(() -> variavel.adicionarMorador(ana), "Bloquear duplicado pela instancia");
        falhaEsperada(() -> variavel.adicionarMorador(
                new Morador(1, "Outra Ana", "outra@example.invalid", "44999990005")),
                "Bloquear duplicado pelo ID");

        // Limite 1..15 da composicao DespesaVariavel x Morador no diagrama.
        List<Morador> quinze = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            quinze.add(new Morador(100 + i, "Participante " + i,
                    "participante" + i + "@example.invalid", "44999991000"));
        }
        DespesaVariavel comQuinze = new DespesaVariavel(0, "Rateio limite",
                new BigDecimal("150.00"), LocalDate.of(2026, 11, 15), quinze);
        conferir(comQuinze.getQuantidadeAtual() == 15, "Aceitar quinze moradores no construtor");
        conferir(new BigDecimal("10.00").equals(comQuinze.calcularRateio()),
                "Rateio com quinze moradores");
        falhaEsperada(() -> comQuinze.adicionarMorador(
                new Morador(116, "Participante extra", "extra@example.invalid", "44999991001")),
                "Bloquear o decimo sexto morador via adicionarMorador");
        conferir(comQuinze.getQuantidadeAtual() == 15,
                "Manter lista original apos tentativa de exceder limite");
        List<Morador> dezesseis = new ArrayList<>(quinze);
        dezesseis.add(new Morador(116, "Participante extra", "extra@example.invalid", "44999991001"));
        falhaEsperada(() -> new DespesaVariavel(0, "Excesso de participantes",
                new BigDecimal("160.00"), LocalDate.of(2026, 11, 15), dezesseis),
                "Bloquear dezesseis moradores via construtor");

        Despesa centavos = new DespesaFixa("Conta de teste", new BigDecimal("100.00"),
                LocalDate.of(2026, 11, 10), List.of(ana, bia, leo));
        Map<Morador, BigDecimal> cotas = centavos.calcularRateiosIndividuais();
        conferir(new BigDecimal("33.34").equals(cotas.get(ana)), "Centavo para o primeiro");
        conferir(new BigDecimal("33.33").equals(cotas.get(bia)), "Cota do segundo");
        conferir(new BigDecimal("100.00").equals(
                cotas.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)),
                "Soma precisa das cotas");
        falhaEsperada(() -> cotas.clear(), "Resultado de rateio imutavel");

        DespesaVariavel vazia = new DespesaVariavel("Teste", new BigDecimal("5.00"),
                LocalDate.of(2026, 11, 10));
        falhaEsperada(vazia::calcularRateio, "Nao dividir sem moradores");
        falhaEsperada(() -> new DespesaFixa("Teste", new BigDecimal("5.00"),
                LocalDate.of(2026, 11, 10), List.of()), "Nao criar fixa sem moradores");
        falhaEsperada(() -> fixa.setValorTotal(new BigDecimal("-1.00")), "Valor negativo");
        falhaEsperada(() -> fixa.setValorTotal(new BigDecimal("1.001")), "Mais de dois decimais");
        falhaEsperada(() -> fixa.setValorTotal(new BigDecimal("100000000.00")), "Excede DECIMAL(10,2)");
        falhaEsperada(() -> ana.setNome("  "), "Nome obrigatorio");
        falhaEsperada(() -> ana.setEmail("invalido"), "E-mail valido");
        falhaEsperada(() -> fixa.getMoradoresEnvolvidos().clear(), "Lista fixa imutavel");

        System.out.println("OK: " + testes + " testes passaram.");
    }

    private static void conferir(boolean condicao, String caso) {
        if (!condicao) throw new AssertionError("Falha: " + caso);
        testes++;
    }

    private static void falhaEsperada(Runnable acao, String caso) {
        try {
            acao.run();
            throw new AssertionError("Falha: deveria rejeitar " + caso);
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException esperado) {
            testes++;
        }
    }
}
