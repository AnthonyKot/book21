CREATE TABLE account (tenant VARCHAR(20) PRIMARY KEY, balance_cents INTEGER NOT NULL);
CREATE TABLE credit (id VARCHAR(20) PRIMARY KEY, tenant VARCHAR(20) NOT NULL REFERENCES account(tenant), amount_cents INTEGER NOT NULL, applied BOOLEAN NOT NULL);
INSERT INTO account VALUES ('cedar',0),('birch',0);
INSERT INTO credit VALUES ('C-1001','cedar',1000,FALSE),('C-1002','cedar',1000,FALSE),('B-2001','birch',1000,FALSE);
