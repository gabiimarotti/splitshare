package br.com.splitshare.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Abre uma conexão nova por operação. A configuração vem do ambiente. */
public final class ConexaoJdbc {
    private final String url;
    private final String usuario;
    private final String senha;

    private ConexaoJdbc(String prefixo) {
        url = lerVariavel(prefixo + "_URL", false);
        usuario = lerVariavel(prefixo + "_USER", false);
        senha = lerVariavel(prefixo + "_PASSWORD", true);
    }

    public static ConexaoJdbc doAmbiente() {
        return new ConexaoJdbc("SPLITSHARE_DB");
    }

    Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    <T> T emTransacao(Operacao<T> operacao) throws SQLException {
        try (Connection conexao = abrir()) {
            conexao.setAutoCommit(false);
            try {
                T resultado = operacao.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException falha) {
                try {
                    conexao.rollback();
                } catch (SQLException falhaNoRollback) {
                    falha.addSuppressed(falhaNoRollback);
                }
                throw falha;
            }
        }
    }

    static int lerIdGerado(PreparedStatement comando) throws SQLException {
        try (ResultSet chaves = comando.getGeneratedKeys()) {
            if (!chaves.next()) {
                throw new SQLException("O banco não retornou o ID do registro.");
            }
            return chaves.getInt(1);
        }
    }

    private static String lerVariavel(String nome, boolean permiteVazio) {
        String valor = System.getenv(nome);
        if (valor == null || (!permiteVazio && valor.isBlank())) {
            throw new IllegalStateException("Configure a variável de ambiente " + nome + ".");
        }
        return valor;
    }

    @FunctionalInterface
    interface Operacao<T> {
        T executar(Connection conexao) throws SQLException;
    }
}
