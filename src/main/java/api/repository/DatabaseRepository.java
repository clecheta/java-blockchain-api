package api.repository;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class DatabaseRepository {

    private static final Logger LOGGER = Logger.getLogger("DatabaseIndex");
    /**
     * Kept as a lifecycle compatibility seam. Flyway owns PostgreSQL DDL and
     * indexes; Hibernate validates them at startup.
     */
    public void createIndex() {
        LOGGER.debug("PostgreSQL schema and indexes managed by Flyway");
    }
}
