package com.sqlconsole.core.model.entity;

import jakarta.persistence.*;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Represents a user in the system. */
@Entity
@Table(name = "sys_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  /** Role for system administrators. */
  public static final String ROLE_ADMIN = "ROLE_ADMIN";

  /** Role for auditors. */
  public static final String ROLE_AUDITOR = "ROLE_AUDITOR";

  /** Role for standard users. */
  public static final String ROLE_USER = "ROLE_USER";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true)
  private String username;

  @com.fasterxml.jackson.annotation.JsonProperty(
      access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
  private String password; // BCrypt encoded

  private String role; // ROLE_USER, ROLE_AUDITOR

  @ManyToMany
  @JoinTable(
      name = "user_db_permissions",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "db_config_id"))
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<DbConfig> accessibleDatabases = new java.util.HashSet<>();

  /**
   * Constructs a new User.
   *
   * @param username the username
   * @param password the encoded password
   * @param role the user role
   */
  public User(String username, String password, String role) {
    this.username = username;
    this.password = password;
    this.role = role;
  }
}
