package com.sqlconsole.core.core.vdba.analysis;

import com.sqlconsole.core.core.vdba.DbaSuggestion;
import com.sqlconsole.core.core.vdba.UnifiedExecutionNode;
import java.util.List;

/**
 * Result of the execution plan analysis.
 * @param enrichedRoot the root node of the enriched execution plan
 * @param suggestions list of DBA suggestions based on the analysis
 */
public record AnalysisResult(UnifiedExecutionNode enrichedRoot, List<DbaSuggestion> suggestions) {}
