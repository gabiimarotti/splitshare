-- ============================================================================
-- SplitShare | Estrutura inicial do banco de dados (AEP - 2a entrega)
-- SGBD: MySQL 8.0.16 ou superior
-- DER de referencia: docs/der.png
-- ============================================================================
-- Execute este arquivo antes de iniciar a aplicacao Java.
-- O script nao apaga bancos, tabelas ou dados existentes.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS splitshare
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE splitshare;

-- RF01: moradores (somente os dados pessoais definidos nos requisitos).
CREATE TABLE IF NOT EXISTS morador (
    id_morador INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    telefone_celular VARCHAR(20) NOT NULL,

    CONSTRAINT pk_morador PRIMARY KEY (id_morador),
    CONSTRAINT uq_morador_email UNIQUE (email),
    CONSTRAINT chk_morador_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0)
) ENGINE = InnoDB;

-- RF02, RF03 e RF05: despesas fixas e variaveis.
-- O tipo identifica qual subclasse Java (DespesaFixa/DespesaVariavel)
-- representa a despesa na camada de dominio.
CREATE TABLE IF NOT EXISTS despesa (
    id_despesa INT NOT NULL AUTO_INCREMENT,
    descricao VARCHAR(100) NOT NULL,
    valor_total DECIMAL(10,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    tipo_despesa ENUM('Fixa', 'Variavel') NOT NULL,

    CONSTRAINT pk_despesa PRIMARY KEY (id_despesa),
    CONSTRAINT chk_despesa_descricao CHECK (CHAR_LENGTH(TRIM(descricao)) > 0),
    CONSTRAINT chk_despesa_valor_positivo CHECK (valor_total > 0),

    INDEX idx_despesa_vencimento (data_vencimento),
    INDEX idx_despesa_tipo (tipo_despesa)
) ENGINE = InnoDB;

-- RF02, RF03 e RF04: participantes de cada divisao de despesa.
-- A chave composta impede que o mesmo morador seja associado duas vezes
-- a uma mesma despesa.
CREATE TABLE IF NOT EXISTS despesa_morador (
    id_despesa INT NOT NULL,
    id_morador INT NOT NULL,

    CONSTRAINT pk_despesa_morador PRIMARY KEY (id_despesa, id_morador),
    CONSTRAINT fk_dm_despesa FOREIGN KEY (id_despesa)
        REFERENCES despesa (id_despesa)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_dm_morador FOREIGN KEY (id_morador)
        REFERENCES morador (id_morador)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    INDEX idx_dm_morador (id_morador)
) ENGINE = InnoDB;

-- REGRAS PARA A FUTURA IMPLEMENTACAO JAVA (nao automatizadas neste DDL):
-- 1. Despesa fixa: ao cadastrar, associar TODOS os moradores atuais.
-- 2. Despesa variavel: associar SOMENTE os moradores selecionados.
-- 3. Nao permitir salvar uma despesa sem ao menos um participante.
-- 4. Criar despesa + participantes na mesma transacao (COMMIT/ROLLBACK).
-- 5. Calcular o rateio usando o valor_total e os participantes associados.
--    Tratar a distribuicao dos centavos na aplicacao para que as parcelas
--    somadas sejam exatamente iguais ao valor total da despesa.
-- 6. A exclusao de despesa apaga suas associacoes (CASCADE).
-- 7. A exclusao de morador com despesas vinculadas e bloqueada (RESTRICT)
--    para evitar alteracoes silenciosas no historico dos rateios.
-- 8. Cadastrar novos moradores nao altera participantes de despesas antigas.
