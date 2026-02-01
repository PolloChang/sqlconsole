package com.sqlconsole.core.core.vdba;

/**
 * Represents a suggestion for optimizing a SQL query.
 *
 * @param type The type of suggestion (e.g., "INDEX", "REWRITE").
 * @param message The suggestion message.
 */
public record DbaSuggestion(String type, String message) {}
