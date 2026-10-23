-- Pacientes ingresados directamente por profesionales; esta relación no crea tutoría legal.
CREATE TABLE pediatrician_patients (
  child_id uuid PRIMARY KEY REFERENCES children (id) ON DELETE CASCADE,
  pediatrician_account_id uuid NOT NULL REFERENCES accounts (id) ON DELETE CASCADE
);
CREATE INDEX pediatrician_patients_account_id_idx ON pediatrician_patients (pediatrician_account_id);
