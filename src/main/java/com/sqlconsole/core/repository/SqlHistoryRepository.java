package com.sqlconsole.core.repository;

import com.sqlconsole.core.model.entity.SqlHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for managing SQL execution history. */
public interface SqlHistoryRepository extends JpaRepository<SqlHistory, Long> {}
