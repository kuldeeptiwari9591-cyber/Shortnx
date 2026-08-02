package com.shortnx.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Single shared connection pool for the whole app.
 * Credentials come ONLY from environment variables — never hardcode them.
 * Set these before starting Tomcat:
 *   export DB_URL="jdbc:postgresql://<project>.pooler.supabase.com:6543/postgres"
 *   export DB_USER="postgres.xxxxxxxx"
 *   export DB_PASSWORD="..."
 */
public final class DBConfig {

    private static final HikariDataSource ds;

    static {
        String url = requireEnv("DB_URL");
        String user = requireEnv("DB_USER");
        String password = requireEnv("DB_PASSWORD");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(1_800_000);
        // Fail fast on boot if the DB is unreachable rather than failing
        // silently on the first request.
        config.setInitializationFailTimeout(5000);

        ds = new HikariDataSource(config);
    }

    private DBConfig() {
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }
}
