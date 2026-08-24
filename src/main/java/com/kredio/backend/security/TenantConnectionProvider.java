package com.kredio.backend.security;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Proveedor de conexiones que ejecuta SET search_path para cambiar de schema.
 * Cada conexión se configura con el schema del tenant actual.
 */
@Component
public class TenantConnectionProvider implements MultiTenantConnectionProvider {

    private final DataSource dataSource;

    public TenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return getConnection("public");
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        releaseConnection("public", connection);
    }

    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();
        if (tenantIdentifier != null && !tenantIdentifier.equals("public")) {
            try (Statement stmt = connection.createStatement()) {
                // Establece el schema del tenant, con fallback a public para tablas compartidas
                stmt.execute("SET search_path TO " + tenantIdentifier + ", public");
            }
        }
        return connection;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Limpia el search_path antes de devolver la conexión al pool de Hikari
            stmt.execute("SET search_path TO public");
        }
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}