/*
 * Copyright (c) 2026 Pollo Chang. All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying, via any medium, is strictly prohibited.
 */
package com.sqlconsole.core.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import report.DbaReport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PostgresDbaProviderTest {

    private PostgresDbaProvider provider;

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        provider = new PostgresDbaProvider();

        // 設定 Mock 行為
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
    }

    @Test
    void testGetExecutionPlan_Success() throws Exception {
        // 模擬 ResultSet 內容
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString(1)).thenReturn("Seq Scan on users", "Total cost: 10.0");

        DbaReport report = provider.getExecutionPlan(mockConnection, "SELECT * FROM users");

        assertNotNull(report);
        assertTrue(report.planContent().contains("Seq Scan"));
        assertEquals(1, report.suggestions().size());
        verify(mockStatement, times(1)).executeQuery(contains("EXPLAIN (ANALYZE"));
    }

    @Test
    void testSupports() {
        assertTrue(provider.supports("POSTGRESQL"));
        assertFalse(provider.supports("ORACLE"));
    }
}