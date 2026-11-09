package br.com.splitshare.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DespesaFixa extends Despesa {
    private final List<Morador> moradoresEnvolvidos;

    public DespesaFixa(int id, String descricao, BigDecimal valorTotal,
                       LocalDate dataVencimento, Collection<Morador> todosOsMoradores) {
        super(id, descricao, valorTotal, dataVencimento);
        moradoresEnvolvidos = copiarParticipantes(todosOsMoradores);
        if (moradoresEnvolvidos.isEmpty()) {
            throw new IllegalArgumentException("Despesa fixa exige pelo menos um morador.");
        }
    }

    public DespesaFixa(String descricao, BigDecimal valorTotal,
                       LocalDate dataVencimento, Collection<Morador> todosOsMoradores) {
        this(0, descricao, valorTotal, dataVencimento, todosOsMoradores);
    }

    @Override
    public BigDecimal calcularRateio() {
        return calcularCotaBase(moradoresEnvolvidos.size());
    }

    @Override
    public String getTipoDespesa() { return "Fixa"; }

    @Override
    public List<Morador> getMoradoresEnvolvidos() {
        return Collections.unmodifiableList(moradoresEnvolvidos);
    }
}
