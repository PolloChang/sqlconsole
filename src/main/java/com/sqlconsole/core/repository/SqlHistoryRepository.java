package com.sqlconsole.core.repository;

import com.sqlconsole.core.model.entity.SqlHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SqlHistoryRepository extends JpaRepository<SqlHistory, Long> {}
