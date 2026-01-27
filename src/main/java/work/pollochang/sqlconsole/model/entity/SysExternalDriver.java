package work.pollochang.sqlconsole.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_external_driver")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysExternalDriver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_name", nullable = false)
    private String driverName;

    @Column(name = "jar_path", nullable = false)
    private String jarPath;

    @Column(name = "driver_class", nullable = false)
    private String driverClass;

    @Column(name = "sha256", nullable = false, unique = true)
    private String sha256;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
