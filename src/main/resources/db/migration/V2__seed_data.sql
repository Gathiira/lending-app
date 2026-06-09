-- =========================================================
-- V2__seed_data.sql
-- Seed data for the Lms Application
-- =========================================================

-- =========================================================
-- NOTIFICATION TEMPLATES
-- =========================================================
INSERT INTO notification_templates (event, channel, subject, body) VALUES
                                                                       ('LOAN_CREATED',    'EMAIL', 'Your Loan Application - {{loanReference}}',
                                                                        'Dear {{customerName}},\n\nYour loan application {{loanReference}} for KES {{amount}} has been successfully created.\n\nDue Date: {{dueDate}}\n\nThank you for choosing us.\n\nBest regards,\nLending Team'),

                                                                       ('LOAN_DISBURSED',  'EMAIL', 'Loan Disbursed - {{loanReference}}',
                                                                        'Dear {{customerName}},\n\nYour loan {{loanReference}} of KES {{amount}} has been disbursed to your account.\n\nPlease ensure repayment by {{dueDate}}.\n\nBest regards,\nLending Team'),

                                                                       ('LOAN_REPAYMENT',  'EMAIL', 'Repayment Received - {{loanReference}}',
                                                                        'Dear {{customerName}},\n\nWe have received your repayment of KES {{amount}} for loan {{loanReference}}.\n\nRemaining Balance: KES {{outstandingBalance}}\n\nThank you!\n\nBest regards,\nLending Team'),

                                                                       ('LOAN_OVERDUE',    'EMAIL', 'URGENT: Overdue Loan - {{loanReference}}',
                                                                        'Dear {{customerName}},\n\nYour loan {{loanReference}} is overdue. Outstanding balance: KES {{outstandingBalance}}.\n\nPlease make payment immediately to avoid further fees.\n\nBest regards,\nLending Team'),

                                                                       ('DUE_DATE_REMINDER','EMAIL', 'Payment Reminder - {{loanReference}}',
                                                                        'Dear {{customerName}},\n\nThis is a reminder that your loan {{loanReference}} payment of KES {{outstandingBalance}} is due on {{dueDate}}.\n\nBest regards,\nLending Team'),

                                                                       ('LOAN_CLOSED',     'EMAIL', 'Loan Closed - {{loanReference}}',
                                                                        'Dear {{customerName}},\n\nCongratulations! Your loan {{loanReference}} has been fully repaid and closed.\n\nThank you for your prompt payments!\n\nBest regards,\nLending Team'),

                                                                       ('LATE_FEE_APPLIED','EMAIL', 'Late Fee Applied - {{loanReference}}',
                                                                        'Dear {{customerName}},\n\nA late fee of KES {{amount}} has been applied to your loan {{loanReference}}.\n\nPlease make payment as soon as possible.\n\nBest regards,\nLending Team'),

-- SMS templates
                                                                       ('LOAN_CREATED',    'SMS', NULL,
                                                                        'Loan {{loanReference}} created. Amount: KES {{amount}}. Due: {{dueDate}}. Thank you.'),

                                                                       ('LOAN_DISBURSED',  'SMS', NULL,
                                                                        'Loan {{loanReference}} disbursed: KES {{amount}}. Due: {{dueDate}}. Thank you.'),

                                                                       ('LOAN_REPAYMENT',  'SMS', NULL,
                                                                        'Payment received KES {{amount}} for {{loanReference}}. Balance: KES {{outstandingBalance}}.'),

                                                                       ('LOAN_OVERDUE',    'SMS', NULL,
                                                                        'URGENT: Loan {{loanReference}} is overdue. Balance: KES {{outstandingBalance}}. Pay now to avoid fees.'),

                                                                       ('DUE_DATE_REMINDER','SMS', NULL,
                                                                        'Reminder: Loan {{loanReference}} payment of KES {{outstandingBalance}} due on {{dueDate}}.'),

                                                                       ('LATE_FEE_APPLIED','SMS', NULL,
                                                                        'Late fee KES {{amount}} added to loan {{loanReference}}. Total balance: KES {{outstandingBalance}}.');

-- =========================================================
-- LOAN PRODUCTS
-- =========================================================
INSERT INTO loan_products
(name, description, min_amount, max_amount, interest_rate, tenure_value, tenure_type, loan_type, installment_count, billing_cycle_type, grace_period_days)
VALUES

    ('Quick Cash 30',
     'Short-term lump sum loan repayable in 30 days.',
     1000, 50000, 12.50, 30, 'DAYS', 'LUMP_SUM', NULL, 'INDIVIDUAL', 3),

    ('Flexi Monthly 3',
     'Three-month installment loan with monthly repayments.',
     5000, 200000, 18.00, 3, 'MONTHS', 'INSTALLMENT', 3, 'INDIVIDUAL', 5),

    ('Consolidated Payroll',
     'Monthly payroll loan consolidated on the 25th of each month.',
     10000, 500000, 10.00, 1, 'MONTHS', 'LUMP_SUM', NULL, 'CONSOLIDATED', 0),

    ('Business Boost 6M',
     'Six-month business loan with monthly installments.',
     50000, 1000000, 15.75, 6, 'MONTHS', 'INSTALLMENT', 6, 'INDIVIDUAL', 7);

-- =========================================================
-- PRODUCT FEES
-- =========================================================
-- Quick Cash 30: 5% service fee + 0.5% daily fee + 500 late fee after 3 days
INSERT INTO product_fees (product_id, fee_type, calculation_method, amount, days_after_due, description) VALUES
                                                                                                             (1, 'SERVICE_FEE', 'PERCENTAGE', 5.00,  0, '5% origination service fee'),
                                                                                                             (1, 'DAILY_FEE',   'PERCENTAGE', 0.50,  0, '0.5% daily fee on outstanding balance'),
                                                                                                             (1, 'LATE_FEE',    'FIXED',      500.00, 3, 'KES 500 late fee after 3 days overdue');

-- Flexi Monthly 3: 3% service fee + 1000 late fee after 5 days
INSERT INTO product_fees (product_id, fee_type, calculation_method, amount, days_after_due, description) VALUES
                                                                                                             (2, 'SERVICE_FEE', 'PERCENTAGE', 3.00,   0, '3% origination service fee'),
                                                                                                             (2, 'LATE_FEE',    'FIXED',      1000.00, 5, 'KES 1000 late fee after 5 days overdue');

-- Consolidated Payroll: flat 2% service fee + 1500 late fee after 0 days
INSERT INTO product_fees (product_id, fee_type, calculation_method, amount, days_after_due, description) VALUES
                                                                                                             (3, 'SERVICE_FEE', 'PERCENTAGE', 2.00,   0, '2% origination service fee'),
                                                                                                             (3, 'LATE_FEE',    'FIXED',      1500.00, 0, 'KES 1500 late fee immediately on overdue');

-- Business Boost 6M: 4% service fee + 2000 late fee after 7 days
INSERT INTO product_fees (product_id, fee_type, calculation_method, amount, days_after_due, description) VALUES
                                                                                                             (4, 'SERVICE_FEE', 'PERCENTAGE', 4.00,   0, '4% origination service fee'),
                                                                                                             (4, 'LATE_FEE',    'FIXED',      2000.00, 7, 'KES 2000 late fee after 7 days overdue');

-- =========================================================
-- CUSTOMERS
-- =========================================================
INSERT INTO customers (first_name, last_name, email, phone_number, national_id, credit_score, max_loan_limit, current_loan_limit, preferred_channel) VALUES
                                                                                                                                                         ('Alice',   'Kamau',   'alice.kamau@gmail.com',   '+254711000001', '12345678', 750, 200000, 200000, 'EMAIL'),
                                                                                                                                                         ('Bob',     'Otieno',  'bob.otieno@gmail.com',    '+254722000002', '23456789', 620, 100000, 100000, 'SMS'),
                                                                                                                                                         ('Carol',   'Wanjiku', 'carol.wanjiku@gmail.com', '+254733000003', '34567890', 810, 500000, 500000, 'EMAIL'),
                                                                                                                                                         ('David',   'Mwangi',  'david.mwangi@gmail.com',  '+254744000004', '45678901', 580, 50000,  50000,  'SMS'),
                                                                                                                                                         ('Eve',     'Ndungu',  'eve.ndungu@gmail.com',    '+254755000005', '56789012', 690, 300000, 300000, 'EMAIL');