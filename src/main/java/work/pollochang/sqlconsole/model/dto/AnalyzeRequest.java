/*
 * Copyright (c) 2026 Pollo Chang. All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying, via any medium, is strictly prohibited.
 */
package work.pollochang.sqlconsole.model.dto;

public record AnalyzeRequest(Long dbId, String sql) {}
