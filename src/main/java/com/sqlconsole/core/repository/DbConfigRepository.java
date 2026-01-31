package com.sqlconsole.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sqlconsole.core.model.entity.DbConfig;

public interface DbConfigRepository extends JpaRepository<DbConfig, Long> {}
