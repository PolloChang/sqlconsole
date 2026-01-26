package work.pollochang.sqlconsole;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import work.pollochang.sqlconsole.model.entity.DbConfig;
import work.pollochang.sqlconsole.model.entity.User;
import work.pollochang.sqlconsole.model.enums.DbType;
import work.pollochang.sqlconsole.repository.UserRepository;
import work.pollochang.sqlconsole.service.DbConfigService;

@Slf4j
@SpringBootApplication
public class SqlConsoleApplication {

  public static void main(String[] args) {
    SpringApplication.run(SqlConsoleApplication.class, args);
  }

  @Bean
  CommandLineRunner init(
      UserRepository userRepo, DbConfigService dbConfigService, PasswordEncoder encoder) {
    return args -> {
      // 初始化使用者
      if (userRepo.count() == 0) {
        userRepo.save(new User("user", encoder.encode("1234"), "ROLE_USER"));
        userRepo.save(new User("admin", encoder.encode("1234"), "ROLE_AUDITOR"));
        log.info("✅ 預設使用者已建立：user/1234, admin/1234");
      }
      // 初始化測試 DB 設定 (請確保本地有這個 Postgres DB)
      if (dbConfigService.getAllConfigs().isEmpty()) {
        dbConfigService.saveConfig(
            new DbConfig(
                "Local Postgres",
                DbType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/sql_console_sys",
                "postgres",
                "password"));
        log.info("✅ 預設資料庫連線已建立");
      }
    };
  }
}
