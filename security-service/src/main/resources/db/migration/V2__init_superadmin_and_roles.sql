INSERT INTO roles(role_id, role_name) VALUES
    (1,'CLIENT'),
    (2,'ADMIN'),
    (3,'SUPERADMIN'),
    (4,'WORKER');

INSERT INTO users (id, email, password, workplace_id, role_id)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'superadmin@sto.by',
           '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDMVr5yUP1KUOYTa',
           NULL,
           3
       );
