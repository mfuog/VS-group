-- MySQL definition and initialization script for shop service application
-- Copyright 2010-2013 Sascha Baumeister, all rights reserved
SET CHARACTER SET latin1;
DROP DATABASE IF EXISTS shop;
CREATE DATABASE shop CHARACTER SET utf8;
USE shop;

CREATE TABLE Customer (
	identity BIGINT AUTO_INCREMENT,
	alias VARCHAR(16) NOT NULL,
	password VARCHAR(16) NOT NULL,
	firstName VARCHAR(128) NOT NULL,
	lastName VARCHAR(128) NOT NULL,
	street VARCHAR(128) NOT NULL,
	postCode VARCHAR(16) NOT NULL,
	city VARCHAR(128) NOT NULL,
	email VARCHAR(128) NOT NULL,
	phone VARCHAR(128) NOT NULL,
	PRIMARY KEY (identity),
	UNIQUE KEY (alias)
) ENGINE=InnoDB;

CREATE TABLE Article (
	identity BIGINT AUTO_INCREMENT,
	description VARCHAR(4000) NOT NULL,
	price BIGINT NOT NULL,
	count INT NOT NULL,
	PRIMARY KEY (identity)
) ENGINE=InnoDB;

CREATE TABLE Purchase (
	identity BIGINT AUTO_INCREMENT,
	customerIdentity BIGINT NOT NULL,
	creationTimestamp BIGINT NOT NULL,
	taxRate DOUBLE NOT NULL,
	PRIMARY KEY (identity),
	FOREIGN KEY (customerIdentity) REFERENCES Customer (identity) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE PurchaseItem (
	identity BIGINT AUTO_INCREMENT,
	purchaseIdentity BIGINT NOT NULL,
	articleIdentity BIGINT NOT NULL,
	articlePrice BIGINT NOT NULL,
	count INT NOT NULL,
	PRIMARY KEY (identity),
	FOREIGN KEY (purchaseIdentity) REFERENCES Purchase (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (articleIdentity) REFERENCES Article (identity) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;


INSERT INTO Customer VALUES (0, "ines", "ines", "Ines", "Bergmann", "Wiener Strasse 42", "10999", "Berlin", "ines.bergmann@web.de", "0172/2345678");
SET @c1 = LAST_INSERT_ID();
INSERT INTO Customer VALUES (0, "sascha", "sascha", "Sascha", "Baumeister", "Glogauer Strasse 17", "10999", "Berlin", "sascha.baumeister@gmail.com", "0174/3345975");
SET @c2 = LAST_INSERT_ID();

INSERT INTO Article VALUES (0, "CARIOCA Fahrrad-Schlauch, 28x1.5 Zoll", 167, 40);
SET @a1 = LAST_INSERT_ID();
INSERT INTO Article VALUES (0, "CONTINENTAL Fahrrad-Schlauch Tour, 28 Zoll", 336, 80);
SET @a2 = LAST_INSERT_ID();
INSERT INTO Article VALUES (0, "PROPHETE Fahrrad-Schlauch, 14x1.75 Zoll", 252, 20);
SET @a3 = LAST_INSERT_ID();

INSERT INTO Purchase VALUES (0, @c1, 1288605807761, 0.19);
SET @p1 = LAST_INSERT_ID();
INSERT INTO Purchase VALUES (0, @c2, 1288635807761, 0.19);
SET @p2 = LAST_INSERT_ID();

INSERT INTO PurchaseItem VALUES (0, @p1, @a1, 167, 2);
INSERT INTO PurchaseItem VALUES (0, @p2, @a2, 336, 1);
INSERT INTO PurchaseItem VALUES (0, @p2, @a3, 252, 4);
COMMIT;

SELECT * FROM Customer;
SELECT * FROM Article;
SELECT * FROM Purchase;
SELECT * FROM PurchaseItem;
