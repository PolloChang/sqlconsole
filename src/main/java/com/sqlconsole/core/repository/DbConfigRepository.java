package com.sqlconsole.core.repository;

import com.sqlconsole.core.model.entity.DbConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for managing database configurations. */
public interface DbConfigRepository extends JpaRepository<DbConfig, Long> {}
