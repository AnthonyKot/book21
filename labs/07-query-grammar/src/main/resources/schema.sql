CREATE TABLE invoice (
 id VARCHAR(20) PRIMARY KEY,
 tenant_id VARCHAR(20) NOT NULL,
 title VARCHAR(200) NOT NULL,
 amount INTEGER NOT NULL
);
INSERT INTO invoice VALUES ('C-1001', 'cedar', 'Office', 300);
INSERT INTO invoice VALUES ('C-1002', 'cedar', 'O''Brien', 100);
INSERT INTO invoice VALUES ('C-1003', 'cedar', '100% sample', 200);
INSERT INTO invoice VALUES ('B-2001', 'birch', 'Private', 900);
