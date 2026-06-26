<div align="center">
  <h1>🚀 AI-Powered Learning Platform</h1>
  <p><strong>A Next-Generation Educational Ecosystem leveraging Spring Boot and Python ML</strong></p>
  
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  [![Java: 17](https://img.shields.io/badge/Java-17-blue)](https://www.java.com/)
  [![Spring Boot: 3.x](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
  [![Python: 3.12](https://img.shields.io/badge/Python-3.12-blue)](https://www.python.org/)
  [![FastAPI](https://img.shields.io/badge/FastAPI-0.109-009688.svg?logo=fastapi)](https://fastapi.tiangolo.com/)
</div>

<hr />

## 📖 Overview

The **AI-Powered Learning Platform (AIPLP)** is a sophisticated, full-stack educational system designed to provide hyper-personalized learning experiences. By combining a robust **Spring Boot** backend for user and content management with an advanced **Python AI Microservice** (featuring FAISS and LLM integrations), the platform dynamically generates, evaluates, and adapts learning paths based on real-time user performance.

---

## ✨ Key Features

- **🧠 Adaptive Learning Paths**: Dynamically generated curriculum utilizing AI-based assessment of user strengths and weaknesses.
- **⚡ Advanced Assessment Engine**: Automated quiz evaluation and learning path verification powered by ML models.
- **🔍 Semantic Search (FAISS)**: Blazing-fast retrieval of relevant learning materials and vector embeddings for contextual learning.
- **📊 Comprehensive Analytics**: Built-in tracking of quiz results, user surveys, and learning progress.
- **🏆 Certification Schema**: Automated generation and validation of course completion certificates.

---

## 🏗️ Architecture

The platform operates on a robust microservices architecture:

1. **Core Backend (`aiplp-spring-1.0.2`)**
   - Built with **Java / Spring Boot** and **Maven**.
   - Handles REST API requests, database transactions, authentication, and core application logic.
   - Manages quizzes, survey results, and user certification data.

2. **AI Inference Server (`aiplp-python-0.1`)**
   - Built with **Python** (utilizing `uvicorn` / `fastapi`).
   - Handles complex algorithmic tasks such as the `learning_assessment_server.py`.
   - Utilizes `FAISS` for vector indexing and intelligent content recommendations.

---

## 💻 Tech Stack

### Backend & Core APIs
- **Java 17+**
- **Spring Boot** (Web, Data JPA, Security)
- **Maven** (Dependency Management)

### AI & Data Processing
- **Python 3.12**
- **FastAPI / Uvicorn** (High-performance API layer)
- **FAISS** (Facebook AI Similarity Search)
- **SQLite** (`learning_data.db` for localized AI data caching)

---

## 📂 Project Structure

```text
AI-Powered-Learning-Platform/
├── aiplp-spring-1.0.2/              # Core Spring Boot Application
│   ├── src/                         # Java source code
│   ├── data/                        # JSON data stores (quizzes, surveys)
│   ├── pom.xml                      # Maven configuration
│   └── update_certificate_schema.sql# Database migration scripts
│
└── aiplp-python-0.1/                # Python AI Microservice
    ├── learning_assessment_server.py# Main AI inference server
    ├── faiss_index.bin              # Pre-computed vector embeddings
    ├── learning_data.db             # ML and assessment data store
    └── generated_learning_paths/    # Outputs from the path generator
```

---

## 🚀 Getting Started

### 1. Spring Boot Core Service
Navigate to the Spring Boot directory and run the application using the Maven wrapper:
```bash
cd aiplp-spring-1.0.2/aiplp-spring-1.0.2
./mvnw spring-boot:run
```
*(Ensure you have Java 17+ installed and your `JAVA_HOME` is set correctly)*

### 2. Python AI Service
Navigate to the Python directory, install dependencies (if a virtual environment is set up), and run the server:
```bash
cd aiplp-python-0.1/aiplp-python-0.1
# Assuming requirements are installed
python learning_assessment_server.py
```
*(By default, this spins up an assessment server communicating with the Spring backend)*

---

## 📜 License

This project is licensed under the **MIT License**.

<div align="center">
  <i>Empowering the future of education with Artificial Intelligence.</i>
</div>
