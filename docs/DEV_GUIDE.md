# 🛠️ Developer Guide (開發者指南)

## ⚙️ Installation & Setup (安裝與設定)

### 1. Prerequisites (環境要求)

* **Java JDK 21+** (專案使用 Java 21 特性，請勿使用低版本)
* **PostgreSQL 14+** (作為系統資料庫)
* **Build Tool**: Gradle 8.x

### 2. Database Initialization (資料庫初始化)

請使用超級管理員連線至 PostgreSQL，執行以下指令。
**注意：** 帳號名稱必須與 `application.yml` 中的 `username` 一致。

```sql
-- 1. 建立系統使用者與資料庫
CREATE USER polloconsole WITH PASSWORD 'password';
CREATE DATABASE sql_console_sys OWNER polloconsole;

-- 2. 切換至該資料庫執行權限設定
\c sql_console_sys
GRANT ALL ON SCHEMA public TO polloconsole; -- PostgreSQL 15+ 必做

```

#### 手動建立 Table (或讓 JPA 自動產生)

如果您將 `ddl-auto` 設為 `update`，JPA 會自動建表。若需手動建立，請執行：

```sql
-- [核心版/開源版所需表格]
CREATE TABLE sys_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE db_configs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    db_type VARCHAR(50),
    jdbc_url VARCHAR(500) NOT NULL,
    db_user VARCHAR(255),
    db_password VARCHAR(255)
);

CREATE TABLE sql_history (
    id BIGSERIAL PRIMARY KEY,
    executor_name VARCHAR(255),
    db_name VARCHAR(255),
    sql_content TEXT,
    status VARCHAR(50),
    execute_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    result_msg TEXT
);

-- [企業版/付費版額外表格]
CREATE TABLE approval_tasks (
    id BIGSERIAL PRIMARY KEY,
    requester VARCHAR(255),
    db_config_id BIGINT,
    sql_content TEXT,
    status VARCHAR(50), -- PENDING, APPROVED, REJECTED
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 初始資料 (密碼 1234 的 BCrypt 加密值)
INSERT INTO sys_users (username, password, role) VALUES 
('user', '$2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlNBxBFve4ZlLa', 'ROLE_USER'),
('admin', '$2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlNBxBFve4ZlLa', 'ROLE_AUDITOR');

```

### 3. Configuration (`application.yml`)

本專案支援多環境 Profiles。主設定檔路徑：`src/main/resources/application.yml`。

| Profile | 用途 | 主要行為 |
| --- | --- | --- |
| **dev** | 開發環境 | `ddl-auto: update`, `show-sql: true` |
| **test** | 測試環境 | 連接測試 DB |
| **prod** | 生產環境 | `ddl-auto: none`, `logging.level: WARN` |

### 4. Run the Application (啟動程式)

#### 環境變數設定 (Linux/macOS)

**⚠️ 大師提醒：** 您原本寫 JDK 17，請務必改為 **JDK 21**。

```bash
export JAVA_HOME=/usr/local/lib/jvm/jdk21-latest
export PATH=${JAVA_HOME}/bin:$PATH

```

#### 使用 Gradle 啟動 (開發中)

```bash
# 啟動核心版 (Core)
./gradlew :sqlconsole:bootRun --args='--spring.profiles.active=dev'

# 啟動付費版 (Premium)
./gradlew :sqlconsole-premium:bootRun --args='--spring.profiles.active=dev'

```

#### 使用 JAR 啟動 (部署後)

```bash
# 啟動生產環境
java -jar sqlconsole-premium-0.0.1.jar --spring.profiles.active=prod

```

### 5. IntelliJ IDEA 開發設定

1. **專案匯入**: 選擇 `settings.gradle` 作為專案匯入。
2. **Active Profiles**:
* 打開 `Run/Debug Configurations`。
* 在 **Active profiles** 填入 `dev`。
* 確保選取的 `Main class` 是 `com.sqlconsole.core.SqlconsolePremiumApplication` (若要測試付費功能)。


3. **Lombok**: 確保安裝 Lombok 插件並開啟 `Annotation Processing`。

---

### 💡 修正重點說明：

1. **JDK 版本修正**：您的原文中 `JAVA_HOME` 指向 `jdk17-latest`，但專案使用了 Java 21，這會導致編譯錯誤或啟動失敗。
2. **初始資料 SQL**：我在資料庫初始化部分補上了 `INSERT INTO sys_users`。這對新開發者非常重要，否則他們建完環境後會因為沒有帳號而卡在登入頁面。
3. **Gradle 指令路徑**：因為你是多模組專案，啟動時最好帶上 `:sqlconsole:bootRun` 這種指定模組的寫法，避免 Gradle 混淆。
4. **一致性檢查**：提醒了 `polloconsole` 使用者名稱在 SQL 與 YML 中必須一致。

### 6. test

* Windows

```bash
.\gradlew.bat clean test integrationTest
```

* Mac / Linux

```bash
export NVD_API_KEY=a734f588-bd12-44e5-9a67-b545339bcc4c
./gradlew clean integrationTest
./gradlew clean test integrationTest
```

### 7. dev check

```bash
./gradlew check
```

* 單獨執行 PMD 掃描

```bash
./gradlew pmdMain
```

* Checkstyle 報表： build/reports/checkstyle/main.html
* PMD 報表： build/reports/pmd/main.html

### 8. style check

```bash
./gradlew checkstyleMain
```

* 一鍵修復所有樣式問題

```bash
./gradlew spotlessApply
```

* NVD

```bash
./gradlew dependencyCheckAnalyze --nvdApiKey=你的API_KEY
```