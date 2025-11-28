# ⚡ Transformer Thermal Inspection Backend

<p align="center">
  <img src="https://img.shields.io/badge/Backend-SpringBoot-green" />
  <img src="https://img.shields.io/badge/Database-PostgreSQL-blueviolet" />
  <img src="https://img.shields.io/badge/Build-Maven-orange" />
  <img src="https://img.shields.io/badge/Status-Active-success" />
</p>

---

## 📖 Introduction  
The **Transformer Thermal Inspection Backend** is the core server for the Transformer Thermal Inspection System.  
It provides RESTful APIs for:  
- Transformer management  
- Inspection scheduling  
- Image upload and retrieval (baseline & maintenance)  
- Database handling with PostgreSQL  

This backend works together with the [Frontend Application](https://github.com/Project-TransformerIQ/FrontEnd) to provide a full-stack solution.  

- 🔗 [Backend Repository](https://github.com/Project-TransformerIQ/Backend_Transformer)  
- 🔗 [Frontend Repository](https://github.com/Project-TransformerIQ/FrontEnd)  

---

## ✨ Features  
- API endpoints for transformer CRUD operations.  
- Inspection creation, view, and deletion.  
- Baseline and maintenance image upload.
- Anomaly detection, creation and deletion. 
- PostgreSQL database integration with schema management.  
 

---

## 🛠️ Tech Stack  
- **Framework**: Spring Boot (Java)  
- **Database**: PostgreSQL  
- **ORM**: Hibernate / JPA  
- **Build Tool**: Maven  
- **Security**: Spring Security (future enhancement)  
- **Deployment**: Localhost / Cloud ready  

---

## 👥 Team Members  
This backend was developed by **Team WeMakeSoftware**:  

1. Dulmin Dulsara 
2. Nirosh Lakshan  
3. Tharushi Karavita
4. Lasith Haputhanthri 

---

## ⚙️ Setup & Installation  

### 🔑 Prerequisites  
- [Java JDK 17+](https://adoptium.net/)  
- [PostgreSQL](https://www.postgresql.org/)  
- Maven  

---

### 🚀 Steps to Run Backend  

1. **Clone the backend repository**  
   ```bash
   git clone https://github.com/Project-TransformerIQ/Backend_Transformer.git
   cd Backend_Transformer
   ```

2. **Configure PostgreSQL**  
   - Create the database:  
     ```sql
     CREATE DATABASE transformer;
     ```  

   - Update your `application.yml` with database credentials:  
     ```properties
     spring.datasource.url=jdbc:postgresql://localhost:5432/transformer
     spring.datasource.username=postgres
     spring.datasource.password=yourpassword
     spring.jpa.hibernate.ddl-auto=update
     ```

3. **Build and run the server**  
   ```bash
   ./mvnw spring-boot:run
   ```
   
   



## 🔗 API Endpoints  

### Transformer Management  
- `GET /transformer-management/view/{id}`  
- `POST /transformer-management/create`  
- `PUT /transformer-management/update/{id}`  
- `DELETE /transformer-management/delete/{id}`  

### Inspection Management  
- `POST /inspection-management/create`  
- `GET /inspection-management/view/{id}`  
- `DELETE /inspection-management/delete/{id}`  

### Image Management  
- `POST /image-inspection-management/upload`  
- `GET /image-inspection-management/baseline/{transformerNo}`  
- `GET /image-inspection-management/maintenance/{inspectionNo}`  
- `DELETE /image-inspection-management/delete/{imageId}`  

---

## 🧩 Maintenance Form: Generation & Saving

This backend exposes a purpose-built flow to generate a maintenance record form for a transformer and then persist the record once an engineer completes it.

- GET `GET /api/transformers/{id}/maintenance-record-form`
   - Query params: `inspectionId` (optional), `imageId` (optional).
   - Selection logic:
      - Validates transformer exists.
      - If `inspectionId` is provided, validates it belongs to the transformer.
      - If `imageId` is provided, validates it belongs to the transformer and is a `MAINTENANCE` image; if the image has an inspection and none provided, it is used.
      - If `imageId` is omitted, the most recent `MAINTENANCE` image is selected (optionally scoped to the provided inspection).
   - Response payload (`MaintenanceRecordFormDTO`):
      - `transformer`: Basic transformer details.
      - `inspection`: Current inspection (if any).
      - `maintenanceImage`: Selected maintenance image metadata.
      - `anomalies`: Anomaly regions (from `fault_regions`) mapped to `FaultRegionDTO`.
      - `displayBoxColors`: Optional display hints (from `display_metadata`), e.g., bounding-box colors.
      - `allowedStatuses`: Enum values (`OK`, `NEEDS_MAINTENANCE`, `URGENT_ATTENTION`).
      - `existingRecord`: If a record already exists for the selected image, it is included to support edit flows.

- POST `POST /api/transformers/{id}/maintenance-records`
   - AuthZ: Requires `MAINTENANCE_ENGINEER` (checked via `X-Auth-Token`).
   - Body (`CreateMaintenanceRecordDTO`):
      - `transformerId` (required; must match path `{id}`)
      - `inspectionId` (optional; must belong to transformer if present)
      - `maintenanceImageId` (required; must be a `MAINTENANCE` image of the transformer)
      - `inspectionTimestamp` (optional; defaults to inspection.createdAt or image.createdAt)
      - `inspectorName`, `status` (required), `electricalReadings` (map), `recommendedAction`, `additionalRemarks`
   - Constraints & behavior:
      - Exactly one record per maintenance image (unique `maintenance_image_id`).
      - Rejects if a record already exists for the image (HTTP 409).
      - On success returns `201 Created` with `MaintenanceRecordDTO`.

- PUT `PUT /api/transformers/maintenance-records/{recordId}`
   - AuthZ: Requires `MAINTENANCE_ENGINEER`.
   - Body (`UpdateMaintenanceRecordDTO`): patch-like; all fields optional except `id` (must match path).
   - Updates mutable fields and returns the updated `MaintenanceRecordDTO`.

Authentication notes:
- Login via `POST /api/auth/login` with body `{ "name": "...", "password": "..." }`.
- Pass `X-Auth-Token: <token>` header on subsequent requests.

---

## 🗄️ Database Schema

The dev profile uses `spring.jpa.hibernate.ddl-auto=update`, and migrations define core tables. Below reflects the current structure.

- `transformers`
   - `id` BIGINT PK, `created_at` TIMESTAMP(6)
   - `transformer_no`, `pole_no`, `region` (VARCHAR NOT NULL)
   - `transformer_type` (VARCHAR NOT NULL; CHECK in {`BULK`,`DISTRIBUTION`})

- `inspections`
   - `id` BIGINT PK, `created_at` TIMESTAMP(6) NOT NULL
   - `inspector` VARCHAR NOT NULL, `notes` TEXT, `title` VARCHAR NOT NULL
   - `status` VARCHAR NOT NULL; CHECK in {`OPEN`,`IN_PROGRESS`,`CLOSED`}
   - `transformer_id` BIGINT NOT NULL FK → `transformers(id)`

- `transformer_images`
   - `id` BIGINT PK, `created_at` TIMESTAMP(6)
   - `uploader` VARCHAR NOT NULL, `filename` VARCHAR, `content_type` VARCHAR, `size_bytes` BIGINT, `storage_path` VARCHAR
   - `image_type` VARCHAR NOT NULL; CHECK in {`BASELINE`,`MAINTENANCE`}
   - Environmental fields (from `EnvCondition`): `humidity` DOUBLE, `temperaturec` DOUBLE, `weather` VARCHAR CHECK in {`SUNNY`,`CLOUDY`,`RAINY`}, `location_note` VARCHAR
   - `transformer_id` BIGINT NOT NULL FK → `transformers(id)`
   - `inspection_id` BIGINT NULL FK → `inspections(id)`

- `fault_regions`
   - `db_id` BIGINT PK (AUTO_INCREMENT), `region_id` INT, `type` VARCHAR(100), `dominant_color` VARCHAR(50)
   - Bounding box: `bbox_x`, `bbox_y`, `bbox_width`, `bbox_height`, `bbox_area_px` (INT)
   - Centroid: `centroid_x`, `centroid_y` (INT)
   - `aspect_ratio` DOUBLE, `elongated` BOOLEAN, `connected_to_wire` BOOLEAN, `tag` VARCHAR(50), `confidence` DOUBLE
   - `image_id` BIGINT FK → `transformer_images(id)`
   - Manual annotation fields: `comment` TEXT, `is_manual` BOOLEAN
   - Audit fields: `created_at` TIMESTAMP, `created_by` VARCHAR(255), `last_modified_at` TIMESTAMP, `last_modified_by` VARCHAR(255)
   - Soft delete: `is_deleted` BOOLEAN DEFAULT FALSE, `deleted_at` TIMESTAMP

- `fault_region_colors`
   - `fault_region_id` BIGINT FK → `fault_regions(db_id)`
   - `color_value` INTEGER (RGB component values)

- `display_metadata`
   - `id` BIGINT PK (identity), `timestamp` TIMESTAMP(6)
   - `image_id` BIGINT FK → `transformer_images(id)`

- `display_box_colors`
   - `display_metadata_id` BIGINT FK → `display_metadata(id)`
   - `color_key` VARCHAR(50), `color_value` VARCHAR(50)

- `original_anomaly_results`
   - `id` BIGSERIAL PK
   - `image_id` BIGINT NOT NULL UNIQUE FK → `transformer_images(id)`
   - `anomaly_json` TEXT, `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

- `maintenance_records` (JPA entity-driven)
   - `id` BIGINT PK
   - `transformer_id` BIGINT NOT NULL FK → `transformers(id)`
   - `inspection_id` BIGINT NULL FK → `inspections(id)`
   - `maintenance_image_id` BIGINT NOT NULL UNIQUE FK → `transformer_images(id)`
   - `inspection_timestamp` TIMESTAMP, `inspector_name` VARCHAR
   - `status` VARCHAR enum in {`OK`,`NEEDS_MAINTENANCE`,`URGENT_ATTENTION`}
   - `recommended_action` TEXT, `additional_remarks` TEXT
   - `created_at`, `updated_at` TIMESTAMP

- `maintenance_record_electrical_readings`
   - `maintenance_record_id` BIGINT FK → `maintenance_records(id)`
   - `reading_key` VARCHAR, `reading_value` VARCHAR

Notes:
- Foreign keys do not cascade deletes in the current schema dump; handle deletions explicitly in application code or migrations.
- The maintenance record uniquely binds to a single maintenance image.
- Production use is recommended with Flyway enabled; dev uses JPA update.

---

## 🔧 Setup & Usage

## 📚 Schema Reference (Dump Summary)

Quick view of the live PostgreSQL schema from `transformer_schema.sql`:

- `transformers`: identity PK; `transformer_type` in {BULK, DISTRIBUTION}.
- `inspections`: identity PK; `status` in {OPEN, IN_PROGRESS, CLOSED}; FK `transformer_id` → `transformers(id)`.
- `transformer_images`: identity PK; env fields (`humidity`, `temperaturec`, `weather`, `location_note`); checks on `image_type` and `weather`; FKs `transformer_id`, `inspection_id`.
- `fault_regions`: identity PK; bbox/centroid fields; manual/audit/soft delete fields; FK `image_id` → `transformer_images(id)`.
- `fault_region_colors`: FK `fault_region_id` → `fault_regions(db_id)`.
- `display_metadata`: identity PK; `timestamp` TIMESTAMP(6); FK `image_id`.
- `display_box_colors`: composite PK (`display_metadata_id`, `color_key`); FK `display_metadata_id`.
- `original_anomaly_results`: identity PK; UNIQUE `image_id`; FK `image_id` → `transformer_images(id)`.
- `maintenance_records`: identity PK; UNIQUE `maintenance_image_id`; `status` in {OK, NEEDS_MAINTENANCE, URGENT_ATTENTION}; FKs `transformer_id`, `inspection_id`, `maintenance_image_id`.
- `maintenance_record_electrical_readings`: composite PK (`maintenance_record_id`, `reading_key`); FK `maintenance_record_id`.
- `app_users`: identity PK; UNIQUE `name`; `occupation` in {ADMIN, INSPECTION_ENGINEER, MAINTENANCE_ENGINEER, TECHNICIAN}.

Prerequisites:
- Java 17, Maven, PostgreSQL 13+

Configure database (dev profile):
- Edit `src/main/resources/application-dev.yml` to set `spring.datasource.url`, `username`, and `password` for your local PostgreSQL.
- Create the database in Postgres:
   ```sql
   CREATE DATABASE transformer;
   ```

Run the server (Windows PowerShell):
```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
./mvnw.cmd spring-boot:run
```

Default admin user:
- On first run, an admin is auto-created: name `admin`, password `admin123`.

Authenticate and call APIs (PowerShell examples):
```powershell
# 1) Login
$login = @{ name = "admin"; password = "admin123" } | ConvertTo-Json
$resp = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType "application/json" -Body $login
$token = $resp.token

# 2) Fetch maintenance form
Invoke-RestMethod -Method Get -Headers @{ "X-Auth-Token" = $token } -Uri "http://localhost:8080/api/transformers/1/maintenance-record-form"

# 3) Create maintenance record
$body = @{
   transformerId = 1
   inspectionId = 123       # optional
   maintenanceImageId = 456
   inspectorName = "Jane Doe"
   status = "NEEDS_MAINTENANCE"
   electricalReadings = @{ voltagePhaseA = "11kV"; currentPhaseA = "120A" }
   recommendedAction = "Tighten connections"
   additionalRemarks = "Minor hotspot observed"
} | ConvertTo-Json -Depth 6

Invoke-RestMethod -Method Post -Headers @{ "X-Auth-Token" = $token } `
   -Uri "http://localhost:8080/api/transformers/1/maintenance-records" `
   -ContentType "application/json" -Body $body

# 4) Update maintenance record
$update = @{ id = 789; status = "OK" } | ConvertTo-Json
Invoke-RestMethod -Method Put -Headers @{ "X-Auth-Token" = $token } `
   -Uri "http://localhost:8080/api/transformers/maintenance-records/789" `
   -ContentType "application/json" -Body $update
```

Notes:
- Ensure the `maintenanceImageId` refers to an image with `image_type = MAINTENANCE` for the same transformer.
- Only users with occupation `MAINTENANCE_ENGINEER` can create/update maintenance records; use the admin to create such users via `POST /api/admin/users`.

---



