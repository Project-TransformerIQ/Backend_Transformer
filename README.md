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
- Baseline and maintenance image upload, retrieval, and deletion.  
- PostgreSQL database integration with schema management.  
- Production-ready Spring Boot server.  

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
This backend was developed by **Team WeMake Software**:  

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
     CREATE DATABASE transformers_db;
     ```  

   - Update your `application.properties` with database credentials:  
     ```properties
     spring.datasource.url=jdbc:postgresql://localhost:5432/transformers_db
     spring.datasource.username=postgres
     spring.datasource.password=yourpassword
     spring.jpa.hibernate.ddl-auto=update
     ```

3. **Build and run the server**  
   ```bash
   ./mvnw spring-boot:run
   ```
   Or run with a production profile:  
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

4. **Server runs at:**  
   👉 `http://localhost:5509/transformer-thermal-inspection/`

---

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

## 🤝 Contribution Guide  
1. Fork the repository.  
2. Create a feature branch:  
   ```bash
   git checkout -b feature-apiEnhancement
   ```  
3. Commit changes:  
   ```bash
   git commit -m "Improved inspection management APIs"
   ```  
4. Push and create a Pull Request.  

---

## 📌 Future Improvements  
- Authentication & Role-based Access.  
- Image processing (thermal anomaly detection).  
- Report generation (PDF/Excel).  
- Docker support.  

---

## 📄 License  
This project is developed for **academic and research purposes**.  
All rights reserved by the authors.  

---

<p align="center">
⚡ Built with ❤️ by <b>Team WeMake Software</b>
</p>
