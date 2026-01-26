package work.pollochang.sqlconsole.model.entity;

import jakarta.persistence.*;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "sys_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  public static final String ROLE_ADMIN = "ROLE_ADMIN";
  public static final String ROLE_AUDITOR = "ROLE_AUDITOR";
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

  // Constructors, Getters, Setters

  public User(String username, String password, String role) {
    this.username = username;
    this.password = password;
    this.role = role;
  }
}
