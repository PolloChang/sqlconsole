package work.pollochang.sqlconsole.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sys_data_sources")
@Data
public class SysDataSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String url;
    private String username;

    // Encrypted
    private String password;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private SysExternalDriver driver;

    // HikariCP specific settings (optional)
    private Integer maxPoolSize = 10;
}
