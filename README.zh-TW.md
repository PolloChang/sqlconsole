# 🦁 SQL Console: Your Enterprise Gatekeeper for Database Operations

### **Empowering Developers, Safeguarding Data.**

**DB Console** 是一款專為現代企業設計的「資料庫治理與安全審核平台」。我們解決了開發速度與資安合規之間的矛盾，讓您的團隊在享受高效開發的同時，徹底告別「誤刪資料表」與「未授權異動」的災難。

---

## 💡 為什麼企業需要 DB Console？

在傳統開發環境中，DBA 往往面臨兩難：

1. **過度授權**：開發人員直接持有高權限帳號，風險極高。
2. **流程卡頓**：每次異動都要寫文件、過簽呈，嚴重拖慢上線速度。

**DB Console 完美解決了這個問題：**

* **風險對沖**：敏感指令（DDL/DML）強制進入審核流，落實「四眼原則 (Four-Eyes Principle)」。
* **權限收斂**：開發者無需持有資料庫真實密碼，僅透過 Web 介面操作。
* **合規留痕**：每一行 SQL、每一次執行結果都有據可查，輕鬆應對內部與外部稽核。

---

## 🚀 核心功能對比

| 功能特性 | 🟢 社群開源版 (Community) | 🔵 企業進階版 (Premium) |
| --- | --- | --- |
| **多資料庫支援** (Oracle, PG, MySQL, DB2, MSSQL) | ✅ | ✅ |
| **SQL 執行歷史紀錄** | ✅ | ✅ |
| **手動 TCL 控制** (Commit/Rollback) | ✅ | ✅ |
| **多角色權限管理** (User / Auditor) | ✅ | ✅ |
| **敏感指令攔截** (DDL/DML/DCL) | ❌ | **✅ 強制攔截** |
| **自動化審核工作流** (Approval Workflow) | ❌ | **✅ 核心功能** |
| **企業身分整合** (LDAP / Windows AD / SSO) | ❌ | **✅ 支援** |
| **精細化資源控管** (按 DB 實例分配權限) | ❌ | **✅ 支援** |
| **專業技術支援與 SLA** | ❌ | **✅ 提供** |

---

## 🛠 技術堆疊：極速、現代、穩定

我們堅持使用最新的穩定技術架構，確保系統能承載企業級的高併發與高可靠性需求。

* **核心引擎**: **Java 21** (利用虛擬執行緒提供極致性能)。
* **持久層**: **Spring Data JPA + PostgreSQL**。

---

## 📖 企業級工作流 (Workflow)

### 1. 智慧攔截 (Intelligent Interception)

當一般使用者輸入 `DROP TABLE` 或 `UPDATE` 大量資料時，系統會自動辨識為「高風險操作」，並將任務暫存在審核池中，狀態設為 `PENDING`。

### 2. 四眼審核 (Four-Eyes Approval)

審核者（Auditor）會收到通知，開啟控制台預覽 SQL 內容。一鍵點擊「批准」後，系統會自動在後端代為執行，並確保事務（Transaction）正確 Commit。

### 3. 完整審計鏈 (Full Audit Trail)

無論是誰執行的 SQL，系統都會紀錄：

* **Who**: 哪位員工？
* **What**: 執行了什麼內容？
* **When**: 精確到毫秒的時間戳記。
* **Where**: 針對哪個資料庫環境？
* **Status**: 成功還是失敗？

---

## ⚙️ 快速部署 (Quick Start)

我們提供單一 JAR 檔案部署，讓您在 5 分鐘內完成環境架設。

```bash
java -jar sqlconsole.jar
```

---

## 🛡️ License & Copyright

* **Open Source Version**: Licensed under [Apache License 2.0](https://github.com/PolloChang/sqlconsole?tab=Apache-2.0-1-ov-file).
* **Premium Version**: Licensed under [End User License Agreement, EULA].

Copyright © 2026 **Pollo Chang (Java Master)**. All rights reserved.

---

## 📞 聯繫我們 (Contact)

**正在尋找更安全、更高效的資料庫管理方式嗎？**
我們提供企業版免費試用與客製化諮詢，歡迎與我們的專家團隊聯繫：

* **Email**: jameschangwork@gmail.com
