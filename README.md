# HeartSync

HeartSync is a microservices based hospital management platform featuring an integrated AI coronary angiography analysis pipeline. It is designed to handle patient management, ECG processing, clinical reporting and AI driven analysis.

## Architecture

The platform follows a robust microservices architecture, ensuring scalability and isolation.

### Core Components
* **Frontend**: React + Vite
* **API Gateway**: Spring Cloud Gateway (Single entry point, JWT validation, routing)
* **Service Registry**: Eureka Server (Dynamic service discovery)

### Microservices
* **IAM Service**: Identity and access management.
* **Patient Service**: Patient records and demographic data management.
* **ECG Service**: ECG data handling and processing.
* **Reporting Service**: Clinical report generation.
* **AI Inference Service (Java)**: Coordinates AI workflows and metadata.
* **AI Python Service (FastAPI)**: Handles core AI execution, including vessel segmentation and QCA.

### Infrastructure & Storage
* **Databases**: 
  * PostgreSQL (Database-per-service pattern for IAM, Patient, ECG, and Reporting).
  * MongoDB (Flexible schema for AI metadata and segmentation results).
* **Message Broker**: RabbitMQ (Asynchronous event-driven communication).
* **Object Storage**: MinIO (S3-compatible storage for ECG images, generated PDFs, and angiograms).

## Getting Started

The entire platform is containerized and orchestrated using Docker Compose.

### Prerequisites
* Docker
* Docker Compose

### Running the Application

To run the full project (both the backend infrastructure/microservices and the frontend development server), follow these steps:

1. **Start all backend services and infrastructure:**
   From the root directory, run the following command to build and start all Docker containers:
   ```bash
   docker compose --profile all up -d --build

   ```

2. **Run the Frontend locally** (Navigate into the frontend directory to install dependencies and start the development server):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

### Accessing Services

Once started, the following services will be available:
* **Frontend**: `http://localhost:<FRONTEND_PORT>` (configured in your `.env`)
* **RabbitMQ Management**: `http://localhost:15672`
* **MinIO Console**: `http://localhost:9001`
* **Eureka Dashboard**: `http://localhost:8761`
* **API Gateway**: `http://localhost:8080`
