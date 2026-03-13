package com.datanymize.test;

import com.datanymize.database.model.ConnectionConfig;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

import java.time.LocalDateTime;

/**
 * Specialized generators for database connection configurations.
 * 
 * Provides property-based test generators for:
 * - PostgreSQL configurations
 * - MySQL configurations
 * - MongoDB configurations
 * - Invalid configurations for error testing
 * 
 * These generators are used across all database connectivity tests
 * to ensure consistent and comprehensive test coverage.
 */
public class DatabaseConfigGenerators extends BasePropertyTest {
    
    // ============ PostgreSQL Generators ============
    
    /**
     * Generator for valid PostgreSQL connection configurations.
     * 
     * Generates realistic PostgreSQL configs with:
     * - Valid hostnames (localhost, domain names, IP addresses)
     * - Standard PostgreSQL port (5432) or alternatives (5433, 5434)
     * - Valid database names
     * - Valid usernames and passwords
     * - TLS/SSL configuration options
     * - Connection timeout (1-30 seconds)
     * 
     * @return Arbitrary<ConnectionConfig> for valid PostgreSQL configs
     */
    public Arbitrary<ConnectionConfig> validPostgreSQLConfigs() {
        return Combinators.combine(
                hostnames(),
                Arbitraries.of(5432, 5433, 5434),
                databaseNames(),
                usernames(),
                passwords(),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                connectionTimeouts()
        ).as((host, port, database, username, password, useTLS, verifyCert, timeout) ->
                ConnectionConfig.builder()
                        .id("test-pg-" + System.nanoTime())
                        .type("postgresql")
                        .host(host)
                        .port(port)
                        .database(database)
                        .username(username)
                        .password(password)
                        .useTLS(useTLS)
                        .verifyCertificate(verifyCert)
                        .connectionTimeoutSeconds(timeout)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
    
    /**
     * Generator for invalid PostgreSQL connection configurations.
     * 
     * Generates configurations with missing or invalid parameters:
     * - Empty host
     * - Invalid port (0, negative, > 65535)
     * - Empty database name
     * - Empty username
     * - Null password
     * - Invalid timeout (0 or negative)
     * 
     * @return Arbitrary<ConnectionConfig> for invalid PostgreSQL configs
     */
    public Arbitrary<ConnectionConfig> invalidPostgreSQLConfigs() {
        return Arbitraries.oneOf(
                // Empty host
                Arbitraries.just(ConnectionConfig.builder()
                        .type("postgresql")
                        .host("")
                        .port(5432)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Invalid port (0)
                Arbitraries.just(ConnectionConfig.builder()
                        .type("postgresql")
                        .host("localhost")
                        .port(0)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Invalid port (negative)
                Arbitraries.just(ConnectionConfig.builder()
                        .type("postgresql")
                        .host("localhost")
                        .port(-1)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Invalid port (> 65535)
                Arbitraries.just(ConnectionConfig.builder()
                        .type("postgresql")
                        .host("localhost")
                        .port(70000)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Empty database
                Arbitraries.just(ConnectionConfig.builder()
                        .type("postgresql")
                        .host("localhost")
                        .port(5432)
                        .database("")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Empty username
                Arbitraries.just(ConnectionConfig.builder()
                        .type("postgresql")
                        .host("localhost")
                        .port(5432)
                        .database("testdb")
                        .username("")
                        .password("pass")
                        .build()),
                
                // Null password
                Arbitraries.just(ConnectionConfig.builder()
                        .type("postgresql")
                        .host("localhost")
                        .port(5432)
                        .database("testdb")
                        .username("user")
                        .password(null)
                        .build()),
                
                // Invalid timeout (0)
                Arbitraries.just(ConnectionConfig.builder()
                        .type("postgresql")
                        .host("localhost")
                        .port(5432)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .connectionTimeoutSeconds(0)
                        .build()),
                
                // Invalid timeout (negative)
                Arbitraries.just(ConnectionConfig.builder()
                        .type("postgresql")
                        .host("localhost")
                        .port(5432)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .connectionTimeoutSeconds(-1)
                        .build())
        );
    }
    
    // ============ MySQL Generators ============
    
    /**
     * Generator for valid MySQL connection configurations.
     * 
     * Generates realistic MySQL configs with:
     * - Valid hostnames
     * - Standard MySQL port (3306) or alternatives (3307, 3308)
     * - Valid database names
     * - Valid usernames and passwords
     * - TLS/SSL configuration options
     * - Connection timeout (1-30 seconds)
     * 
     * @return Arbitrary<ConnectionConfig> for valid MySQL configs
     */
    public Arbitrary<ConnectionConfig> validMySQLConfigs() {
        return Combinators.combine(
                hostnames(),
                Arbitraries.of(3306, 3307, 3308),
                databaseNames(),
                usernames(),
                passwords(),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                connectionTimeouts()
        ).as((host, port, database, username, password, useTLS, verifyCert, timeout) ->
                ConnectionConfig.builder()
                        .id("test-mysql-" + System.nanoTime())
                        .type("mysql")
                        .host(host)
                        .port(port)
                        .database(database)
                        .username(username)
                        .password(password)
                        .useTLS(useTLS)
                        .verifyCertificate(verifyCert)
                        .connectionTimeoutSeconds(timeout)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
    
    /**
     * Generator for invalid MySQL connection configurations.
     * 
     * Generates configurations with missing or invalid parameters.
     * Same patterns as PostgreSQL invalid configs.
     * 
     * @return Arbitrary<ConnectionConfig> for invalid MySQL configs
     */
    public Arbitrary<ConnectionConfig> invalidMySQLConfigs() {
        return Arbitraries.oneOf(
                // Empty host
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mysql")
                        .host("")
                        .port(3306)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Invalid port
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mysql")
                        .host("localhost")
                        .port(0)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Empty database
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mysql")
                        .host("localhost")
                        .port(3306)
                        .database("")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Empty username
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mysql")
                        .host("localhost")
                        .port(3306)
                        .database("testdb")
                        .username("")
                        .password("pass")
                        .build()),
                
                // Null password
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mysql")
                        .host("localhost")
                        .port(3306)
                        .database("testdb")
                        .username("user")
                        .password(null)
                        .build()),
                
                // Invalid timeout
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mysql")
                        .host("localhost")
                        .port(3306)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .connectionTimeoutSeconds(0)
                        .build())
        );
    }
    
    // ============ MongoDB Generators ============
    
    /**
     * Generator for valid MongoDB connection configurations.
     * 
     * Generates realistic MongoDB configs with:
     * - Valid hostnames
     * - Standard MongoDB port (27017) or alternatives (27018, 27019)
     * - Valid database names
     * - Valid usernames and passwords
     * - TLS/SSL configuration options
     * - Connection timeout (1-30 seconds)
     * 
     * @return Arbitrary<ConnectionConfig> for valid MongoDB configs
     */
    public Arbitrary<ConnectionConfig> validMongoDBConfigs() {
        return Combinators.combine(
                hostnames(),
                Arbitraries.of(27017, 27018, 27019),
                databaseNames(),
                usernames(),
                passwords(),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                connectionTimeouts()
        ).as((host, port, database, username, password, useTLS, verifyCert, timeout) ->
                ConnectionConfig.builder()
                        .id("test-mongo-" + System.nanoTime())
                        .type("mongodb")
                        .host(host)
                        .port(port)
                        .database(database)
                        .username(username)
                        .password(password)
                        .useTLS(useTLS)
                        .verifyCertificate(verifyCert)
                        .connectionTimeoutSeconds(timeout)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
    
    /**
     * Generator for invalid MongoDB connection configurations.
     * 
     * Generates configurations with missing or invalid parameters.
     * Same patterns as PostgreSQL invalid configs.
     * 
     * @return Arbitrary<ConnectionConfig> for invalid MongoDB configs
     */
    public Arbitrary<ConnectionConfig> invalidMongoDBConfigs() {
        return Arbitraries.oneOf(
                // Empty host
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mongodb")
                        .host("")
                        .port(27017)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Invalid port
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mongodb")
                        .host("localhost")
                        .port(0)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Empty database
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mongodb")
                        .host("localhost")
                        .port(27017)
                        .database("")
                        .username("user")
                        .password("pass")
                        .build()),
                
                // Empty username
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mongodb")
                        .host("localhost")
                        .port(27017)
                        .database("testdb")
                        .username("")
                        .password("pass")
                        .build()),
                
                // Null password
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mongodb")
                        .host("localhost")
                        .port(27017)
                        .database("testdb")
                        .username("user")
                        .password(null)
                        .build()),
                
                // Invalid timeout
                Arbitraries.just(ConnectionConfig.builder()
                        .type("mongodb")
                        .host("localhost")
                        .port(27017)
                        .database("testdb")
                        .username("user")
                        .password("pass")
                        .connectionTimeoutSeconds(0)
                        .build())
        );
    }
    
    // ============ Generic Generators ============
    
    /**
     * Generator for any valid database connection configuration.
     * 
     * Randomly selects between PostgreSQL, MySQL, and MongoDB configs.
     * 
     * @return Arbitrary<ConnectionConfig> for any valid database config
     */
    public Arbitrary<ConnectionConfig> anyValidDatabaseConfig() {
        return Arbitraries.oneOf(
                validPostgreSQLConfigs(),
                validMySQLConfigs(),
                validMongoDBConfigs()
        );
    }
    
    /**
     * Generator for any invalid database connection configuration.
     * 
     * Randomly selects between PostgreSQL, MySQL, and MongoDB invalid configs.
     * 
     * @return Arbitrary<ConnectionConfig> for any invalid database config
     */
    public Arbitrary<ConnectionConfig> anyInvalidDatabaseConfig() {
        return Arbitraries.oneOf(
                invalidPostgreSQLConfigs(),
                invalidMySQLConfigs(),
                invalidMongoDBConfigs()
        );
    }
}
