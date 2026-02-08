/*
 * Copyright (c) 2026 Pollo Chang. All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying, via any medium, is strictly prohibited.
 */
package com.sqlconsole.core.model.dto;

/**
 * Request object for SQL analysis.
 *
 * @param dbId the database ID
 * @param sql the SQL query to analyze
 */
public record AnalyzeRequest(Long dbId, String sql) {}
