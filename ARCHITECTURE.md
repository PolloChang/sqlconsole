# sqlConsole 產品架構設計規範

## 1. 產品願景與定位 (Product Vision)

sqlConsole 旨在建立一個從個人開發者到企業資安合規的全方位資料庫管理平台。本專案採用「三層級擴充模型」，確保代碼的靈活性與商業價值的最大化。

### 1.1 三層級擴充模型

* **Tier 1: OpenSource (開發者導向)** - 專注於開發人員的個人生產力工具。
* **Tier 2: Premium (DBA 導向)** - 專注於專業資料庫管理員的效能優化與深度維運。
* **Tier 3: Enterprise (審核與治理導向)** - 專注於企業資安合規、乙方連線管控與零信任治理。

---

## 2. 功能矩陣與邊界 (Functional Boundaries)

### 2.1 OpenSource 版本 (The Productivity Engine)

* **目標用戶：** 開發人員。
* **核心功能：**
* 基礎 SQL 編輯器：支援語法高亮、自動補齊、查詢結果分頁。
* 基礎連線：支援 Oracle (11g/19c), MS SQL Server, DB2, PostgreSQL, MySQL, MariaDB。
* 資料導出：支援 CSV, JSON, XML 格式。
* **OpenSource DB 效能優化工具：** 
  * PostgreSQL: pg_stat_statements (核心擴展), pgBadger (報表工具),pg_qualstats (索引建議基礎)
  * MySQL: pt-query-digest (來自 Percona Toolkit), EXPLAIN ANALYZE (MySQL 8.0+)
  * MariaDB: Performance Schema (與 Sys Schema), NALYZE TABLE / ANALYZE [FORMAT=JSON], SHOW EXPLAIN FOR [Thread_ID]
* **設計原則：** 輕量、無狀態、易於 Docker 部署。

### 2.2 Premium 版本 (The Management Suite)

* **目標用戶：** 資料庫管理員 (DBA)。
* **核心功能：**
* **深度整合 - 企業級資料庫支援：** 深度整合 Oracle (11g/19c), SQL Server, DB2
* **效能優化工具：** 
  * Oracle: 實作執行計畫對比、SQL Tuning Advisor, AWR
  * MS SQL Server: Query Store (查詢存放區), SQL Server Profiler / Extended Events (XEvents), Showplan
  * DB2: DB2 Design Advisor, Visual Explain
* **健康檢查：** 自動偵測死鎖 (Deadlock)、長事務 (Long Transaction) 與空間預警。


* **設計原則：** 結合專業 DBA 邏輯層，處理複雜的資料庫元數據 (Metadata)。

### 2.3 Enterprise 版本 (The Governance Gateway)

* **目標用戶：** 企業 CISO、稽核人員、甲方管理職。
* **核心功能：**
* **連線申請工作流 (Vendor Access Workflow)：** 乙方開發者在進入甲方環境前，必須提交申請。經由「申請 -> 審核 -> 授權」流程，獲取臨時連線權限。
* **動態脫敏 (Dynamic Data Masking)：** 針對敏感欄位（如身分證字號、信用卡號）進行即時遮罩，不改動原始資料。
* **操作審計與回放：** 完整記錄所有連線期間執行的 SQL 語句，支援錄影或日誌回放。
* **堡壘機 Proxy：** 用戶不直接接觸 DB 帳密，僅透過本平台的 Proxy 閘道連線。


* **設計原則：** 嚴謹的權限隔離 (RBAC)、完整的審計日誌 (Audit Trail)、LDAP/MFA 整合。

---

## 3. 技術棧與規範 (Technical Standards)

* **核心開發語言：** Java 17。
* **測試標準：** 必須遵循 `JUnit 5` 規範。所有業務邏輯、特別是「審核流」與「脫敏邏輯」，必須具備 80% 以上的單元測試覆蓋率。
* **安全性參考：**
* 企業版功能應參考 `Oracle Advanced Security Guide` 與 `Database Vault` 之概念實作。
* 所有的 SQL 執行必須經過「防注入掃描層」。


* **模組化要求：**
* 企業版的「申請流」與「審計模組」應設計為獨立的 Plugin/Module。
* 嚴禁將 Enterprise 專屬的商務邏輯（如甲方/乙方權限模型）侵入 OpenSource 核心引擎。



---

## 4. 開發指令準則 (Instructions for Jules)

當接收到開發需求時，請 `google jules` 遵循以下步驟：

1. **識別 Tier：** 確認功能屬於 OS、Premium 還是 Enterprise。
2. **定位模組：** 根據 Tier 決定檔案位置。
* OpenSource 功能：放置於 `com.sqlconsole.core.*`
* Premium 功能：放置於 `com.sqlconsole.premium.*`
* Enterprise 功能：放置於 `com.sqlconsole.enterprise.*`


3. **遵循測試標準：** 撰寫對應的 JUnit 5 測試案例。


## 備註

### **跨資料庫效能優化工具對照表**

| 功能類別 | **Oracle** | **PostgreSQL** | **MySQL** | **MariaDB** | **MS SQL Server** | **DB2 (LUW)** |
| --- | --- | --- | --- | --- | --- | --- |
| **歷史效能快照 (AWR)** | AWR / Statspack | **pg_stat_statements** + pgBadger | Performance Schema + pt-query-digest | Performance Schema + pt-query-digest | **Query Store (QS)** | IBM DMC (Historical Metrics) |
| **即時診斷 (ASH)** | ASH (Active Session History) | pg_stat_activity + pg_wait_sampling | SHOW PROCESSLIST / sys schema | **SHOW EXPLAIN FOR [ID]** | Extended Events (XEvents) | dsmtop / MON_GET_TABLE |
| **SQL 優化建議 (Advisor)** | **SQL Tuning Advisor** | pg_qualstats / pganalyze | MySQLTuner / EverSQL | User Statistics / Analyze Statement | **DTA (Database Engine Tuning Advisor)** | **Design Advisor (db2advis)** |
| **執行計畫分析 (Plan)** | Explain Plan / SQL Monitor | **EXPLAIN ANALYZE** / PEV | **EXPLAIN ANALYZE** / Workbench | ANALYZE [FORMAT=JSON] | **Showplan / Live Query Stats** | db2exfmt / Visual Explain |
| **效能基準鎖定** | SQL Plan Baseline | pg_plan_guider (擴展) | Query Rewrite / Hints | Query Rewrite Plugins | Plan Forcing (in QS) | Optimization Profiles |
