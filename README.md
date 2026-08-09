# Spotify Payment Manager Bot 🤖💶

A Spring Boot-powered Telegram bot designed to manage shared recurring expenses—originally created for tracking Spotify Family plan subscriptions among friends.

## Features

- **Automated Expense Tracking:** Manages monthly dues and individual friend balances seamlessly.
- **Telegram Interface:** Interactive bot commands allowing users to check their current debt, payment methods, and transaction history.
- **Admin Control:** Dedicated administrative commands to record payments, manually adjust or override member balances, and broadcast reminders.
- **Lightweight Storage:** Uses SQLite for local persistent storage, avoiding the overhead of external database instances.
- **Containerized Deployment:** Fully Dockerized and ready to deploy on cloud platforms such as Render where I've already done for my personal bot.

---

## Tech Stack

- **Java 25**
- **Spring Boot 3 / JPA (Hibernate 7)**
- **SQLite**
- **Telegram Bots Spring Boot Starter**
- **Docker**

---

## Getting Started

### Prerequisites

- Java 25 Development Kit (JDK)
- Maven 3.9+
- Docker (optional, for containerization)
- A Telegram Bot Token (obtained via [@BotFather](https://t.me/BotFather))

---

## Configuration

The application reads configuration settings from `src/main/resources/application.yaml`. You can set environment variables or update the YAML file directly for local development. If you want to run locally, you have to create a new <br> application-local.yaml file with local credentials such as bot-token or credentials for accessing DB (ex. MySQL)

### Environment Variables

| Variable | Description |
| :--- | :--- |
| `TELEGRAM_BOT_USERNAME` | The handle of your Telegram bot (e.g., `MySpotifyBot`) |
| `TELEGRAM_BOT_TOKEN` | The API token provided by BotFather |
| `TELEGRAM_ADMIN_ID` | Telegram User ID of the administrator |

---

## Local Development

1. **Clone the repository:**
   ```bash
   git clone https://github.com/AntonioTauro/SpotifyPaymentManager.git
   cd SpotifyPaymentManager
   ```

2. **Build the project:**
   ```bash
   ./mvnw clean package -DskipTests
   ```

3. **Run locally:**
   ```bash
   ./mvnw spring-boot:run
   ```

The application will create a local SQLite database file (`spotify_debt.db`) automatically on first run.

---

## Docker & Cloud Deployment (e.g., Render)

This repository includes a multi-stage `Dockerfile` optimized for containerized environments.

### Building and Running via Docker

```bash
# Build the Docker image
docker build -t spotify-payment-manager .

# Run the container
docker run -d \
  -e TELEGRAM_BOT_USERNAME="YourBotUsername" \
  -e TELEGRAM_BOT_TOKEN="YourBotToken" \
  -e TELEGRAM_ADMIN_ID="YourTelegramID" \
  --name YOUR BOT NAME \
  spotify-payment-manager
```

### Deploying to Render

1. Create a new **Web Service** on [Render](https://render.com/).
2. Connect your GitHub repository.
3. Select **Docker** as the environment.
4. Add your runtime environment variables (`TELEGRAM_BOT_USERNAME`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_ADMIN_ID`).
5. Click **Deploy**.
