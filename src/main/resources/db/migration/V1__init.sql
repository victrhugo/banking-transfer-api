-- Create accounts table
CREATE TABLE IF NOT EXISTS accounts (
  id uuid PRIMARY KEY,
  name varchar(255) NOT NULL,
  balance numeric(19,2) NOT NULL
);

-- Create transfers table
CREATE TABLE IF NOT EXISTS transfers (
  id uuid PRIMARY KEY,
  sender_id uuid NOT NULL,
  receiver_id uuid NOT NULL,
  amount numeric(19,2) NOT NULL,
  created_at timestamp NOT NULL
);

-- Seed sample accounts
INSERT INTO accounts(id, name, balance) VALUES
('863398d2-b5c2-4ba9-a5e3-a3c0e89c56b0'::uuid, 'Hugo', 3000.00),
('a39e1b3b-4246-4218-add0-09f4148bb351'::uuid, 'Victor', 1000.00),
('976d3cd0-0a8c-48fd-812e-644244024087'::uuid, 'Alice', 1000.00),
('f4a141b8-b0d7-47ba-879a-3c422094e7b3'::uuid, 'Bob', 500.00)
ON CONFLICT DO NOTHING;
