package work.pollochang.sqlconsole.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import work.pollochang.sqlconsole.model.enums.DbType;

@Entity
@Table(name = "db_configs")
@Data
public class DbConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public DbConfig() {}

    public DbConfig(String name, DbType dbType, String jdbcUrl, String dbUser, String dbPassword) {
        this.name = name;
        this.dbType = dbType;
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }
}