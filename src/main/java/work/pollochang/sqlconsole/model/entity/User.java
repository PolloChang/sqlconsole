package work.pollochang.sqlconsole.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sys_users")
@Data
public class User {


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    private String password; // BCrypt encoded
    private String role; // ROLE_USER, ROLE_AUDITOR

    // Constructors, Getters, Setters
    public User() {}

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}