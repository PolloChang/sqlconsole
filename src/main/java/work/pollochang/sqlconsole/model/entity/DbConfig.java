package work.pollochang.sqlconsole.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "db_configs")
@Data
public class DbConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String jdbcUrl;
    private String dbUser;
    private String dbPassword;

    public DbConfig() {}
    public DbConfig(String name, String jdbcUrl, String dbUser, String dbPassword) {
        this.name = name;
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

}