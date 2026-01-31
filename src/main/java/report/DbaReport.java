/*
 * Copyright (c) 2026 Pollo Chang. All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying, via any medium, is strictly prohibited.
 */
package report;

import java.util.List;

/**
 * 標準化 DBA 診斷報告
 * @param planContent 執行計畫原始內容
 * @param suggestions 系統自動生成的優化建議
 * @param executionTimeMs 預估或實際執行耗時 (毫秒)
 */
public record DbaReport(
        String planContent,
        List<String> suggestions,
        long executionTimeMs
) {}