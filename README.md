# Backend_Transformer

Backend development of **TransformerIQ**.

---

## Prerequisites

- **PostgreSQL** (Version used: 17.6)
- **Maven** (for running Spring Boot application)
- **Java 17+**

---

## Database Setup

1. Install PostgreSQL (version 17.6 ).
2. Use password **`dulmin`** for the PostgreSQL superuser.
3. Set the PostgreSQL binary path in **Git Bash**.  
   Replace `17` with your installed PostgreSQL version:
   ```bash
   export PG_BIN="/c/Program Files/PostgreSQL/17/bin"
4. Create the database. Enter the password when prompte:
   ```bash
   "$PG_BIN/psql.exe" -U postgres -d postgres -c "CREATE DATABASE transformers_db;"

5. Run the backend.
   ```bash 
   mvn spring-boot:run -Dspring-boot.run.profiles=prod




