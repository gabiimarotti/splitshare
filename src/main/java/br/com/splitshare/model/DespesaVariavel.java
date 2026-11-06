package br.com.splitshare.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Despesa dividida somente entre os moradores selecionados. */
public class DespesaVariavel extends Despesa {
    private final List<Morador> moradoresEnvolvidos = new ArrayList<>();

    public DespesaVariavel(int id, String descricao, BigDecimal valorTotal,
                           LocalDate dataVencimento, Collection<Morador> selecionados) {
        super(id, descricao, valorTotal, dataVencimento);
        moradoresEnvolvidos.addAll(copiarParticipantes(selecionados));
    }

    public DespesaVariavel(String descricao, BigDecimal valorTotal,
                           LocalDate dataVencimento) {
        this(0, descricao, valorTotal, dataVencimento, List.of());
    }

    public void adicionarMorador(Morador morador) {
        adicionarSemDuplicacao(moradoresEnvolvidos, morador);
    }

    /** Retorna true se um participante foi removido. */
    public boolean removerMorador(int idMorador) {
        if (idMorador <= 0) throw new IllegalArgumentException("Informe um ID valido.");
        return moradoresEnvolvidos.removeIf(morador -> morador.getId() == idMorador);
    }

    public int getQuantidadeAtual() { return moradoresEnvolvidos.size(); }

    @Override
    public BigDecimal calcularRateio() {
        return calcularCotaBase(getQuantidadeAtual());
    }

    @Override
    public String getTipoDespesa() { return "Variavel"; }

    @Override
    public List<Morador> getMoradoresEnvolvidos() {
        return Collections.unmodifiableList(moradoresEnvolvidos);
    }
}
