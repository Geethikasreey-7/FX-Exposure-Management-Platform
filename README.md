# FX Exposure Management Platform

A backend REST API for managing foreign exchange (FX) exposures, derivatives, hedge allocations, and risk analytics.

The platform helps organizations track their currency exposure, connect exposures with hedging instruments, calculate hedge ratios, and analyze overall FX risk through secure REST APIs.

## Features

- JWT-based authentication and authorization
- User and role management
- FX exposure management
- Derivative management
- Hedge allocation between exposures and derivatives
- Risk analytics and hedge ratio calculation
- Filtering and searching of exposures and derivatives
- Input validation and centralized exception handling
- Swagger/OpenAPI API documentation
- PostgreSQL database integration
- Comprehensive unit and controller testing
- Compatible with cloud PostgreSQL databases such as Neon

## System Workflow

User Registration
       ↓
JWT Authentication
       ↓
Create FX Exposure
       ↓
Create Derivative
       ↓
Allocate Derivative to Exposure
       ↓
Calculate Hedge Ratio
       ↓
Analyze FX Risk
       ↓
Risk Analytics & Summary

## Example

Suppose a company expects to receive:

Exposure:
USD 1,000,000

The company uses derivatives to hedge:

Forward Contract → USD 300,000
Option → USD 200,000

Total Hedged Amount:

USD 500,000

Hedge Ratio:

Hedge Ratio = (500,000 / 1,000,000) × 100
            = 50%

Therefore, the exposure status becomes:

PARTIALLY_HEDGED

## Main Modules

### 1. User Management

Provides APIs for managing users and their roles.

Supported roles:

- ADMIN
- MANAGER
- ANALYST

User passwords are securely stored using BCrypt hashing.

### 2. Authentication

The application uses JWT-based authentication.

Authentication flow:

Login
  ↓
Validate Username & Password
  ↓
Generate JWT
  ↓
Client sends JWT with requests
  ↓
JWT Filter validates token
  ↓
Access protected APIs

Protected APIs require:

Authorization: Bearer <JWT_TOKEN>

### 3. Exposure Management

An exposure represents a company's potential foreign currency gain or obligation.

Supported exposure types:

- RECEIVABLE
- PAYABLE
- FORECAST
- ASSET
- LIABILITY

Supported exposure statuses:

- OPEN
- PARTIALLY_HEDGED
- FULLY_HEDGED
- CLOSED

Example:

Exposure Reference : EXP-001
Type               : RECEIVABLE
Currency           : USD
Base Currency      : INR
Amount             : 1,000,000

Exposure APIs:

POST   /api/exposures
GET    /api/exposures
GET    /api/exposures/{id}
PUT    /api/exposures/{id}
DELETE /api/exposures/{id}

Filtering is supported using:

- Currency
- Exposure type
- Status

### 4. Derivative Management

The platform manages financial instruments used to hedge FX exposure.

Supported derivative types:

- SPOT
- FORWARD
- SWAP
- OPTION

Supported derivative statuses:

- ACTIVE
- MATURED
- SETTLED
- CANCELLED

For options:

- CALL
- PUT

Example:

Derivative Reference : DER-001
Type                 : FORWARD
Base Currency        : USD
Quote Currency       : INR
Notional Amount      : 500,000
Forward Rate         : 84.50

Derivative APIs:

POST   /api/derivatives
GET    /api/derivatives
GET    /api/derivatives/{id}
PUT    /api/derivatives/{id}
DELETE /api/derivatives/{id}

### 5. Hedge Allocation

This module connects an FX exposure with a derivative.

For example:

Exposure:
USD 1,000,000

Derivative:
USD 500,000 Forward

Allocation:
USD 300,000

The system automatically checks:

- Whether the exposure exists
- Whether the derivative exists
- Currency compatibility
- Exposure over-allocation
- Derivative over-allocation

It also automatically recalculates the exposure's hedge status.

Hedge Allocation APIs:

POST   /api/hedge-allocations
GET    /api/hedge-allocations
GET    /api/hedge-allocations/{id}
PUT    /api/hedge-allocations/{id}
DELETE /api/hedge-allocations/{id}

Additional APIs:

GET /api/hedge-allocations/exposure/{exposureId}

GET /api/hedge-allocations/derivative/{derivativeId}

GET /api/hedge-allocations/exposure/{exposureId}/summary

### 6. Risk Analytics

The risk analytics module provides an overall view of FX exposure and hedging.

It calculates:

- Total exposure
- Total hedged amount
- Total unhedged amount
- Hedge ratio
- Exposure status counts
- Currency-wise risk information
- Exposure-specific risk information

Risk Analytics APIs:

GET /api/risk-analytics/summary

GET /api/risk-analytics/currencies

GET /api/risk-analytics/exposures

GET /api/risk-analytics/exposures/{exposureId}

Hedge Ratio:

Hedge Ratio = (Total Hedged Amount / Exposure Amount) × 100

Example:

Exposure = 1,000,000
Hedged   = 500,000

Hedge Ratio = 50%
Unhedged Amount = 500,000

## Security

The application uses:

- Spring Security
- JWT authentication
- BCrypt password hashing
- Bearer token authentication
- Protected REST endpoints
- Centralized authentication error handling

Sensitive configuration such as database credentials and JWT secrets is supplied through environment variables.

## Technology Stack

Java 21
Spring Boot 3.3.5
Spring Security
JWT
Spring Data JPA
Hibernate
PostgreSQL
Neon Cloud PostgreSQL
Maven
Bean Validation
Lombok
Swagger/OpenAPI
JUnit 5
Mockito

## Architecture

The project follows a layered architecture:

Client
  ↓
REST Controller
  ↓
Service
  ↓
Repository
  ↓
PostgreSQL

Project Structure:

src

├── main

│   ├── java

│   │   └── com.fxexposure

│   │       ├── config

│   │       ├── controller

│   │       ├── dto

│   │       ├── entity

│   │       ├── exception

│   │       ├── repository

│   │       ├── security

│   │       └── service

│   │

│   └── resources

│       └── application.properties

│

└── test

    └── java
    
        └── com.fxexposure

## Prerequisites

Before running the project, make sure you have:

- Java 21
- Maven
- PostgreSQL database
- Git

A cloud PostgreSQL provider such as Neon can also be used.

## Configuration

The application reads database and JWT configuration from environment variables.

Example:

DB_URL=jdbc:postgresql://<your-db-host>:<port>/<database-name>?sslmode=require
DB_USERNAME=<your-db-username>
DB_PASSWORD=<your-db-password>
JWT_SECRET=<your-jwt-secret>
JWT_EXPIRATION=3600000
PORT=8080

Do not commit real database passwords, JWT secrets, API keys, or other credentials to GitHub.

The repository contains .env.example only as a configuration template.

## Running the Application

### 1. Clone the repository

git clone https://github.com/Geethikasreey-7/FX-Exposure-Management-Platform.git

### 2. Navigate to the project

cd FX-Exposure-Management-Platform

### 3. Configure environment variables

Set your PostgreSQL and JWT configuration.

### 4. Build the project

mvn clean install

### 5. Run the application

mvn spring-boot:run

The application runs on:

http://localhost:8080

## API Documentation

Swagger UI is available at:

http://localhost:8080/swagger-ui/index.html

The Swagger interface can be used to:

- View available APIs
- Understand request/response formats
- Authenticate using JWT
- Test REST endpoints

## Testing

The project contains unit and controller tests covering:

- User management
- Authentication
- JWT security
- Exposure management
- Derivative management
- Hedge allocation
- Risk analytics
- Validation
- Exception handling
- Business rules

Complete automated test suite:

131 tests
131 passed
0 failures
0 errors
0 skipped

## Business Rules

### Exposure Allocation

The system prevents allocating more than the available exposure amount.

Available Exposure
= Exposure Amount - Existing Allocations

### Derivative Allocation

The system also prevents allocating more than the derivative's notional amount.

Available Derivative
= Derivative Notional - Existing Allocations

### Exposure Status

The exposure status is automatically updated based on the hedge amount.

0% Hedged
   ↓
OPEN

Between 0% and 100%
   ↓
PARTIALLY_HEDGED

100% or more
   ↓
FULLY_HEDGED

### Currency Validation

The system validates that the currencies involved in an exposure and its hedge allocation are compatible.

## Project Objective

The main objective of this project is to build a secure backend platform that demonstrates how foreign exchange exposure can be tracked and managed using software.

The system connects:

FX Exposure
     ↓
Derivatives
     ↓
Hedge Allocation
     ↓
Hedge Ratio
     ↓
Risk Analytics

This provides a structured backend for understanding and managing currency-related financial risk.

## Future Enhancements

Possible future improvements include:

- Role-based endpoint authorization
- Real-time foreign exchange rates
- Derivative valuation
- Mark-to-market calculations
- Value-at-Risk (VaR)
- FX risk alerts
- Hedging policy monitoring
- Scheduled maturity notifications
- Portfolio-level risk dashboards
- Frontend dashboard using React
- Docker deployment
- CI/CD pipeline

## Disclaimer

This project is intended for educational and portfolio purposes.

The risk calculations and financial logic implemented in this project are simplified demonstrations and should not be considered professional financial advice or a production-grade trading/risk management system.

## Author

Geethika Sree

B.Tech – Computer Science and Engineering

GitHub:
https://github.com/Geethikasreey-7

If you find this project useful, consider giving the repository a ⭐ on GitHub.
