package br.com.splitshare.model;

public class Morador {
    private int id;
    private String nome;
    private String email;
    private String telefone;

    public Morador(int id, String nome, String email, String telefone) {
        setId(id);
        setNome(nome);
        setEmail(email);
        setTelefone(telefone);
    }

    public Morador(String nome, String email, String telefone) {
        this(0, nome, email, telefone);
    }

    public int getId() { return id; }

    public void setId(int id) {
        if (id < 0) throw new IllegalArgumentException("O ID do morador nao pode ser negativo.");
        this.id = id;
    }

    public String getNome() { return nome; }

    public void setNome(String nome) {
        this.nome = textoObrigatorio(nome, 100, "nome");
    }

    public String getEmail() { return email; }

    public void setEmail(String email) {
        String emailValidado = textoObrigatorio(email, 100, "e-mail");
        if (!emailValidado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("E-mail invalido.");
        }
        this.email = emailValidado;
    }

    public String getTelefone() { return telefone; }

    public void setTelefone(String telefone) {
        this.telefone = textoObrigatorio(telefone, 20, "telefone");
    }

    static String textoObrigatorio(String valor, int tamanhoMaximo, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo " + campo + " e obrigatorio.");
        }
        String texto = valor.trim();
        if (texto.length() > tamanhoMaximo) {
            throw new IllegalArgumentException("O campo " + campo + " excede " + tamanhoMaximo + " caracteres.");
        }
        return texto;
    }

    @Override
    public String toString() {
        return "Morador{id=" + id + ", nome='" + nome + "'}";
    }
}
