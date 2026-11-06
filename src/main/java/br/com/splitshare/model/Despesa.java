package br.com.splitshare.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Classe abstrata base para despesas fixas e variaveis (RF02 a RF05). */
public abstract class Despesa {
    private int id;
    private String descricao;
    private BigDecimal valorTotal;
    private LocalDate dataVencimento;

    protected Despesa(int id, String descricao, BigDecimal valorTotal, LocalDate dataVencimento) {
        setId(id);
        setDescricao(descricao);
        setValorTotal(valorTotal);
        setDataVencimento(dataVencimento);
    }

    public int getId() { return id; }

    /** ID zero indica registro ainda nao persistido. */
    public void setId(int id) {
        if (id < 0) throw new IllegalArgumentException("O ID da despesa nao pode ser negativo.");
        this.id = id;
    }

    public String getDescricao() { return descricao; }

    public void setDescricao(String descricao) {
        this.descricao = Morador.textoObrigatorio(descricao, 100, "descricao");
    }

    public BigDecimal getValorTotal() { return valorTotal; }

    public void setValorTotal(BigDecimal valorTotal) {
        Objects.requireNonNull(valorTotal, "O valor total e obrigatorio.");
        if (valorTotal.signum() <= 0 || valorTotal.scale() > 2
                || valorTotal.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("Informe valor positivo com ate duas casas decimais, dentro de DECIMAL(10,2).");
        }
        this.valorTotal = valorTotal.setScale(2, RoundingMode.UNNECESSARY);
    }

    public LocalDate getDataVencimento() { return dataVencimento; }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = Objects.requireNonNull(dataVencimento, "A data de vencimento e obrigatoria.");
    }

    /** Valor base da cota: eventuais centavos restantes sao distribuidos separadamente. */
    public abstract BigDecimal calcularRateio();

    /** Valor compatível com o ENUM do banco: Fixa ou Variavel. */
    public abstract String getTipoDespesa();

    /** Moradores associados a esta despesa, em ordem estavel. */
    public abstract List<Morador> getMoradoresEnvolvidos();

    protected final BigDecimal calcularCotaBase(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalStateException("Nao e possivel dividir uma despesa sem moradores.");
        }
        return valorTotal.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.DOWN);
    }

    /**
     * Rateio exato, inclusive nos casos em que ha centavos indivisiveis.
     * Os primeiros participantes, na ordem cadastrada, recebem 1 centavo adicional
     * ate que a soma das cotas seja exatamente igual ao valor total.
     */
    public final Map<Morador, BigDecimal> calcularRateiosIndividuais() {
        List<Morador> participantes = getMoradoresEnvolvidos();
        BigDecimal cotaBase = calcularRateio(); // despacho polimorfico (@Override)
        long centavosTotais = valorTotal.movePointRight(2).longValueExact();
        long centavosBase = cotaBase.movePointRight(2).longValueExact();
        long restantes = centavosTotais - centavosBase * participantes.size();
        Map<Morador, BigDecimal> resultado = new LinkedHashMap<>();
        for (Morador participante : participantes) {
            BigDecimal parcela = cotaBase;
            if (restantes > 0) {
                parcela = parcela.add(new BigDecimal("0.01"));
                restantes--;
            }
            resultado.put(participante, parcela);
        }
        return Collections.unmodifiableMap(resultado);
    }

    /** Copia defensiva e deteccao de moradores duplicados por ID ou instancia. */
    protected static List<Morador> copiarParticipantes(Collection<Morador> moradores) {
        Objects.requireNonNull(moradores, "A lista de moradores e obrigatoria.");
        List<Morador> copia = new ArrayList<>();
        for (Morador morador : moradores) {
            adicionarSemDuplicacao(copia, morador);
        }
        return copia;
    }

    protected static void adicionarSemDuplicacao(List<Morador> destino, Morador morador) {
        Objects.requireNonNull(morador, "Morador nao pode ser nulo.");
        for (Morador anterior : destino) {
            if (anterior == morador || (morador.getId() > 0 && anterior.getId() == morador.getId())) {
                throw new IllegalArgumentException("Morador duplicado na divisao da despesa.");
            }
        }
        destino.add(morador);
    }
}
