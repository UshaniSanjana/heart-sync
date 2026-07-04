# HeartSync

HeartSync is a distributed cardiovascular clinical system for managing patients, ECG uploads, coronary angiogram analysis, and cardiac assessment reports. The platform combines a React frontend, Spring Boot microservices, asynchronous RabbitMQ event processing, object storage, and AI services for ECG and angiogram workflows.

## Features

- User registration and authentication for clinical users.
- Patient registry with demographics and contact information.
- ECG upload, storage, and AI-assisted analysis.
- Coronary angiogram upload with stenosis detection, segmentation, and QCA-style measurements.
- Clinical report generation as downloadable PDF reports.
- Event-driven AI orchestration using RabbitMQ.
- Service discovery through Eureka and centralized routing through Spring Cloud Gateway.

## Architecture Overview

HeartSync follows a microservices architecture. The frontend communicates only with the API Gateway. Backend services own their own data stores and communicate through REST where appropriate and RabbitMQ events for asynchronous AI/reporting workflows.

### Main Components

| Component | Technology | Responsibility |
| --- | --- | --- |
| Frontend | React, Vite, Tailwind CSS | Clinical UI for doctors and patient workflows |
| API Gateway | Spring Cloud Gateway | Single entry point, routing, JWT validation |
| Eureka Server | Spring Cloud Netflix Eureka | Service discovery |
| IAM Service | Spring Boot, PostgreSQL | Authentication, registration, user identity |
| Patient Service | Spring Boot, PostgreSQL | Patient records and demographics |
| ECG Service | Spring Boot, PostgreSQL, MinIO, RabbitMQ | ECG upload, metadata, AI request events |
| AI Inference Service | Spring Boot, MongoDB, RabbitMQ | AI orchestration and result persistence |
| ECG Model Service | FastAPI/Python | ECG image classification model wrapper |
| AI Python Service | FastAPI/Python | Angiogram segmentation and QCA processing |
| Reporting Service | Spring Boot, PostgreSQL, MinIO, RabbitMQ | Cardiac report generation and storage |
| RabbitMQ | RabbitMQ Management image | Event broker for asynchronous workflows |
| MinIO | S3-compatible object storage | ECG files, angiograms, generated PDF reports |

## Event Flow

HeartSync uses a unified AI analysis request flow:

1. A doctor uploads an ECG or coronary angiogram from the frontend.
2. The related service saves the file and metadata.
3. An `AiAnalysisRequestedEvent` is published to RabbitMQ.
4. The AI Inference Service consumes the event.
5. The AI Inference Service routes the request by analysis type:
   - ECG requests go to the ECG model workflow.
   - Angiogram requests go to segmentation and QCA workflows.
6. AI results are stored and published as result events.
7. Reporting uses the completed findings to generate clinical PDF reports.

The AI request event includes operational fields such as request ID, idempotency key, event version, timestamps, and trace information to support retries, observability, and safer processing.

## Repository Structure

```text
heart-sync/
  frontend/                         React + Vite application
  infrastructure/
    api-gateway/                    Spring Cloud Gateway
    eureka-server/                  Eureka service registry
  services/
    iam-service/                    Identity and access management
    patient-service/                Patient records
    ecg-service/                    ECG upload and analysis events
    ai-inference-service/           AI orchestration service
    ai-python-service/              Angiogram AI workflow
    ecg-model-service/              ECG model API
    reporting-service/              PDF report generation
  docker-compose.yml                Full local platform orchestration
  swagger.yaml                      API documentation
```

## Prerequisites

- Docker Desktop
- Docker Compose
- Node.js and npm, only if running the frontend locally outside Docker

## Environment Configuration

The project uses a root `.env` file for service ports, database credentials, RabbitMQ credentials, MinIO credentials, and JWT configuration.

Before starting the system, confirm that `.env` exists in the project root and contains the required values used by `docker-compose.yml`.

## Running the System

### Start Everything With Docker

From the project root:

```bash
docker compose --profile all up -d --build
```

This builds and starts the infrastructure, backend microservices, AI services, and frontend container.

### Start Without Rebuilding

Use this when images are already built and you only want to start containers:

```bash
docker compose --profile all up -d
```

### Rebuild a Specific Service

Use this after changing one service:

```bash
docker compose build <service-name>
docker compose --profile all up -d <service-name>
```

Example:

```bash
docker compose build frontend
docker compose --profile all up -d frontend
```

### Run the Frontend Locally

If you prefer running the React app with Vite instead of the Docker frontend container:

```bash
cd frontend
npm install
npm run dev
```

## Useful Service URLs

Default URLs depend on the values in `.env`, but the usual local addresses are:

| Service | URL |
| --- | --- |
| Frontend | `http://localhost:<FRONTEND_PORT>` |
| API Gateway | `http://localhost:<GATEWAY_PORT>` |
| Eureka Dashboard | `http://localhost:<EUREKA_PORT>` |
| RabbitMQ Management | `http://localhost:<RABBITMQ_MANAGEMENT_PORT>` |
| MinIO Console | `http://localhost:<MINIO_CONSOLE_PORT>` |
| AI Python Health | `http://localhost:<AI_PYTHON_PORT>/health` |
| ECG Model Health | `http://localhost:<ECG_MODEL_SERVICE_PORT>/health` |

## Common Docker Commands

Check running containers:

```bash
docker compose ps
```

View logs for one service:

```bash
docker compose logs -f <service-name>
```

Stop all services:

```bash
docker compose --profile all down
```

Stop services and remove volumes:

```bash
docker compose --profile all down -v
```

Use `down -v` carefully because it removes local database, RabbitMQ, MongoDB, and MinIO data.

## API Documentation

The API contract is documented in:

```text
swagger.yaml
```

Use this file with Swagger UI, Postman, or another OpenAPI-compatible tool to inspect available endpoints.

## Development Notes

- The frontend should call backend APIs through the API Gateway, not individual services directly.
- Each backend service owns its own database or persistence store.
- MinIO stores uploaded clinical files and generated report PDFs.
- RabbitMQ is used for asynchronous AI and reporting workflows.
- AI workflows should be idempotent because messages can be retried.
- Use logs, trace IDs, timestamps, RabbitMQ queues, and service health endpoints when debugging event flow issues.
