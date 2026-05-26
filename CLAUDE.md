# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**HeartSync** is a distributed microservices platform for ECG (electrocardiogram) analysis and clinical cardiac reporting. It demonstrates modern cloud-native architecture patterns including service discovery, API gateway routing, event-driven communication, and containerized deployment.

The system processes patient medical data, uploads ECG files, performs AI-based cardiac analysis, and generates clinical PDF reports through an event-driven pipeline.

## Architecture

### High-Level System Design

HeartSync uses a **microservices architecture** with the following key principles:

- **Database-per-service pattern**: Each service has its own isolated PostgreSQL database (or MongoDB for AI) to prevent tight coupling
- **Service discovery**: Eureka server handles dynamic service registration and load balancing
- **API Gateway**: Spring Cloud Gateway provides a single entry point with JWT validation and routing
- **Event-driven communication**: RabbitMQ enables asynchronous, decoupled service-to-service communication
- **Object storage**: MinIO (S3-compatible) stores ECG files and generated PDF reports
- **Frontend-backend separation**: React/Vite frontend communicates exclusively through the API Gateway

### Services and Responsibilities

#### Infrastructure Services

1. **Eureka Server** (infrastructure/eureka-server)
   - Service registry and discovery
   - Port: 8761
   - Dashboard: http://localhost:8761
   - No database dependency

2. **API Gateway** (infrastructure/api-gateway)
   - Spring Cloud Gateway (WebFlux-based, non-blocking)
   - Single entry point for all client requests (port 8080)
   - Routing rules:
     - /api/auth/** → iam-service (no JWT validation)
     - /api/patients/** → patient-service
     - /api/ecg/** → ecg-service
     - /api/ai/** → ai-inference-service
     - /api/reports/** → reporting-service
   - JWT validation and token parsing (excludes /api/auth/**)
   - Injects X-User-Id and X-User-Role headers into downstream requests
   - CORS configuration for http://localhost:3000 and http://localhost:5173

#### Microservices

1. **IAM Service** (services/iam-service, port 8082)
   - User registration and authentication
   - JWT token generation (24-hour expiry)
   - Database: PostgreSQL (iam_db)
   - No async dependencies
   - Endpoints: POST /api/auth/register, POST /api/auth/login

2. **Patient Service** (services/patient-service, port 8081)
   - Patient record management (CRUD operations)
   - Database: PostgreSQL (patient_db)
   - Listens to X-User-Id header from gateway to track record ownership
   - No async dependencies
   - Endpoints: GET/POST/PUT/DELETE /api/patients

3. **ECG Service** (services/ecg-service, port 8083)
   - ECG file upload and metadata storage
   - Database: PostgreSQL (ecg_db)
   - Integrates with MinIO (S3) for file storage (bucket: ecg-files)
   - Publishes events to RabbitMQ: ecg.analysis.queue
   - Receives files up to 50MB (multipart/form-data)

4. **AI Inference Service** (services/ai-inference-service, port 8084)
   - Cardiac analysis using AI/ML models
   - Database: MongoDB (ai_db) for flexible document storage of analysis results
   - Consumes from: ecg.analysis.queue
   - Publishes to: ai.results.queue
   - Stores segmentation results and model outputs as JSON documents

5. **Reporting Service** (services/reporting-service, port 8085)
   - Clinical report generation in PDF format
   - Database: PostgreSQL (reporting_db)
   - Consumes from: ai.results.queue
   - Publishes to: report.ready.queue (for WebSocket/SSE updates)
   - Uses OpenPDF library for PDF generation
   - Stores generated PDFs in MinIO

#### Frontend

- React 18 + Vite (frontend/)
- Port 3000 (production) / 5173 (dev with Vite)
- Styled with Tailwind CSS
- Router-based pages: Login, Dashboard, Patient Detail
- Authentication context stores JWT token and user data in localStorage
- API client abstracts axios calls to the gateway

### Technology Stack

Backend: Spring Boot 3.2.5, Spring Cloud 2023.0.1, Java 17, Maven 3.9
Databases: PostgreSQL 16 (4 instances), MongoDB 7
Message Broker: RabbitMQ 3.13
Object Storage: MinIO (S3-compatible)
Frontend: React 18, Vite 5, Tailwind CSS
Service Discovery: Netflix Eureka
API Gateway: Spring Cloud Gateway (WebFlux)
JWT: JJWT 0.12.3
PDF Generation: OpenPDF 1.3.30
Containerization: Docker + Docker Compose

### Event Flow

User uploads ECG → ECG Service publishes ecg.analysis.queue
→ AI Service consumes, publishes ai.results.queue
→ Reporting Service consumes, generates PDF, publishes report.ready.queue
→ Frontend notified (WebSocket/SSE - pending implementation)

## Build Commands

### Start Full Stack

docker compose up -d --build                  # All services with build
docker compose --profile infra up -d          # Infrastructure only (no microservices)
docker compose down -v                        # Stop and remove volumes

### Frontend Development

cd frontend
npm install                                   # Install dependencies
npm run dev                                   # Vite dev server (port 5173)
npm run build                                 # Production build
npm run preview                               # Preview production build

### Java Services

cd services/patient-service (or any service)
mvn clean package                             # Build JAR
mvn clean package -DskipTests                 # Skip tests (faster)
mvn test                                      # Run all tests
mvn test -Dtest=PatientControllerTest        # Run specific test
mvn dependency:tree                           # View dependency tree

docker compose build patient-service          # Rebuild single service

## Configuration

### Environment Variables (.env)

JWT_SECRET                          Must be 32+ chars for HMAC-SHA256
PATIENT_DB_NAME, PATIENT_DB_USER    Database credentials per service
MONGO_ROOT_USER, MONGO_ROOT_PASSWORD MongoDB auth for AI Service
RABBITMQ_USER, RABBITMQ_PASSWORD    RabbitMQ auth
MINIO_ROOT_USER, MINIO_ROOT_PASSWORD MinIO (S3) auth
<SERVICE>_PORT                      Port mapping for each service

### Key Configuration Files

Each service has src/main/resources/application.yml with:
- Server port, application name (for Eureka registration)
- Database connection details (from env vars)
- RabbitMQ/MongoDB connection settings
- Eureka client configuration
- Management endpoints (health, info)

infrastructure/api-gateway/src/main/resources/application.yml:
- Spring Cloud Gateway routes (path predicates to service URIs)
- CORS policy (allows localhost:3000 and 5173)
- JWT secret injection from env var
- Eureka client configuration (5s registry fetch)

frontend/vite.config.js:
- Proxy: /api requests to http://localhost:8080
- Dev server port: 5173

## Running the Application

### Docker Compose (Recommended)

docker compose up -d --build
docker compose logs -f patient-service        # View service logs
docker compose down                           # Stop all services

### Access Points

Frontend:         http://localhost:3000
API Gateway:      http://localhost:8080
Eureka Dashboard: http://localhost:8761
RabbitMQ Console: http://localhost:15672 (guest/guest)
MinIO Console:    http://localhost:9001 (minio_admin/minio_pass)

### Local Development (Without Docker)

Start infrastructure manually (PostgreSQL, MongoDB, RabbitMQ, MinIO)
Then run each component:
- Eureka: cd infrastructure/eureka-server && mvn spring-boot:run
- API Gateway: cd infrastructure/api-gateway && mvn spring-boot:run
- Microservices: cd services/<service> && mvn spring-boot:run
- Frontend: cd frontend && npm run dev

## API Usage

### Authentication

# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"doctor@hospital.com","password":"Pass123","fullName":"Dr. Jane","role":"DOCTOR"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"doctor@hospital.com","password":"Pass123"}'

# Response: { "accessToken": "eyJhbG...", "userId": "...", "email": "...", "role": "..." }

### Patient Management

TOKEN="<your_token>"

# Create patient
curl -X POST http://localhost:8080/api/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","age":45,"medicalHistory":"Hypertension"}'

# Get all patients
curl http://localhost:8080/api/patients -H "Authorization: Bearer $TOKEN"

# Search patients by name
curl "http://localhost:8080/api/patients?name=John" -H "Authorization: Bearer $TOKEN"

# Get patient by ID
curl http://localhost:8080/api/patients/{id} -H "Authorization: Bearer $TOKEN"

### ECG Upload

curl -X POST http://localhost:8080/api/ecg/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@ecg_data.csv" \
  -F "patientId={patient_id}"

Full API reference: `swagger.yaml` at the repo root (OpenAPI 3.0 — load in Swagger UI or Postman)

## Code Organization

### Spring Boot Services

Each microservice structure:
com/heartsync/<service>/
├── <Service>Application.java         @SpringBootApplication, @EnableDiscoveryClient
├── controller/                       @RestController with REST endpoints
├── service/                          @Service for business logic
├── entity/                           @Entity for JPA models
├── repository/                       @Repository extends JpaRepository
├── dto/                              Request/response DTOs
├── config/                           Spring configuration beans
├── event/                            Event classes for RabbitMQ
└── consumer/                         RabbitMQ message listeners

Key annotations:
- @EnableDiscoveryClient: Register with Eureka
- @EnableEurekaServer: Turns app into service registry
- @RestController: REST endpoint provider
- @RequestMapping("/api/..."): Base path for routes
- @PostMapping, @GetMapping, etc.: HTTP method handlers
- @RequestHeader("X-User-Id"): Extract gateway-injected headers
- @Valid: Validate request DTOs
- @RequiredArgsConstructor: Lombok constructor injection

### Frontend Structure

src/
├── pages/              LoginPage, DashboardPage, PatientPage
├── components/        PrivateRoute, Navbar, AddPatientModal
├── context/           AuthContext (global auth state)
├── api/               axios client and API functions
├── App.jsx            Route definitions
└── main.jsx           Entry point

## Important Implementation Details

### JWT Handling

- Generation: IAM Service creates JWT with user claims
- Validation: API Gateway validates using JWT_SECRET
- Storage: Frontend stores in localStorage (key: "token")
- Service trust: Services trust X-User-Id header (gateway pre-validates)

### RabbitMQ Queues

ecg.analysis.queue      ECG Service publishes, AI Service consumes
ai.results.queue        AI Service publishes, Reporting Service consumes
report.ready.queue      Reporting Service publishes (frontend listener - pending)

### MinIO Buckets

ecg-files               Raw ECG uploads
reports                 Generated PDF reports

### Docker Networking

All containers on heartsync-network bridge
Services reach each other by container name (e.g., postgres-patient:5432)
Frontend proxies /api to http://api-gateway:8080

### Build Optimization

Multi-stage Dockerfiles: Maven build → minimal JRE runtime
Layer caching: pom.xml dependencies resolved separately
Frontend: Node build → nginx static server

## Troubleshooting

Service won't register with Eureka:
- Check EUREKA_SERVER_URL env var
- Verify Eureka is healthy: curl http://localhost:8761/actuator/health
- Check service logs for discovery errors

JWT validation fails:
- Ensure JWT_SECRET matches in IAM (generation) and Gateway (validation)
- Check token format: Bearer <token>
- Verify token expiry (24 hours)

RabbitMQ messages not consumed:
- Check credentials in environment variables
- Verify consumer beans defined properly
- Inspect queue at http://localhost:15672

MinIO file upload fails:
- Verify buckets exist: ecg-files, reports
- Check MinIO credentials
- File size must be under 50MB
