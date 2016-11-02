ALTER TABLE environment ADD COLUMN environment_type_2 CHARACTER VARYING(6);
UPDATE environment SET environment_type_2 = environment_type;
ALTER TABLE environment drop COLUMN environment_type;
ALTER TABLE environment ADD COLUMN environment_type CHARACTER VARYING(6);
UPDATE environment SET environment_type = environment_type_2;
ALTER TABLE environment ADD CONSTRAINT environment_environment_type_check CHECK (environment_type in ('ROLLER', 'DOCKER', 'RPM'));
