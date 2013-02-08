-- MySQL definition and initialisation script for shop service application
SET CHARACTER SET latin1;
DROP DATABASE IF EXISTS http;
CREATE DATABASE http CHARACTER SET utf8;
USE http;

CREATE TABLE GlobalScope (
	identity BIGINT AUTO_INCREMENT,
	alias VARCHAR(254) NOT NULL,
	value VARBINARY(64760) NOT NULL,
	PRIMARY KEY (identity),
	UNIQUE KEY (alias)
) ENGINE=InnoDB;
