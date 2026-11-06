package br.com.splitshare.dao;

import br.com.splitshare.model.Despesa;
import br.com.splitshare.model.DespesaFixa;
import br.com.splitshare.model.DespesaVariavel;
import br.com.splitshare.model.Morador;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersistenciaMySqlIT {
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 11, 10);
    private ConexaoJdbc banco;
    private MoradorDAO moradores;
    private DespesaDAO despesas;

    @BeforeEach
    void preparar() throws SQLException {
        banco = ConexaoJdbc.doAmbienteDeTeste();
        limparBancoDeTeste();
        moradores = new MoradorDAO(banco);
        despesas = new DespesaDAO(banco);
    }

    @AfterEach
    void limpar() throws SQLException {
        if (banco != null) {
            limparBancoDeTeste();
        }
    }

    @Test
    void gerenciaMoradoresComParametrosEEmailUnico() throws SQLException {
        Morador ana = novoMorador("Ana D'Ávila", "ana");
        assertTrue(ana.getId() > 0);
        assertEquals(ana.getNome(), moradores.buscarPorId(ana.getId()).orElseThrow().getNome());
        assertEquals("44999990001", moradores.listar().get(0).getTelefone());

        Morador duplicado = new Morador("Outra Ana", ana.getEmail(), ana.getTelefone());
        assertThrows(SQLException.class, () -> moradores.inserir(duplicado));
        assertEquals(0, duplicado.getId());
        assertEquals(1, moradores.listar().size());

        ana.setNome("Ana Maria");
        ana.setEmail("ana.maria@example.invalid");
        ana.setTelefone("44999990002");
        assertTrue(moradores.atualizar(ana));
        Morador salva = moradores.buscarPorId(ana.getId()).orElseThrow();
        assertEquals(ana.getNome(), salva.getNome());
        assertEquals(ana.getEmail(), salva.getEmail());
        assertEquals(ana.getTelefone(), salva.getTelefone());
        assertTrue(moradores.excluir(ana.getId()));
        assertFalse(moradores.atualizar(ana));
        assertFalse(moradores.excluir(ana.getId()));
        assertTrue(moradores.buscarPorId(ana.getId()).isEmpty());
    }

    @Test
    void fixaSelecionaTodosEConservaParticipantesNaEdicao() throws SQLException {
        assertThrows(IllegalArgumentException.class,
                () -> despesas.inserirFixa("Sem moradores", BigDecimal.ONE, VENCIMENTO));
        assertTrue(despesas.listar().isEmpty());

        Morador ana = novoMorador("Ana", "ana");
        Morador bia = novoMorador("Bia", "bia");
        Morador leo = novoMorador("Leo", "leo");
        DespesaFixa fixa = despesas.inserirFixa("Aluguel", new BigDecimal("100.00"), VENCIMENTO);
        assertEquals(List.of(ana.getId(), bia.getId(), leo.getId()), ids(fixa));

        novoMorador("Lia", "lia");
        DespesaFixa edicao = new DespesaFixa(fixa.getId(), "Aluguel de novembro",
                new BigDecimal("101.00"), VENCIMENTO.plusDays(1), moradores.listar());
        assertTrue(despesas.atualizar(edicao));

        Despesa salva = assertInstanceOf(DespesaFixa.class, despesas.buscarPorId(fixa.getId()).orElseThrow());
        assertEquals(ids(fixa), ids(salva));
        assertEquals(edicao.getDescricao(), salva.getDescricao());
        assertEquals(edicao.getDataVencimento(), salva.getDataVencimento());
        assertEquals(edicao.getValorTotal(), salva.getValorTotal());
        assertEquals(List.of(new BigDecimal("33.67"), new BigDecimal("33.67"), new BigDecimal("33.66")),
                List.copyOf(salva.calcularRateiosIndividuais().values()));
    }

    @Test
    void listaEditaEExcluiDespesasComIntegridadeReferencial() throws SQLException {
        Morador ana = novoMorador("Ana", "ana");
        Morador bia = novoMorador("Bia", "bia");
        Morador leo = novoMorador("Leo", "leo");
        DespesaFixa fixa = despesas.inserirFixa("Aluguel", new BigDecimal("900.00"), VENCIMENTO);
        DespesaVariavel variavel = variavel(ana, leo);
        int id = despesas.inserirVariavel(variavel);
        assertEquals(id, variavel.getId());

        List<Despesa> salvas = despesas.listar();
        assertEquals(List.of(fixa.getId(), id), salvas.stream().map(Despesa::getId).toList());
        assertInstanceOf(DespesaVariavel.class, salvas.get(1));
        assertEquals(List.of(ana.getId(), leo.getId()), ids(salvas.get(1)));
        assertEquals(new BigDecimal("75.00"), salvas.get(1).calcularRateio());

        variavel.removerMorador(leo.getId());
        variavel.adicionarMorador(bia);
        variavel.setValorTotal(new BigDecimal("180.00"));
        assertTrue(despesas.atualizar(variavel));
        Despesa editada = despesas.buscarPorId(id).orElseThrow();
        assertEquals(List.of(ana.getId(), bia.getId()), ids(editada));
        assertEquals(new BigDecimal("90.00"), editada.calcularRateio());

        assertThrows(SQLException.class, () -> moradores.excluir(ana.getId()));
        assertTrue(despesas.excluir(id));
        assertEquals(0, contarVinculos(id));
        assertTrue(despesas.buscarPorId(id).isEmpty());
        assertFalse(despesas.excluir(id));
        assertFalse(despesas.atualizar(variavel));
        assertTrue(despesas.excluir(fixa.getId()));
        assertTrue(moradores.excluir(ana.getId()));
    }

    @Test
    void desfazInclusaoEEdicaoSeUmVinculoFalhar() throws SQLException {
        Morador ana = novoMorador("Ana", "ana");
        Morador bia = novoMorador("Bia", "bia");
        Morador removido = novoMorador("Removido", "removido");
        assertTrue(moradores.excluir(removido.getId()));

        DespesaVariavel invalida = variavel(ana, removido);
        assertThrows(SQLException.class, () -> despesas.inserirVariavel(invalida));
        assertEquals(0, invalida.getId());
        assertTrue(despesas.listar().isEmpty());

        DespesaVariavel original = variavel(ana, bia);
        int id = despesas.inserirVariavel(original);
        invalida.setId(id);
        invalida.setDescricao("Edicao que deve falhar");
        invalida.setValorTotal(new BigDecimal("1.00"));
        assertThrows(SQLException.class, () -> despesas.atualizar(invalida));

        Despesa salva = despesas.buscarPorId(id).orElseThrow();
        assertEquals(original.getDescricao(), salva.getDescricao());
        assertEquals(original.getValorTotal(), salva.getValorTotal());
        assertEquals(ids(original), ids(salva));
        assertEquals(2, contarVinculos(id));
    }

    @Test
    void exigeParticipantesPersistidosENaoConverteTipo() throws SQLException {
        Morador ana = novoMorador("Ana", "ana");
        Morador semId = new Morador("Outro", "outro@example.invalid", "44999990001");
        assertThrows(IllegalArgumentException.class, () -> despesas.inserirVariavel(variavel()));
        assertThrows(IllegalArgumentException.class, () -> despesas.inserirVariavel(variavel(ana, semId)));
        assertTrue(despesas.listar().isEmpty());

        DespesaFixa fixa = despesas.inserirFixa("Aluguel", BigDecimal.TEN, VENCIMENTO);
        DespesaVariavel edicao = variavel(ana);
        edicao.setId(fixa.getId());
        assertThrows(IllegalArgumentException.class, () -> despesas.atualizar(edicao));
        assertInstanceOf(DespesaFixa.class, despesas.buscarPorId(fixa.getId()).orElseThrow());
    }

    private void limparBancoDeTeste() throws SQLException {
        banco.emTransacao(conexao -> {
            if (!"splitshare_test".equals(conexao.getCatalog())) {
                throw new IllegalStateException("A suíte exige um banco descartável chamado splitshare_test.");
            }
            try (PreparedStatement despesas = conexao.prepareStatement("DELETE FROM despesa");
                 PreparedStatement moradores = conexao.prepareStatement("DELETE FROM morador")) {
                despesas.executeUpdate();
                moradores.executeUpdate();
            }
            return null;
        });
    }

    private int contarVinculos(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM despesa_morador WHERE id_despesa = ?";
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, id);
            try (ResultSet linha = comando.executeQuery()) {
                linha.next();
                return linha.getInt(1);
            }
        }
    }

    private Morador novoMorador(String nome, String email) throws SQLException {
        Morador morador = new Morador(nome, email + "@example.invalid", "44999990001");
        moradores.inserir(morador);
        return morador;
    }

    private static DespesaVariavel variavel(Morador... participantes) {
        return new DespesaVariavel(0, "Mercado", new BigDecimal("150.00"), VENCIMENTO, List.of(participantes));
    }

    private static List<Integer> ids(Despesa despesa) {
        return despesa.getMoradoresEnvolvidos().stream().map(Morador::getId).toList();
    }
}
