-- I1-B7 · Vacunación (13 · Base de datos, Vacunación).
--
-- La línea base creó calendar_doses con la edad en días. El calendario oficial la da en meses
-- (RN-VAC-01) y algunas dosis cambian de edad según la fecha de nacimiento (RN-VAC-10). La tabla
-- todavía está vacía: no hay datos que convertir.

ALTER TABLE calendar_doses RENAME COLUMN age_days TO age_months;
ALTER TABLE calendar_doses RENAME CONSTRAINT calendar_doses_age_days_check TO calendar_doses_age_months_check;

-- Rango de fechas de nacimiento al que corresponde la fila, ambos inclusive; nulo es sin límite.
ALTER TABLE calendar_doses
  ADD COLUMN born_from  date,
  ADD COLUMN born_until date,
  ADD CONSTRAINT calendar_doses_born_range CHECK (born_from IS NULL OR born_until IS NULL OR born_from <= born_until);

-- Un mismo dose_code puede tener varias filas, una por cohorte. NULLS NOT DISTINCT: dos filas del
-- mismo código sin born_from también chocan. Que los rangos no se superpongan lo verifica
-- calendarProblems() al cargar.
ALTER TABLE calendar_doses DROP CONSTRAINT calendar_doses_version_dose_code_key;
ALTER TABLE calendar_doses
  ADD CONSTRAINT calendar_doses_version_dose_code_born_from_key UNIQUE NULLS NOT DISTINCT (version_id, dose_code, born_from);

-- Límites de los textos libres (RNF-PRI-03, SUP-24). La nota de other_vaccines ya tenía el suyo.
ALTER TABLE applied_doses
  ADD CONSTRAINT applied_doses_lot_length CHECK (char_length(lot) <= 40),
  ADD CONSTRAINT applied_doses_place_length CHECK (char_length(place) <= 120);
ALTER TABLE other_vaccines
  ADD CONSTRAINT other_vaccines_name_length CHECK (char_length(name) <= 120),
  ADD CONSTRAINT other_vaccines_lot_length CHECK (char_length(lot) <= 40);

-- Para listar las dosis de un niño (sincronización y eliminación).
CREATE INDEX applied_doses_child_id_idx ON applied_doses (child_id);
