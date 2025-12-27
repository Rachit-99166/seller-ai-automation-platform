
# AI-Driven 3P Seller Automation Platform

# Overview

This project is an AI-powered Seller Automation Platform built with Spring Boot, MongoDB, and Groq LLMs.
It enables sellers to manage products, pricing, inventory, logistics, and supply chain decisions using natural language commands.

The system combines:

AI-driven decision making

Real-time inventory monitoring

Distribution center orchestration

Automated product audits


## Skills

Backend: Java 21, Spring Boot 3.x, Spring Data MongoDB.

Intelligence: Groq Cloud API (Llama 3 70B), Prompt Engineering, Automated Reasoning.

DevOps: Docker, Docker Compose, MongoDB Compass.

Frontend: Thymeleaf, Bootstrap 5, Responsive UI Design.


## How To Run
Clone the Repo: git clone <url>

Environment Setup: Add your Groq API Key to src/main/resources/application.properties.

Start Database: Run docker-compose up --build -d mongo to launch MongoDB.

Launch App: Run mvn spring-boot:run or use your IDE.

Access: Open http://localhost:8080.

Stop Database: docker compose down -v to stop docker.


## Impact & Why This Matters Today

Modern e-commerce systems must:

React instantly to market signals

Automate decision-making

Reduce human operational cost

This system demonstrates how AI can act as a real operational brain, not just a chatbot:

Sellers interact using plain English

AI validates, audits, and optimizes

Logistics & inventory adapt dynamically

Such architectures are increasingly used in:

Smart commerce platforms

Supply chain intelligence systems

AI-native SaaS products


Why It Should Be Implemented:

Reduces manual operations

Scales seller intelligence

Enables faster go-to-market

Aligns with AI-first industry direction


## Screenshot
![Image](https://github.com/user-attachments/assets/fcb55abd-46a0-448e-8f54-63b75eddc518)

![Image](https://github.com/user-attachments/assets/061c1d57-1b92-4cf4-8319-3ced9f6d88e7)
