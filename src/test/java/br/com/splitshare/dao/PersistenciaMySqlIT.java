package br.com.splitshare.dao;

import br.com.splitshare.model.Despesa;
import br.com.splitshare.model.DespesaFixa;
import br.com.splitshare.model.DespesaVariavel;
import br.com.splitshare.model.Morador;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class PersistenciaMySqlIT {
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 11, 10);
    private ConexaoJdbc banco;
    private MoradorDAO moradores;
    private DespesaDAO despesas;
    @TempDir
    Path temporarios;

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

    @Test
    void menuCompletaCrudEConservaDadosEntreProcessos() throws Exception {
        String cadastro = executarMenu("""
                1
                Ana D'Ávila
                ana@example.invalid
                44999990001
                1
                Bia
                bia@example.invalid
                44999990002
                1
                Leo
                leo@example.invalid
                44999990003
                2
                0
                """);
        assertTrue(cadastro.contains("Ana D'Ávila"), cadastro);
        List<Morador> lista = moradores.listar();
        assertEquals(3, lista.size());
        int ana = lista.get(0).getId();
        int bia = lista.get(1).getId();
        int leo = lista.get(2).getId();

        String criacao = executarMenu("""
                5
                Aluguel
                100,00
                10/11/2026
                6
                Mercado d'Ávila
                150.00
                12/11/2026
                %d,%d
                0
                """.formatted(leo, ana));
        assertTrue(criacao.contains("parcela R$ 33,34"), criacao);
        assertTrue(criacao.contains("parcela R$ 75,00"), criacao);
        List<Despesa> salvas = despesas.listar();
        assertEquals(2, salvas.size());
        int fixa = salvas.get(0).getId();
        int variavel = salvas.get(1).getId();
        assertEquals(List.of(ana, leo), ids(salvas.get(1)));

        String edicao = executarMenu("""
                3
                %d
                Ana Maria
                ana.maria@example.invalid
                44999990009
                8
                %d
                Aluguel ajustado
                120,00
                15/11/2026
                8
                %d
                Mercado ajustado
                180,00
                16/11/2026
                %d,%d
                7
                0
                """.formatted(ana, fixa, variavel, bia, ana));
        assertTrue(edicao.contains("Despesa atualizada."), edicao);
        assertTrue(edicao.contains("parcela R$ 90,00"), edicao);
        assertTrue(edicao.contains("parcela R$ 40,00"), edicao);
        assertEquals("Ana Maria", moradores.buscarPorId(ana).orElseThrow().getNome());
        assertEquals("44999990009", moradores.buscarPorId(ana).orElseThrow().getTelefone());
        assertEquals(List.of(ana, bia, leo), ids(despesas.buscarPorId(fixa).orElseThrow()));
        assertEquals(List.of(ana, bia), ids(despesas.buscarPorId(variavel).orElseThrow()));
        assertEquals(LocalDate.of(2026, 11, 16), despesas.buscarPorId(variavel).orElseThrow().getDataVencimento());

        String consulta = executarMenu("2\n7\n0\n");
        assertTrue(consulta.contains("Ana Maria | ana.maria@example.invalid"), consulta);
        assertTrue(consulta.contains("Mercado ajustado | Variavel | vencimento 16/11/2026 | total R$ 180,00"), consulta);

        String exclusao = executarMenu("""
                9
                %d
                não
                4
                %d
                SIM
                9
                %d
                SIM
                9
                %d
                SIM
                4
                %d
                SIM
                7
                0
                """.formatted(variavel, ana, variavel, fixa, ana));
        assertTrue(exclusao.contains("Exclusão cancelada."), exclusao);
        assertTrue(exclusao.contains("O morador participa de despesas."), exclusao);
        assertTrue(exclusao.contains("Nenhuma despesa cadastrada."), exclusao);
        assertTrue(despesas.listar().isEmpty());
        assertTrue(moradores.buscarPorId(ana).isEmpty());
        assertEquals(0, contarVinculos(fixa));
        assertEquals(0, contarVinculos(variavel));
    }

    @Test
    void menuRejeitaEntradasInvalidasSemGravarEContinuaFuncionando() throws Exception {
        Morador ana = novoMorador("Ana", "ana");
        String saida = executarMenu("""
                opção errada
                3
                abc
                3
                2147483647
                5
                Conta
                1,001
                5
                Conta
                10,00
                31/02/2026
                6
                Conta
                10,00
                10/11/2026
                %d,%d
                6
                Conta
                10,00
                10/11/2026
                2147483647
                1
                Duplicada
                ana@example.invalid
                44999990001
                1
                Outra
                email-invalido
                44999990001
                2
                0
                """.formatted(ana.getId(), ana.getId()));
        assertTrue(saida.contains("Opção inválida"), saida);
        assertTrue(saida.contains("ID inteiro positivo"), saida);
        assertTrue(saida.contains("Morador não encontrado"), saida);
        assertTrue(saida.contains("data válida"), saida);
        assertTrue(saida.contains("mais de uma vez"), saida);
        assertTrue(saida.contains("Já existe um morador com esse e-mail"), saida);
        assertTrue(saida.contains("Até logo!"), saida);
        assertTrue(despesas.listar().isEmpty());
        assertEquals(1, moradores.listar().size());
    }

    @Test
    void menuCancelaEdicaoEAceitaEnterSemAlterarValores() throws Exception {
        Morador ana = novoMorador("Ana", "ana");
        int id = despesas.inserirVariavel(variavel(ana));
        String saida = executarMenu("""
                3
                %d
                Nome abandonado
                /cancelar
                8
                %d




                0
                """.formatted(ana.getId(), id));
        assertTrue(saida.contains("Operação cancelada."), saida);
        assertTrue(saida.contains("Despesa atualizada."), saida);
        assertEquals("Ana", moradores.buscarPorId(ana.getId()).orElseThrow().getNome());
        assertEquals(new BigDecimal("150.00"), despesas.buscarPorId(id).orElseThrow().getValorTotal());
        assertEquals(List.of(ana.getId()), ids(despesas.buscarPorId(id).orElseThrow()));
        String interrompida = executarMenu("1\nCadastro incompleto\n");
        assertTrue(interrompida.contains("Entrada encerrada"), interrompida);
        assertEquals(1, moradores.listar().size());
    }

    @Test
    void menuRespeitaLimiteDeParticipantesERateioExato() throws Exception {
        List<Morador> lista = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            lista.add(novoMorador("Participante " + i, "p" + i));
        }
        String quinze = lista.subList(0, 15).stream().map(m -> String.valueOf(m.getId())).collect(Collectors.joining(","));
        String dezesseis = quinze + "," + lista.get(15).getId();
        String saida = executarMenu("""
                6
                Divisão em centavos
                1,00
                10/11/2026
                %s
                6
                Participantes em excesso
                16,00
                10/11/2026
                %s
                0
                """.formatted(quinze, dezesseis));
        assertTrue(saida.contains("no máximo 15 moradores"), saida);
        assertEquals(1, despesas.listar().size());
        Despesa salva = despesas.listar().get(0);
        assertEquals(15, salva.getMoradoresEnvolvidos().size());
        assertEquals(new BigDecimal("1.00"), salva.calcularRateiosIndividuais().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private String executarMenu(String linhas) throws Exception {
        Path log = Files.createTempFile(temporarios, "menu-", ".txt");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8",
                "-jar", "target/splitshare.jar").redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("SPLITSHARE_DB_URL", System.getenv("SPLITSHARE_TEST_DB_URL"));
        builder.environment().put("SPLITSHARE_DB_USER", System.getenv("SPLITSHARE_TEST_DB_USER"));
        builder.environment().put("SPLITSHARE_DB_PASSWORD", System.getenv("SPLITSHARE_TEST_DB_PASSWORD"));
        Process processo = builder.start();
        try {
            try (OutputStreamWriter entrada = new OutputStreamWriter(processo.getOutputStream(), StandardCharsets.UTF_8)) {
                entrada.write(linhas);
            }
            assertTrue(processo.waitFor(30, TimeUnit.SECONDS), "O menu não encerrou em 30 segundos.");
            String saida = Files.readString(log, StandardCharsets.UTF_8);
            assertEquals(0, processo.exitValue(), saida);
            return saida;
        } finally {
            if (processo.isAlive()) {
                processo.destroyForcibly();
            }
        }
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
