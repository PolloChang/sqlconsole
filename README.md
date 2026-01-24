# 🦁 SQL Console: Your Enterprise Gatekeeper for Database Operations

### **Empowering Developers, Safeguarding Data.**

**DB Console** is a "Database Governance and Security Audit Platform" specifically designed for modern enterprises. We resolve the conflict between development speed and security compliance, allowing your team to enjoy efficient development while completely eliminating disasters such as "accidental table deletion" and "unauthorized modifications."

---

## 💡 Why do Enterprises need DB Console?

In traditional development environments, DBAs often face a dilemma:

1. **Over-authorization**: Developers directly hold high-privilege accounts, posing extremely high risks.
2. **Workflow Friction**: Every change requires writing documents and going through approval processes, severely slowing down deployment speed.

**DB Console solves this problem perfectly:**

* **Risk Hedging**: Sensitive commands (DDL/DML) are forced into the approval flow, implementing the **"Four-Eyes Principle."**
* **Credential Consolidation**: Developers do not need to hold actual database passwords; they operate exclusively through the web interface.
* **Compliance Traceability**: Every line of SQL and every execution result is recorded and searchable, easily handling internal and external audits.

---

## 🚀 Core Feature Comparison

| Feature | 🟢 Community (Open Source) | 🔵 Premium (Enterprise) |
| --- | --- | --- |
| **Multi-Database Support** (Oracle, PG, MySQL, DB2, MSSQL) | ✅ | ✅ |
| **SQL Execution History** | ✅ | ✅ |
| **Manual TCL Control** (Commit/Rollback) | ✅ | ✅ |
| **Multi-role Permission Management** (User / Auditor) | ✅ | ✅ |
| **Sensitive Command Interception** (DDL/DML/DCL) | ❌ | **✅ Mandatory Interception** |
| **Automated Approval Workflow** | ❌ | **✅ Core Feature** |
| **Enterprise Identity Integration** (LDAP / AD / SSO) | ❌ | **✅ Supported** |
| **Fine-grained Resource Control** (DB-level permissions) | ❌ | **✅ Supported** |
| **Professional Technical Support & SLA** | ❌ | **✅ Provided** |

---

## 🛠 Tech Stack: Fast, Modern, Stable

We adhere to the latest stable technical architectures to ensure the system can handle enterprise-level high concurrency and reliability requirements.

* **Core Engine**: **Java 21** (Utilizing Virtual Threads for ultimate performance).
* **Persistence Layer**: **Spring Data JPA + PostgreSQL**.

---

## 📖 Enterprise Workflow

### 1. Intelligent Interception

When a general user enters `DROP TABLE` or `UPDATE` for large amounts of data, the system automatically identifies it as a "high-risk operation" and holds the task in the approval pool with a **`PENDING`** status.

### 2. Four-Eyes Approval

Auditors receive notifications and open the console to preview the SQL content. Upon clicking "Approve," the system automatically executes the command on the backend and ensures the transaction is correctly committed.

### 3. Full Audit Trail

Regardless of who executes the SQL, the system records:

* **Who**: Which employee?
* **What**: What was the content executed?
* **When**: Timestamp accurate to milliseconds.
* **Where**: Which database environment?
* **Status**: Success or failure?

---

## ⚙️ Quick Start

We provide single JAR file deployment, allowing you to set up the environment in 5 minutes.

```bash
java -jar sqlconsole.jar

```

---

## 🛡️ License & Copyright

* **Open Source Version**: Licensed under [Apache License 2.0](https://github.com/PolloChang/sqlconsole?tab=Apache-2.0-1-ov-file).
* **Premium Version**: Licensed under [End User License Agreement, EULA].

Copyright © 2026 **Pollo Chang (Java Master)**. All rights reserved.

---

## 📞 Contact Us

**Looking for a safer and more efficient way to manage your databases?**
We offer free trials for the Enterprise Edition and customized consultations. Please contact our expert team:

* **Email**: jameschangwork@gmail.com
