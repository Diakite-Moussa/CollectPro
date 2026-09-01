-- Objectif chiffré de collectes attendues pour une mission (nullable :
-- les missions existantes n'ont pas cette info, et le champ reste optionnel).
ALTER TABLE missions ADD COLUMN expected_collectes_count INTEGER;

-- Garde-fou : si renseigné, la valeur doit être positive.
ALTER TABLE missions ADD CONSTRAINT chk_expected_collectes_positive
    CHECK (expected_collectes_count IS NULL OR expected_collectes_count > 0);