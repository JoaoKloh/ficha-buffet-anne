-- Callback do Flyway: roda depois de todo migrate (não é migration, não entra no histórico).
-- Executa como anne_owner (spring.flyway.init-sqls). Num banco novo, o histórico do Flyway é
-- criado por anne_owner e os privilégios padrão o liberariam para anne_app; o usuário de
-- runtime não deve ler nem alterar o histórico de migrations.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anne_app') THEN
        REVOKE ALL ON TABLE "${flyway:defaultSchema}"."${flyway:table}" FROM anne_app;
    END IF;
END
$$;
