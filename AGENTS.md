# 🤖 AGENTS.md - AI Developer Guide

## 📌 Project Identity: SQLConsole

You are an expert Java & DBA agent assisting in the development of **sqlconsole**, a self-hosted Web DB IDE. This tool focuses on enterprise SQL auditing, performance tracking, and cross-platform database management.

---

## 🛠️ Technical Stack & Constraints

* **Java Version:** **JDK 21** (Must use Java 21 features; do not downgrade to 17).
* **Framework:** Spring Boot 3.x.
* **Database:** PostgreSQL 14+ (System DB), supporting Oracle DB (Target).
* **Build Tool:** Gradle 8.x (Multi-module project).
* **Lombok:** Required (Ensure annotation processing is enabled).
* **Code Style:** Google Java Style (Enforced via Spotless).

---

## ⚙️ Setup & Initialization

### 1. Database Setup (PostgreSQL)

Before running or testing, ensure the system database is initialized:

```sql
CREATE USER polloconsole WITH PASSWORD 'password';
CREATE DATABASE sql_console_sys OWNER polloconsole;

-- For PostgreSQL 15+
\c sql_console_sys
GRANT ALL ON SCHEMA public TO polloconsole;

```

* **Initial Credentials:** `user` / `1234` (See `sys_users` table for BCrypt hashes).

### 2. Environment Variables

```bash
export JAVA_HOME=/usr/local/lib/jvm/jdk21-latest
export PATH=${JAVA_HOME}/bin:$PATH

```

---

## 🚀 Commands for Agents

### 1. Build & Style Check

Perform these before any Pull Request:

* **Full Check:** `./gradlew check`
* **Fix Style:** `./gradlew spotlessApply`
* **Static Analysis:** `./gradlew pmdMain`

### 2. Running the Application

* **Core (Open Source):** `./gradlew :sqlconsole:bootRun --args='--spring.profiles.active=dev'`

### 3. Testing (Primary Command)

**To verify changes, execute only the following:**

```bash
./gradlew clean integrationTest

```

> **Note:** Integration tests require a live PostgreSQL instance based on the configurations above.

---

## 📝 Development Rules for Agents

1. **Strict Typing:** Leverage Java 21's `record`, `var`, and Pattern Matching.
2. **Audit Trail:** Every SQL execution must be captured in `sql_history`.
3. **Module Awareness:** * New features go to `:sqlconsole`.
* Approval/Workflow features go to `:sqlconsole-premium`.


4. **Security:** Always use `PreparedStatement` or JPA to prevent SQL Injection.
5. **Fail Fast:** If `integrationTest` fails, analyze `build/reports/tests/integrationTest/index.html` immediately.

---

## 📁 Module Reference

* `sqlconsole`: Core management, DB configs, user auth.
