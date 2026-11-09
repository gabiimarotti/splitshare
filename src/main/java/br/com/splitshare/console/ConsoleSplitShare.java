package br.com.splitshare.console;

import br.com.splitshare.dao.DespesaDAO;
import br.com.splitshare.dao.MoradorDAO;
import br.com.splitshare.model.Despesa;
import br.com.splitshare.model.DespesaVariavel;
import br.com.splitshare.model.Morador;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConsoleSplitShare {
    private final MoradorDAO moradores;
    private final DespesaDAO despesas;
    private final EntradaConsole entrada;
    private final PrintWriter saida;

    public ConsoleSplitShare(MoradorDAO moradores, DespesaDAO despesas, Scanner leitor, PrintWriter saida) {
        this.moradores = moradores;
        this.despesas = despesas;
        this.entrada = new EntradaConsole(leitor, saida);
        this.saida = saida;
    }

    public void executar() {
        saida.println("SplitShare — moradores e despesas compartilhadas");
        saida.println("Digite /cancelar em qualquer campo para voltar ao menu.");
        while (true) {
            saida.println("""

                    1 - Cadastrar morador
                    2 - Listar moradores
                    3 - Editar morador
                    4 - Excluir morador
                    5 - Cadastrar despesa fixa
                    6 - Cadastrar despesa variável
                    7 - Listar despesas e rateios
                    8 - Editar despesa
                    9 - Excluir despesa
                    0 - Sair
                    """);
            try {
                switch (entrada.texto("Opção")) {
                    case "1" -> cadastrarMorador();
                    case "2" -> exibirMoradores(moradores.listar());
                    case "3" -> editarMorador();
                    case "4" -> excluirMorador();
                    case "5" -> cadastrarFixa();
                    case "6" -> cadastrarVariavel();
                    case "7" -> listarDespesas();
                    case "8" -> editarDespesa();
                    case "9" -> excluirDespesa();
                    case "0" -> {
                        saida.println("Até logo!");
                        saida.flush();
                        return;
                    }
                    default -> saida.println("Opção inválida. Escolha um número de 0 a 9.");
                }
            } catch (EntradaConsole.OperacaoCancelada cancelada) {
                saida.println("Operação cancelada.");
            } catch (NoSuchElementException fimDaEntrada) {
                saida.println("Entrada encerrada. Até logo!");
                saida.flush();
                return;
            } catch (IllegalArgumentException | IllegalStateException falha) {
                saida.println("Confira os dados. " + falha.getMessage());
            } catch (SQLException falha) {
                saida.println(mensagemBanco(falha));
            }
            saida.flush();
        }
    }

    private void cadastrarMorador() throws SQLException {
        Morador morador = new Morador(entrada.texto("Nome"), entrada.texto("E-mail"), entrada.texto("Celular"));
        moradores.inserir(morador);
        saida.println("Morador cadastrado com ID " + morador.getId() + ".");
    }

    private void editarMorador() throws SQLException {
        Morador morador = buscarMorador();
        morador.setNome(entrada.editarTexto("Nome", morador.getNome()));
        morador.setEmail(entrada.editarTexto("E-mail", morador.getEmail()));
        morador.setTelefone(entrada.editarTexto("Celular", morador.getTelefone()));
        if (moradores.atualizar(morador)) {
            saida.println("Morador atualizado.");
        } else {
            saida.println("Morador não encontrado. Atualize a listagem.");
        }
    }

    private void excluirMorador() throws SQLException {
        Morador morador = buscarMorador();
        if (confirmarExclusao("morador " + morador.getId() + " — " + morador.getNome())) {
            saida.println(moradores.excluir(morador.getId()) ? "Morador excluído." : "Morador não encontrado.");
        }
    }

    private Morador buscarMorador() throws SQLException {
        return moradores.buscarPorId(entrada.id("ID do morador"))
                .orElseThrow(() -> new IllegalArgumentException("Morador não encontrado."));
    }

    private void exibirMoradores(List<Morador> lista) {
        if (lista.isEmpty()) {
            saida.println("Nenhum morador cadastrado.");
        }
        for (Morador morador : lista) {
            saida.printf("%d | %s | %s | %s%n", morador.getId(), morador.getNome(),
                    morador.getEmail(), morador.getTelefone());
        }
    }

    private void cadastrarFixa() throws SQLException {
        String descricao = entrada.texto("Descrição");
        BigDecimal valor = EntradaConsole.converterValor(entrada.texto("Valor total (ex. 900,00)"));
        LocalDate vencimento = EntradaConsole.converterData(entrada.texto("Vencimento (dd/mm/aaaa)"));
        Despesa salva = despesas.inserirFixa(descricao, valor, vencimento);
        saida.println("Despesa cadastrada com ID " + salva.getId() + ".");
        exibirDespesa(salva);
    }

    private void cadastrarVariavel() throws SQLException {
        String descricao = entrada.texto("Descrição");
        BigDecimal valor = EntradaConsole.converterValor(entrada.texto("Valor total (ex. 150,00)"));
        LocalDate vencimento = EntradaConsole.converterData(entrada.texto("Vencimento (dd/mm/aaaa)"));
        List<Morador> participantes = selecionarMoradores(List.of());
        DespesaVariavel despesa = new DespesaVariavel(0, descricao, valor, vencimento, participantes);
        despesas.inserirVariavel(despesa);
        saida.println("Despesa cadastrada com ID " + despesa.getId() + ".");

        exibirDespesa(despesas.buscarPorId(despesa.getId()).orElseThrow(
                () -> new IllegalStateException("Despesa removida por outra sessão. Atualize a listagem.")));
    }

    private void listarDespesas() throws SQLException {
        List<Despesa> lista = despesas.listar();
        if (lista.isEmpty()) {
            saida.println("Nenhuma despesa cadastrada.");
        }
        for (Despesa despesa : lista) {
            exibirDespesa(despesa);
        }
    }

    private void exibirDespesa(Despesa despesa) {
        saida.printf("%d | %s | %s | vencimento %s | total R$ %s%n", despesa.getId(),
                despesa.getDescricao(), despesa.getTipoDespesa(), despesa.getDataVencimento().format(EntradaConsole.DATA),
                dinheiro(despesa.getValorTotal()));
        despesa.calcularRateiosIndividuais().forEach((morador, valor) ->
                saida.printf("  %d - %s | parcela R$ %s%n", morador.getId(), morador.getNome(), dinheiro(valor)));
    }

    private void editarDespesa() throws SQLException {
        Despesa despesa = buscarDespesa();
        exibirDespesa(despesa);
        String descricao = entrada.editarTexto("Descrição", despesa.getDescricao());
        BigDecimal valor = EntradaConsole.converterValor(entrada.editarTexto("Valor total", dinheiro(despesa.getValorTotal())));
        LocalDate vencimento = EntradaConsole.converterData(entrada.editarTexto("Vencimento (dd/mm/aaaa)",
                despesa.getDataVencimento().format(EntradaConsole.DATA)));
        if (despesa instanceof DespesaVariavel) {
            despesa = new DespesaVariavel(despesa.getId(), descricao, valor, vencimento,
                    selecionarMoradores(despesa.getMoradoresEnvolvidos()));
        } else {
            despesa.setDescricao(descricao);
            despesa.setValorTotal(valor);
            despesa.setDataVencimento(vencimento);
        }
        if (despesas.atualizar(despesa)) {
            saida.println("Despesa atualizada. Consulte a opção 7 para ver o novo rateio.");
        } else {
            saida.println("Despesa não encontrada. Atualize a listagem.");
        }
    }

    private void excluirDespesa() throws SQLException {
        Despesa despesa = buscarDespesa();
        if (confirmarExclusao("despesa " + despesa.getId() + " — " + despesa.getDescricao())) {
            saida.println(despesas.excluir(despesa.getId()) ? "Despesa excluída." : "Despesa não encontrada.");
        }
    }

    private Despesa buscarDespesa() throws SQLException {
        return despesas.buscarPorId(entrada.id("ID da despesa"))
                .orElseThrow(() -> new IllegalArgumentException("Despesa não encontrada."));
    }

    private List<Morador> selecionarMoradores(List<Morador> atuais) throws SQLException {
        List<Morador> cadastrados = moradores.listar();
        exibirMoradores(cadastrados);
        if (cadastrados.isEmpty()) {
            throw new IllegalArgumentException("Cadastre ao menos um morador antes da despesa.");
        }
        String pergunta = "IDs dos participantes separados por vírgula (1 a 15 moradores)";
        String texto;
        if (atuais.isEmpty()) {
            texto = entrada.texto(pergunta);
        } else {
            String idsAtuais = atuais.stream().map(m -> String.valueOf(m.getId())).collect(Collectors.joining(","));
            texto = entrada.editarTexto(pergunta, idsAtuais);
        }
        String[] ids = texto.split(",", -1);
        if (ids.length > DespesaVariavel.MAX_PARTICIPANTES) {
            throw new IllegalArgumentException("Selecione no máximo 15 moradores.");
        }
        Set<Integer> unicos = new HashSet<>();
        List<Morador> selecionados = new ArrayList<>();
        for (String item : ids) {
            int id = EntradaConsole.converterId(item);
            if (!unicos.add(id)) {
                throw new IllegalArgumentException("O morador " + id + " foi selecionado mais de uma vez.");
            }
            selecionados.add(cadastrados.stream().filter(m -> m.getId() == id).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Morador " + id + " não encontrado.")));
        }
        return selecionados;
    }

    private boolean confirmarExclusao(String registro) {
        if (entrada.texto("Excluir " + registro + "? Digite SIM para confirmar").equalsIgnoreCase("sim")) {
            return true;
        }
        saida.println("Exclusão cancelada.");
        return false;
    }

    private static String dinheiro(BigDecimal valor) {
        return valor.setScale(2).toPlainString().replace('.', ',');
    }

    static String mensagemBanco(SQLException falha) {
        return switch (falha.getErrorCode()) {
            case 1062 -> "Já existe um morador com esse e-mail. Informe outro e-mail.";
            case 1217, 1451 -> "O morador participa de despesas. Resolva os vínculos antes de excluí-lo.";
            case 1216, 1452 -> "Um participante deixou de existir. Atualize a lista de moradores e selecione novamente.";
            case 1045 -> "Acesso ao MySQL recusado. Confira o usuário, a senha e as permissões configuradas.";
            case 1049, 1146 -> "Banco ou tabelas ausentes. Importe database/schema.sql no MySQL configurado.";
            default -> "Falha ao acessar o banco. Confira a conexão e as permissões. Consulte os registros antes de repetir a operação.";
        };
    }
}
