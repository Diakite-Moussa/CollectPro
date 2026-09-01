-- Type MIME du logo (nécessaire pour servir le fichier avec le bon Content-Type).
ALTER TABLE organizations ADD COLUMN logo_content_type VARCHAR(100);
