package br.com.splitshare.dao;

import br.com.splitshare.model.Morador;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MoradorDAO {
    private static final String SELECT = "SELECT id_morador, nome, email, telefone_celular FROM morador";
    private final ConexaoJdbc banco;

    public MoradorDAO(ConexaoJdbc banco) {
        this.banco = banco;
    }

    public int inserir(Morador morador) throws SQLException {
        if (morador.getId() != 0) {
            throw new IllegalArgumentException("Informe um morador sem ID para cadastrar.");
        }
        int id = banco.emTransacao(conexao -> {
            String sql = "INSERT INTO morador (nome, email, telefone_celular) VALUES (?, ?, ?)";
            try (PreparedStatement comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preencher(comando, morador);
                comando.executeUpdate();
                return ConexaoJdbc.lerIdGerado(comando);
            }
        });
        morador.setId(id);
        return id;
    }

    public Optional<Morador> buscarPorId(int id) throws SQLException {
        exigirId(id);
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SELECT + " WHERE id_morador = ?")) {
            comando.setInt(1, id);
            try (ResultSet linhas = comando.executeQuery()) {
                return linhas.next() ? Optional.of(lerMorador(linhas)) : Optional.empty();
            }
        }
    }

    public List<Morador> listar() throws SQLException {
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(SELECT + " ORDER BY id_morador");
             ResultSet linhas = comando.executeQuery()) {
            List<Morador> moradores = new ArrayList<>();
            while (linhas.next()) {
                moradores.add(lerMorador(linhas));
            }
            return moradores;
        }
    }

    public boolean atualizar(Morador morador) throws SQLException {
        exigirId(morador.getId());
        String sql = "UPDATE morador SET nome = ?, email = ?, telefone_celular = ? WHERE id_morador = ?";
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            preencher(comando, morador);
            comando.setInt(4, morador.getId());
            return comando.executeUpdate() == 1;
        }
    }

    public boolean excluir(int id) throws SQLException {
        exigirId(id);
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement("DELETE FROM morador WHERE id_morador = ?")) {
            comando.setInt(1, id);
            return comando.executeUpdate() == 1;
        }
    }

    static Morador lerMorador(ResultSet linha) throws SQLException {
        return new Morador(linha.getInt("id_morador"), linha.getString("nome"),
                linha.getString("email"), linha.getString("telefone_celular"));
    }

    static void exigirId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Informe um ID persistido.");
        }
    }

    private static void preencher(PreparedStatement comando, Morador morador) throws SQLException {
        comando.setString(1, morador.getNome());
        comando.setString(2, morador.getEmail());
        comando.setString(3, morador.getTelefone());
    }
}
