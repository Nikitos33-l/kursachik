INSERT INTO roles(role_id, role_name) VALUES
    (1,'CLIENT'),
    (2,'ADMIN'),
    (3,'SUPERADMIN'),
    (4,'WORKER');

INSERT INTO users (id, email, password, workplace_id, role_id)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'superadmin@sto.by',
           '$2a$10$spG8jhQT/ME9djCutY0cvewYTzAlXnZH9AvHCZ5FPEofM/S2W/Sem',
           NULL,
           3
       );
