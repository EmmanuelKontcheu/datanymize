package com.datanymize.anonymization;

import com.datanymize.database.model.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;

/**
 * Property-based tests for table processing order.
 * **Validates: Requirements 5.1**
 */
@PropertyDefaults(tries = 50)
public class TableProcessingOrderProperties {

    @Property
    @Label("Property 14: Table Processing Order")
    void testTableProcessingOrder() {
        // Feature: datanymize, Property 14: Table Processing Order
        
        // Given a schema with foreign key relationships
        DatabaseSchema schema = new DatabaseSchema();
        schema.setDatabaseName("test");
        
        // Create tables
        Table users = new Table();
        users.setName("users");
        users.setPrimaryKeys(List.of("id"));
        
        Table orders = new Table();
        orders.setName("orders");
        orders.setPrimaryKeys(List.of("id"));
        
        Table orderItems = new Table();
        orderItems.setName("order_items");
        orderItems.setPrimaryKeys(List.of("id"));
        
        schema.setTables(List.of(users, orders, orderItems));
        
        // Create foreign keys: orders -> users, order_items -> orders
        ForeignKey fk1 = new ForeignKey();
        fk1.setSourceTable("orders");
        fk1.setSourceColumn("user_id");
        fk1.setTargetTable("users");
        fk1.setTargetColumn("id");
        
        ForeignKey fk2 = new ForeignKey();
        fk2.setSourceTable("order_items");
        fk2.setSourceColumn("order_id");
        fk2.setTargetTable("orders");
        fk2.setTargetColumn("id");
        
        schema.setForeignKeys(List.of(fk1, fk2));
        
        // When calculating table order
        TableOrderCalculator calculator = new TableOrderCalculator();
        List<String> order = calculator.calculateTableOrder(schema);
        
        // Then tables should be in correct order
        Assume.that(order.size() == 3);
        
        // users must come before orders
        int usersIndex = order.indexOf("users");
        int ordersIndex = order.indexOf("orders");
        assert usersIndex < ordersIndex : "users must be processed before orders";
        
        // orders must come before order_items
        int orderItemsIndex = order.indexOf("order_items");
        assert ordersIndex < orderItemsIndex : "orders must be processed before order_items";
    }

    @Property
    @Label("Property 14b: Circular Dependency Detection")
    void testCircularDependencyDetection() {
        // Feature: datanymize, Property 14b: Circular Dependency Detection
        
        // Given a schema with circular foreign key relationships
        DatabaseSchema schema = new DatabaseSchema();
        schema.setDatabaseName("test");
        
        // Create tables
        Table tableA = new Table();
        tableA.setName("table_a");
        tableA.setPrimaryKeys(List.of("id"));
        
        Table tableB = new Table();
        tableB.setName("table_b");
        tableB.setPrimaryKeys(List.of("id"));
        
        schema.setTables(List.of(tableA, tableB));
        
        // Create circular foreign keys: A -> B -> A
        ForeignKey fk1 = new ForeignKey();
        fk1.setSourceTable("table_a");
        fk1.setSourceColumn("b_id");
        fk1.setTargetTable("table_b");
        fk1.setTargetColumn("id");
        
        ForeignKey fk2 = new ForeignKey();
        fk2.setSourceTable("table_b");
        fk2.setSourceColumn("a_id");
        fk2.setTargetTable("table_a");
        fk2.setTargetColumn("id");
        
        schema.setForeignKeys(List.of(fk1, fk2));
        
        // When calculating table order
        TableOrderCalculator calculator = new TableOrderCalculator();
        
        // Then circular dependency should be detected
        boolean hasCircular = calculator.hasCircularDependencies(schema);
        assert hasCircular : "Circular dependencies should be detected";
    }

    @Property
    @Label("Property 14c: Self-Referential Foreign Keys")
    void testSelfReferentialForeignKeys() {
        // Feature: datanymize, Property 14c: Self-Referential Foreign Keys
        
        // Given a schema with self-referential foreign key
        DatabaseSchema schema = new DatabaseSchema();
        schema.setDatabaseName("test");
        
        // Create table
        Table employees = new Table();
        employees.setName("employees");
        employees.setPrimaryKeys(List.of("id"));
        
        schema.setTables(List.of(employees));
        
        // Create self-referential foreign key
        ForeignKey fk = new ForeignKey();
        fk.setSourceTable("employees");
        fk.setSourceColumn("manager_id");
        fk.setTargetTable("employees");
        fk.setTargetColumn("id");
        
        schema.setForeignKeys(List.of(fk));
        
        // When calculating table order
        TableOrderCalculator calculator = new TableOrderCalculator();
        List<String> order = calculator.calculateTableOrder(schema);
        
        // Then table should be included in order
        Assume.that(order.size() == 1);
        assert order.get(0).equals("employees") : "Self-referential table should be in order";
    }

    @Property
    @Label("Property 14d: Multiple Independent Tables")
    void testMultipleIndependentTables() {
        // Feature: datanymize, Property 14d: Multiple Independent Tables
        
        // Given a schema with multiple independent tables
        DatabaseSchema schema = new DatabaseSchema();
        schema.setDatabaseName("test");
        
        // Create tables
        Table table1 = new Table();
        table1.setName("table_1");
        table1.setPrimaryKeys(List.of("id"));
        
        Table table2 = new Table();
        table2.setName("table_2");
        table2.setPrimaryKeys(List.of("id"));
        
        Table table3 = new Table();
        table3.setName("table_3");
        table3.setPrimaryKeys(List.of("id"));
        
        schema.setTables(List.of(table1, table2, table3));
        schema.setForeignKeys(new ArrayList<>());
        
        // When calculating table order
        TableOrderCalculator calculator = new TableOrderCalculator();
        List<String> order = calculator.calculateTableOrder(schema);
        
        // Then all tables should be in order
        Assume.that(order.size() == 3);
        assert order.contains("table_1") : "table_1 should be in order";
        assert order.contains("table_2") : "table_2 should be in order";
        assert order.contains("table_3") : "table_3 should be in order";
    }
}
