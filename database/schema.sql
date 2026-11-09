CREATE DATABASE IF NOT EXISTS splitshare
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE splitshare;

CREATE TABLE IF NOT EXISTS morador (
    id_morador INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    telefone_celular VARCHAR(20) NOT NULL,

    CONSTRAINT pk_morador PRIMARY KEY (id_morador),
    CONSTRAINT uq_morador_email UNIQUE (email),
    CONSTRAINT chk_morador_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0)
) ENGINE = InnoDB;

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
