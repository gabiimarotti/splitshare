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
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DespesaDAO {
    private static final String SELECT = """
            SELECT d.id_despesa, d.descricao, d.valor_total, d.data_vencimento, d.tipo_despesa,
                   m.id_morador, m.nome, m.email, m.telefone_celular
            FROM despesa d
            LEFT JOIN despesa_morador dm ON dm.id_despesa = d.id_despesa
            LEFT JOIN morador m ON m.id_morador = dm.id_morador
            """;
    private static final String ORDEM = " ORDER BY d.id_despesa, m.id_morador";

    private final ConexaoJdbc banco;

    public DespesaDAO(ConexaoJdbc banco) {
        this.banco = banco;
    }

    public DespesaFixa inserirFixa(String descricao, BigDecimal valor, LocalDate vencimento) throws SQLException {
        return banco.emTransacao(conexao -> {
            String sql = """
                    SELECT id_morador, nome, email, telefone_celular FROM morador
                    ORDER BY id_morador LOCK IN SHARE MODE
                    """;
            List<Morador> moradores = new ArrayList<>();
            try (PreparedStatement comando = conexao.prepareStatement(sql);
                 ResultSet linhas = comando.executeQuery()) {
                while (linhas.next()) {
                    moradores.add(MoradorDAO.lerMorador(linhas));
                }
            }
            DespesaFixa despesa = new DespesaFixa(descricao, valor, vencimento, moradores);
            despesa.setId(inserir(conexao, despesa, idsParticipantes(despesa)));
            return despesa;
        });
    }

    public int inserirVariavel(DespesaVariavel despesa) throws SQLException {
        if (despesa.getId() != 0) {
            throw new IllegalArgumentException("Informe uma despesa sem ID para cadastrar.");
        }
        List<Integer> participantes = idsParticipantes(despesa);
        int id = banco.emTransacao(conexao -> inserir(conexao, despesa, participantes));
        despesa.setId(id);
        return id;
    }

    public Optional<Despesa> buscarPorId(int id) throws SQLException {
        MoradorDAO.exigirId(id);
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SELECT + " WHERE d.id_despesa = ?" + ORDEM)) {
            comando.setInt(1, id);
            return consultar(comando).stream().findFirst();
        }
    }

    public List<Despesa> listar() throws SQLException {
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SELECT + ORDEM)) {
            return consultar(comando);
        }
    }

    public boolean atualizar(Despesa despesa) throws SQLException {
        int id = despesa.getId();
        MoradorDAO.exigirId(id);
        return banco.emTransacao(conexao -> {
            String tipoSalvo = bloquearTipo(conexao, id);
            if (tipoSalvo == null) {
                return false;
            }
            if (!tipoSalvo.equals(despesa.getTipoDespesa())) {
                throw new IllegalArgumentException("O tipo da despesa cadastrada não pode ser alterado.");
            }

            List<Integer> participantes = List.of();
            boolean variavel = tipoSalvo.equals("Variavel");
            if (variavel) {
                participantes = idsParticipantes(despesa);
            }
            String sql = "UPDATE despesa SET descricao = ?, valor_total = ?, data_vencimento = ? WHERE id_despesa = ?";
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                preencher(comando, despesa);
                comando.setInt(4, id);
                comando.executeUpdate();
            }
            if (variavel) {
                try (PreparedStatement comando = conexao.prepareStatement(
                        "DELETE FROM despesa_morador WHERE id_despesa = ?")) {
                    comando.setInt(1, id);
                    comando.executeUpdate();
                }
                vincular(conexao, id, participantes);
            }
            return true;
        });
    }

    public boolean excluir(int id) throws SQLException {
        MoradorDAO.exigirId(id);
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement("DELETE FROM despesa WHERE id_despesa = ?")) {
            comando.setInt(1, id);
            return comando.executeUpdate() == 1;
        }
    }

    private static int inserir(Connection conexao, Despesa despesa, List<Integer> participantes) throws SQLException {
        String sql = """
                INSERT INTO despesa (descricao, valor_total, data_vencimento, tipo_despesa) VALUES (?, ?, ?, ?)
                """;
        int id;
        try (PreparedStatement comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(comando, despesa);
            comando.setString(4, despesa.getTipoDespesa());
            comando.executeUpdate();
            id = ConexaoJdbc.lerIdGerado(comando);
        }
        vincular(conexao, id, participantes);
        return id;
    }

    private static void preencher(PreparedStatement comando, Despesa despesa) throws SQLException {
        comando.setString(1, despesa.getDescricao());
        comando.setBigDecimal(2, despesa.getValorTotal());
        comando.setObject(3, despesa.getDataVencimento());
    }

    private static List<Integer> idsParticipantes(Despesa despesa) {
        List<Integer> ids = despesa.getMoradoresEnvolvidos().stream().map(Morador::getId).sorted().toList();
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("A despesa exige pelo menos um participante.");
        }
        if (ids.get(0) <= 0 || ids.stream().distinct().count() != ids.size()) {
            throw new IllegalArgumentException("Selecione moradores cadastrados com IDs distintos.");
        }
        if (despesa instanceof DespesaVariavel && ids.size() > DespesaVariavel.MAX_PARTICIPANTES) {
            throw new IllegalArgumentException("A despesa variável excede o limite de participantes.");
        }
        return ids;
    }

    private static void vincular(Connection conexao, int id, List<Integer> participantes) throws SQLException {
        String sql = "INSERT INTO despesa_morador (id_despesa, id_morador) VALUES (?, ?)";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, id);
            for (int idMorador : participantes) {
                comando.setInt(2, idMorador);
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static String bloquearTipo(Connection conexao, int id) throws SQLException {
        String sql = "SELECT tipo_despesa FROM despesa WHERE id_despesa = ? FOR UPDATE";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, id);
            try (ResultSet linha = comando.executeQuery()) {
                return linha.next() ? linha.getString("tipo_despesa") : null;
            }
        }
    }

    private static List<Despesa> consultar(PreparedStatement comando) throws SQLException {
        Map<Integer, Registro> registros = new LinkedHashMap<>();
        try (ResultSet linhas = comando.executeQuery()) {
            while (linhas.next()) {
                int id = linhas.getInt("id_despesa");
                Registro registro = registros.get(id);
                if (registro == null) {
                    registro = new Registro(id, linhas.getString("descricao"), linhas.getBigDecimal("valor_total"),
                            linhas.getObject("data_vencimento", LocalDate.class), linhas.getString("tipo_despesa"),
                            new ArrayList<>());
                    registros.put(id, registro);
                }
                if (linhas.getInt("id_morador") != 0) {
                    registro.participantes().add(MoradorDAO.lerMorador(linhas));
                }
            }
        }

        List<Despesa> despesas = new ArrayList<>();
        for (Registro registro : registros.values()) {
            despesas.add(registro.converter());
        }
        return despesas;
    }

    private record Registro(int id, String descricao, BigDecimal valor, LocalDate vencimento,
                            String tipo, List<Morador> participantes) {
        Despesa converter() throws SQLException {
            if (participantes.isEmpty()) {
                throw new SQLException("A despesa " + id + " está sem participantes.");
            }
            return switch (tipo) {
                case "Fixa" -> new DespesaFixa(id, descricao, valor, vencimento, participantes);
                case "Variavel" -> new DespesaVariavel(id, descricao, valor, vencimento, participantes);
                default -> throw new SQLException("Tipo desconhecido na despesa " + id + ".");
            };
        }
    }
}
