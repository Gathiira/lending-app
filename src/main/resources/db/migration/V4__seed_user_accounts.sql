-- =========================================================
-- V4__seed_user_accounts.sql
-- Demo accounts — passwords are BCrypt of "Password1!"
-- =========================================================

-- Staff / Admin accounts (no customer_id)
INSERT INTO user_accounts (username, password, role, status) VALUES
                                                                      ('admin',         '$2a$12$FdRZHhN6jLtZxDl9OV8tGeuO/fE7ZToPPsqQ6dh5nGEgMxckbu0.G', 'ADMIN',    'ACTIVE'),
                                                                      ('john',    '$2a$12$FdRZHhN6jLtZxDl9OV8tGeuO/fE7ZToPPsqQ6dh5nGEgMxckbu0.G', 'STAFF',    'ACTIVE'),
                                                                      ('jane',   '$2a$12$FdRZHhN6jLtZxDl9OV8tGeuO/fE7ZToPPsqQ6dh5nGEgMxckbu0.G', 'STAFF',    'ACTIVE');

-- Customer accounts — linked to the seeded customers (ids 1-5)
INSERT INTO user_accounts (username, password, role, status, customer_id) VALUES
                                                                                   ('kamau',   '$2a$12$FdRZHhN6jLtZxDl9OV8tGeuO/fE7ZToPPsqQ6dh5nGEgMxckbu0.G', 'CUSTOMER', 'ACTIVE', 1),
                                                                                   ('otieno',    '$2a$12$FdRZHhN6jLtZxDl9OV8tGeuO/fE7ZToPPsqQ6dh5nGEgMxckbu0.G', 'CUSTOMER', 'ACTIVE', 2),
                                                                                   ('wanjiku', '$2a$12$FdRZHhN6jLtZxDl9OV8tGeuO/fE7ZToPPsqQ6dh5nGEgMxckbu0.G', 'CUSTOMER', 'ACTIVE', 3),
                                                                                   ('mwangi',  '$2a$12$FdRZHhN6jLtZxDl9OV8tGeuO/fE7ZToPPsqQ6dh5nGEgMxckbu0.G', 'CUSTOMER', 'ACTIVE', 4),
                                                                                   ('ndungu',    '$2a$12$FdRZHhN6jLtZxDl9OV8tGeuO/fE7ZToPPsqQ6dh5nGEgMxckbu0.G', 'CUSTOMER', 'ACTIVE', 5);

-- Note: all passwords above are BCrypt hashes of "Password1!"
-- Regenerate with: new BCryptPasswordEncoder(12).encode("Password1!")