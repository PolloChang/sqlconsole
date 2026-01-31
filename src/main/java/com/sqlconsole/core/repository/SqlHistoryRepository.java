package com.sqlconsole.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sqlconsole.core.model.entity.SqlHistory;

public interface SqlHistoryRepository extends JpaRepository<SqlHistory, Long> {}
