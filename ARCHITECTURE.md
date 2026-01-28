# sqlConsole 多維度產品架構設計規範 (Multi-line Dev Spec)

## 0. 技術棧與規範 (Technical Standards)

* **核心開發語言：** Java 17。

* **測試標準：** 必須遵循 `JUnit 5` 規範。所有業務邏輯、特別是「審核流」與「脫敏邏輯」，必須具備 80% 以上的單元測試覆蓋率。

* **安全性參考：**

* 企業版功能應參考 `Oracle Advanced Security Guide` 與 `Database Vault` 之概念實作。
* 所有的 SQL 執行必須經過「防注入掃描層」

---

## 1. 核心願景與多線並行策略

sqlConsole 採用 **「核心引擎 + 插件化能力 (Core-Plugin Architecture)」** 的設計模式。為了支援多線開發而不出錯，我們實施「介面與實作分離」的最高準則。

### 1.1 三層級並行開發線 (Tier Definition)

* **Tier 1: OpenSource (開發者導向)** - 專注於開發人員的個人生產力工具。
* **Tier 2: Premium (DBA 導向)** - 專注於專業資料庫管理員的效能優化與深度維運。
* **Tier 3: Enterprise (審核與治理導向)**：專注於零信任安全、合規工作流與企業治理能力。

---

## 2. 核心架構規範：能力抽離 (Abstraction Layer)

為避免多線開發時 PM/ENT 功能洩漏至 OS 版本，**所有功能開發必須遵循以下介面模式：**

### 2.1 介面驅動開發 (Interface-Driven)

* **Core 模組 (`work.pollochang.sqlconsole.core`)**：僅定義介面 (e.g., `AuditProvider`, `SecretManager`)。
* **OS 實作**：提供基礎、非感知的 Null 或 Simple 實作。
* **PM/ENT 實作**：於專屬 Repo 中實作進階邏輯 (e.g., `VaultSecretManager`, `ApprovalWorkflow`)。

### 2.2 驅動隔離與動態加載 (需求 1)

* **機制**：實作 `SqlConsoleClassLoader`，確保 OS 與 ENT 版本能針對不同版本的資料庫驅動 (如 Oracle 11g vs 19c) 進行實體隔離，防止 `ClassNotFound` 或類衝突。

### 2.2 * **模組化要求：**

* 企業版的「申請流」與「審計模組」應設計為獨立的 Plugin/Module。

* 嚴禁將 Enterprise 專屬的商務邏輯（如甲方/乙方權限模型）侵入 OpenSource 核心引擎。

---

## 3. 功能矩陣與分層邊界 (Feature Matrix)

| 功能項次 | OpenSource | Premium | Enterprise | 隔離規範 |
| --- | --- | --- | --- | --- |
| **需求 1: JDBC 隔離載入** | **v** |  | **v** | 實作於 Core，由 ENT 提供配置層 |
| **需求 3: 帳密解耦** | **v** | **v** | **v** | OS 使用 YML 加密；PM/ENT 支援 Vault |
| **需求 6: 動態資料遮罩** |  |  | **v** | 核心 Hook 必須在 Resultset 解析層 |
| **需求 7: 查詢防護欄** |  | **v** | **v** | SQL Parser 攔截器實作 |
| **需求 13: 虛擬 DBA** | 基礎版 | 進階版 | 全功能 | 依據「資料庫對照表」動態注入 Provider |

### 虛擬 DBA 實作路徑 (針對多線開發優化)

* **OpenSource**：僅限開發 `EXPLAIN ANALYZE` (MySQL/PG)。
* **Premium**：開發 `AWR/ASH` 與 `Query Store` 可視化。
* **Enterprise**：開發 `SQL Tuning Advisor` 整合。

---

## 4. 給 google jules 的開發指令準則 (The Jules Rules)

為了在多線開發中維持代碼整潔，請 `google jules` 嚴格遵守：

1. **Tier 識別與路徑綁定**：
* OpenSource 功能：放置於 `com.sqlconsole.core.*`
* Premium 功能：放置於 `com.sqlconsole.premium.*`
* Enterprise 功能：放置於 `com.sqlconsole.enterprise.*`

當接收到開發需求時，請 `google jules` 遵循以下步驟：


1. **不污染準則 (Anti-Pollution)**：
* **禁止** 在 Core 引擎中 `import` 任何帶有 `premium` 或 `enterprise` 字樣的類別。
* 若進階功能需要介入核心流程，請在核心定義 `EventPublisher` 或 `HookInterface`。


3. **測試硬指標**：
* 每一條開發線的變更必須包含一個 `JUnit 5` 測試案例。
* PM/ENT 功能必須測試「權限不足時的優雅降級 (Fallback)」。


4. **License 感知**：
* 所有 `Controller` 必須標註 `[LicenseGate]`，當偵測為 OS 版本時自動屏蔽進階路徑。



---

## 5. 虛擬 DBA 跨資料庫技術映射表 (實作導引)

| 功能 | Oracle | PostgreSQL | MS SQL Server |
| --- | --- | --- | --- |
| **歷史快照** | AWR / Statspack | pg_stat_statements | Query Store |
| **即時診斷** | ASH | pg_stat_activity | Extended Events |
| **優化建議** | SQL Tuning Advisor | pg_qualstats | DTA |
| **執行計畫** | SQL Monitor | PEV (Visualization) | Live Query Stats |

---

## 6. 安全性與合規標準 (Security Compliance)

* **資料保護**：動態脫敏 (DDM) 必須參考 `Oracle Advanced Security` 邏輯，在 Data Streaming 階段進行攔截。
* **身份管理**：Enterprise 線必須支持 LDAP/AD 整合，並將資料庫帳密儲存於加密的二進制 Blob 中 (需求 3)。
* **稽核紀錄**：所有 ENT 等級的日誌必須支援「數位簽章」，確保日誌不可篡改 (需求 5)。

---

**專案管理員備註：**
多線開發時，請優先確認該任務的 **[Target Tier]**。若任務涉及跨 Tier 異動，請 google jules 採用「核心埋點、外部實作」的方式處理。