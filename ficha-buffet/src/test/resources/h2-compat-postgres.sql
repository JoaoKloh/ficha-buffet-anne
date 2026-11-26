-- Funções do PostgreSQL usadas em SQL nativo que o H2 não tem (ver H2CompatPostgres).
CREATE ALIAS IF NOT EXISTS PG_GET_SERIAL_SEQUENCE FOR 'com.jbkloh.ficha_buffet.H2CompatPostgres.pgGetSerialSequence';
CREATE ALIAS IF NOT EXISTS SETVAL FOR 'com.jbkloh.ficha_buffet.H2CompatPostgres.setval';
