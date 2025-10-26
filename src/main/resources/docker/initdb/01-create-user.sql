-- ========================================
-- Script d'Initialisation PostgreSQL
-- Exécuté au premier démarrage en tant que POSTGRES_USER (postgres)
-- ========================================

-- 1. Créer l'utilisateur 'dev' pour l'application
CREATE USER dev WITH PASSWORD 'dev' NOSUPERUSER NOCREATEDB NOCREATEROLE;
GRANT CONNECT ON DATABASE contract TO dev;

-- 2. Se connecter à la base 'contract'
\connect contract

-- 3. Créer le schéma dédié à l'application
CREATE SCHEMA IF NOT EXISTS contracts;
COMMENT ON SCHEMA contracts IS 'Schéma dédié à l''application Contract Service - Gestion des clients et contrats';

-- 4. Donner TOUS les droits sur le schéma 'contracts' à l'utilisateur 'dev'
GRANT ALL ON SCHEMA contracts TO dev;
GRANT CREATE ON SCHEMA contracts TO dev;

-- 5. Donner les droits sur les objets existants (si déjà créés)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA contracts TO dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA contracts TO dev;

-- 6. Pour les futurs objets créés par Flyway
ALTER DEFAULT PRIVILEGES IN SCHEMA contracts GRANT ALL PRIVILEGES ON TABLES TO dev;
ALTER DEFAULT PRIVILEGES IN SCHEMA contracts GRANT ALL PRIVILEGES ON SEQUENCES TO dev;
