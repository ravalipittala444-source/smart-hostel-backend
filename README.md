# Smart Hostel Management System — Backend

A RESTful backend for a Smart Hostel Management System built using **Java, Spring Boot, Spring Data JPA, Maven, Spring Security, JWT authentication, and MySQL**. It provides APIs for managing students, hostel rooms, attendance, complaints, mess menus, users, and dashboard information.

## Features

- **Authentication:** User login and JWT-based authentication.
- **Student Management:** Manage student records.
- **Room Management:** Manage hostel rooms and room allocation.
- **Attendance Management:** Record and retrieve student attendance.
- **Complaint Management:** Create and manage hostel complaints.
- **Mess Management:** Manage mess menu information.
- **Dashboard:** Provide hostel management summary information.
- **Security:** Spring Security configuration and protected endpoints.
- **Database Integration:** Persist application data using MySQL and Spring Data JPA.

## Technology Stack

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JSON Web Token (JWT)
- MySQL
- Maven

## Prerequisites

Install the following before running the application:

- Java JDK compatible with the version specified in `pom.xml`
- MySQL Server
- Git (optional)

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/ravalipittala444-source/smart-hostel-backend.git
cd smart-hostel-backend
```

### 2. Create the database

Open MySQL and run:

```sql
CREATE DATABASE hostel_management;
```

### 3. Configure database credentials

Update `src/main/resources/application.properties` with your local database configuration.

Example:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/hostel_management}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD}
```

Make sure these settings match your actual application configuration. Keep passwords and JWT secrets private; do not commit real credentials to GitHub.

### 4. Run the application

On Windows:

```bash
mvnw.cmd spring-boot:run
```

Alternatively, if Maven is installed:

```bash
mvn spring-boot:run
```

The server starts on the port configured in `application.properties` (Spring Boot's default is `8080`).

## API Modules

| Module | Purpose |
|---|---|
| Authentication | Login and authentication |
| Students | Student record management |
| Rooms | Room management and allocation |
| Attendance | Attendance tracking |
| Complaints | Complaint management |
| Mess Menu | Mess menu management |
| Users | User management |
| Dashboard | Dashboard information |

Refer to the controller classes in `src/main/java` for the implemented endpoints and HTTP methods.

## Project Structure

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/Smart_Hostel/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── entity/
│   │   │       ├── repository/
│   │   │       └── security/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── .mvn/
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Running Tests

Run the test suite using:

```bash
mvnw.cmd test
```

## Frontend

This backend is intended to work with the Smart Hostel Management System React frontend.

## Author

Developed as a college project.
