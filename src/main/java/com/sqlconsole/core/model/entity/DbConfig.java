package com.sqlconsole.core.model.entity;

import jakarta.persistence.*;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.sqlconsole.core.model.enums.DbType;

@Entity
@Table(name = "db_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbConfig {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "db_type", length = 50)
  private DbType dbType;

  @Column(nullable = false, length = 500)
  private String jdbcUrl;

  private String dbUser;
  private String dbPassword;

  @ManyToMany(mappedBy = "accessibleDatabases")
  @com.fasterxml.jackson.annotation.JsonIgnore // Prevent circular reference
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<User> authorizedUsers = new java.util.HashSet<>();

  public DbConfig(String name, DbType dbType, String jdbcUrl, String dbUser, String dbPassword) {
    this.name = name;
    this.dbType = dbType;
    this.jdbcUrl = jdbcUrl;
    this.dbUser = dbUser;
    this.dbPassword = dbPassword;
  }
}
