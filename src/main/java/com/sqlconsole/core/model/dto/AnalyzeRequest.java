/*
 * Copyright (c) 2026 Pollo Chang. All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying, via any medium, is strictly prohibited.
 */
package com.sqlconsole.core.model.dto;

public record AnalyzeRequest(Long dbId, String sql) {}
