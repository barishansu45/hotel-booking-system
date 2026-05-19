# Hotel Booking System

> **SE 4458 — Software Architecture & Design | Group 1**  
> A production-grade, microservices hotel booking platform built with Spring Boot 3 and Next.js 15.

---

## 👥 Team — Group 1

| Name | Name |
|------|------|
| Barış Hansu | Batuhan Salcan |
| Mustafa Berkay Düzenlı | Batıkan Akdeniz |
| Cenk Serbest | Toprak Orman |
| Ilayda Gün | Demir Demirdöğen |
| Aycan Kurt | Ayfernaz Baygın |
| Sümeyye Şencan | Begüm Bal |
| Eren Karcı | Idil Balandı |
| Berk Ateş | |

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Browser / Mobile                       │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTP
          ┌───────────▼───────────┐
          │    Next.js Frontend   │  (port 3000)
          │   (Supabase Auth)     │
          └───────────┬───────────┘
                      │ HTTP → localhost:8080
          ┌───────────▼───────────┐
          │     API Gateway       │  (Spring Cloud Gateway, port 8080)
          └──┬──┬──┬──┬──┬───────┘
             │  │  │  │  │
    ┌────────┘  │  │  │  └──────────────────┐
    │     ┌─────┘  │  └─────────┐           │
    ▼     ▼        ▼            ▼           ▼
┌──────┐ ┌──────┐ ┌──────────┐ ┌────────┐ ┌───────┐
│Hotel │ │Search│ │ Booking  │ │Comment │ │  AI   │
│:8081 │ │:8082 │ │  :8083   │ │ :8084  │ │ :8086 │
└──┬───┘ └──┬───┘ └────┬─────┘ └───┬────┘ └───────┘
   │        │           │           │
   ▼        ▼           ▼           ▼
┌──────┐ ┌──────┐ ┌──────────┐ ┌────────┐ ┌────────────┐
│  PG  │ │Redis │ │    PG    │ │MongoDB │ │Notification│
└──────┘ └──────┘ └──────────┘ └────────┘ │   :8085    │
                                           └────────────┘
```

### Services at a Glance

| Service | Port | Responsibility | Database |
|---------|------|---------------|----------|
| **api-gateway** | 8080 | Routing, CORS | — |
| **hotel-service** | 8081 | Hotels, rooms, availability | PostgreSQL |
| **search-service** | 8082 | Search, filtering, Redis cache | Redis |
| **booking-service** | 8083 | Reservations, queue events | PostgreSQL |
| **comment-service** | 8084 | Reviews & ratings | MongoDB |
| **notification-service** | 8085 | Nightly alerts, email | — |
| **ai-agent-service** | 8086 | OpenAI chat + live search | — |
| **frontend** | 3000 | Next.js 15 App Router UI | — |

---

## 🛠️ Technology Stack

### Backend
- **Java 17** + **Spring Boot 3.2**
- **Spring Cloud Gateway** — API routing & CORS
- **Spring Data JPA + Flyway** — PostgreSQL migrations
- **Spring Data MongoDB** — Comments (CosmosDB-compatible)
- **Spring Security + Supabase JWT** — Authentication & RBAC
- **Azure Service Bus** — Production notification queue

### Frontend
- **Next.js 15** (App Router, TypeScript)
- **Supabase Auth** — Sign-in / Sign-up
- **Recharts** — Rating distribution bar + service radar charts
- **Leaflet** — Hotel map view
- **Tailwind CSS** — Styling

### Infrastructure
- **PostgreSQL 16** — Hotels, rooms, bookings
- **MongoDB 7** — Reviews / comments
- **Redis 7** — Hotel search cache (1 h TTL)
- **Docker + Docker Compose** — Local & Azure deployment
- **Azure Container Apps** — Cloud hosting

---

## 🚀 Quick Start — Local Development

### Prerequisites
- Docker Desktop (recommended) **or** Java 17 + Maven 3.9 + Node 18+ installed locally
- Git

### Option A — Full Stack with Docker (recommended)

```bash
git clone <repo-url>
cd hotel-booking-system

# Start everything (builds images on first run, ~3 min)
docker-compose up --build

# Services are now live:
#   Frontend   → http://localhost:3000
#   API Gateway → http://localhost:8080/api/v1
```

### Option B — Run Services Individually

```bash
# 1. Start infrastructure
docker-compose up postgres redis mongodb -d

# 2. Start each backend service
cd backend/hotel-service    && mvn spring-boot:run &
cd backend/search-service   && mvn spring-boot:run &
cd backend/booking-service  && mvn spring-boot:run &
cd backend/comment-service  && mvn spring-boot:run &
cd backend/notification-service && mvn spring-boot:run &
cd backend/ai-agent-service && mvn spring-boot:run &
cd backend/api-gateway      && mvn spring-boot:run &

# 3. Start frontend
cd frontend && npm install && npm run dev
```

---

## 🌐 API Reference (through gateway `http://localhost:8080/api/v1`)

### Hotel Service
| Method | Path | Auth |
|--------|------|------|
| `GET` | `/hotels` `?page&size&sortBy` | Public |
| `GET` | `/hotels/{id}` | Public |
| `GET` | `/rooms/hotel/{hotelId}` | Public |
| `GET` | `/availability/room/{roomId}` | Public |
| `POST` | `/admin/hotels` | **ADMIN JWT** |
| `PUT` | `/admin/hotels/{id}` | **ADMIN JWT** |
| `DELETE` | `/admin/hotels/{id}` | **ADMIN JWT** |
| `POST` | `/admin/rooms` | **ADMIN JWT** |
| `PUT` | `/admin/rooms/{id}` | **ADMIN JWT** |
| `DELETE` | `/admin/rooms/{id}` | **ADMIN JWT** |
| `POST` | `/admin/availability` | **ADMIN JWT** |
| `PATCH` | `/availability/{id}/decrease` | Internal |

### Search Service
| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/search` | Body: `destination`, `checkInDate`, `checkOutDate`, `guests`; 15% discount for authenticated users |
| `GET` | `/search/{hotelId}` | Hotel detail with enriched pricing |

### Booking Service
| Method | Path | Auth |
|--------|------|------|
| `POST` | `/bookings` | JWT required |
| `GET` | `/bookings/user/{userId}` `?page&size` | JWT required |

### Comment Service
| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/comments` | Body includes optional `serviceRatings` map |
| `GET` | `/comments/hotel/{hotelId}` `?page&size&sortBy&sortDir` | Paginated |
| `GET` | `/comments/hotel/{hotelId}/all` | Full list (used by rating graphs) |

### AI Agent Service
| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/ai/chat` | Body: `message`, optional `conversationId`, `userId` |

---

## 🔑 Admin Access Setup (Supabase)

Hotel management endpoints (`/admin/**`) require a JWT with `ROLE_ADMIN`. To grant admin:

1. Go to **Supabase Dashboard → Authentication → Users**
2. Select the user → click **Edit** → set `app_metadata`:
   ```json
   { "role": "admin" }
   ```
3. User must **sign out and sign back in** so the new token is issued.

---

## 🔐 Environment Variables

### Backend Services (docker-compose defaults shown)

| Variable | Used by | Description |
|----------|---------|-------------|
| `DATABASE_URL` | hotel, booking | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | hotel, booking | DB username |
| `DATABASE_PASSWORD` | hotel, booking | DB password |
| `COSMOS_DB_URI` | comment | MongoDB / Cosmos URI |
| `COSMOS_DB_NAME` | comment | Database name |
| `REDIS_HOST` | search | Redis hostname |
| `HOTEL_SERVICE_URL` | search, booking, notification, ai | `http://hotel-service:8081/api/v1` |
| `AZURE_SERVICEBUS_CONNECTION_STRING` | booking, notification | Azure Service Bus (optional locally) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | notification | SMTP credentials (optional) |
| `ADMIN_ALERT_EMAIL` | notification | Nightly alert recipient |
| `OPENAI_API_KEY` | ai | OpenAI API key |
| `OPENAI_MODEL` | ai | Default: `gpt-3.5-turbo` |
| `SUPABASE_JWT_SECRET` | hotel, search | Supabase JWT signing secret |

### Frontend

| Variable | Description |
|----------|-------------|
| `NEXT_PUBLIC_API_URL` | Gateway base URL (e.g. `http://localhost:8080/api/v1`) |
| `NEXT_PUBLIC_AI_API_URL` | AI gateway path (e.g. `http://localhost:8080/api/v1/ai`) |
| `NEXT_PUBLIC_SUPABASE_URL` | Supabase project URL |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Supabase anonymous/publishable key |

> ⚠️ `NEXT_PUBLIC_*` variables are **baked into the image at build time**.  
> When building for Azure, pass them as build args (see Azure deployment section below).

---

## ☁️ Azure Deployment

### Prerequisites
- Azure CLI installed (`az login` completed)
- Docker Desktop running
- An active Azure subscription

### Step 1 — Create Resources

```bash
# Variables — change these
RESOURCE_GROUP="hotel-booking-rg"
LOCATION="westeurope"
ACR_NAME="hotelbookingacr"          # must be globally unique, lowercase
POSTGRES_SERVER="hotelbooking-pg"
REDIS_NAME="hotelbooking-redis"

# Resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Azure Container Registry
az acr create --resource-group $RESOURCE_GROUP --name $ACR_NAME --sku Basic
az acr login --name $ACR_NAME

# Azure Database for PostgreSQL (Flexible Server)
az postgres flexible-server create \
  --resource-group $RESOURCE_GROUP \
  --name $POSTGRES_SERVER \
  --admin-user pgadmin --admin-password "<strong-password>" \
  --sku-name Standard_B1ms \
  --public-access 0.0.0.0

az postgres flexible-server db create \
  --resource-group $RESOURCE_GROUP \
  --server-name $POSTGRES_SERVER \
  --database-name hoteldb

# Azure Cache for Redis
az redis create \
  --resource-group $RESOURCE_GROUP \
  --name $REDIS_NAME \
  --sku Basic --vm-size c0

# Azure Cosmos DB (MongoDB API)
az cosmosdb create \
  --resource-group $RESOURCE_GROUP \
  --name hotelbooking-cosmos \
  --kind MongoDB \
  --server-version 4.2
```

### Step 2 — Build & Push Images

```bash
ACR_LOGIN_SERVER=$(az acr show --name $ACR_NAME --query loginServer -o tsv)
GATEWAY_URL="https://api-gateway.<your-container-app-domain>/api/v1"

# Build frontend with the real Azure gateway URL
docker build \
  --build-arg NEXT_PUBLIC_API_URL="${GATEWAY_URL}" \
  --build-arg NEXT_PUBLIC_AI_API_URL="${GATEWAY_URL}/ai" \
  --build-arg NEXT_PUBLIC_SUPABASE_URL="${SUPABASE_URL}" \
  --build-arg NEXT_PUBLIC_SUPABASE_ANON_KEY="${SUPABASE_ANON_KEY}" \
  -t $ACR_LOGIN_SERVER/frontend:latest ./frontend

# Build all backend images
for service in hotel-service search-service booking-service comment-service notification-service ai-agent-service api-gateway; do
  docker build -t $ACR_LOGIN_SERVER/$service:latest ./backend/$service
done

# Push all images
docker push $ACR_LOGIN_SERVER/frontend:latest
for service in hotel-service search-service booking-service comment-service notification-service ai-agent-service api-gateway; do
  docker push $ACR_LOGIN_SERVER/$service:latest
done
```

### Step 3 — Deploy to Azure Container Apps

```bash
# Create Container Apps environment
az containerapp env create \
  --name hotel-booking-env \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION

# Deploy API Gateway (example — repeat for each service with correct env vars)
az containerapp create \
  --name api-gateway \
  --resource-group $RESOURCE_GROUP \
  --environment hotel-booking-env \
  --image $ACR_LOGIN_SERVER/api-gateway:latest \
  --registry-server $ACR_LOGIN_SERVER \
  --target-port 8080 \
  --ingress external \
  --env-vars \
    HOTEL_SERVICE_URL=http://hotel-service \
    SEARCH_SERVICE_URL=http://search-service \
    BOOKING_SERVICE_URL=http://booking-service \
    COMMENT_SERVICE_URL=http://comment-service \
    AI_AGENT_SERVICE_URL=http://ai-agent-service
```

> **Tip:** After the first deployment, retrieve the gateway FQDN with:  
> `az containerapp show --name api-gateway --resource-group $RESOURCE_GROUP --query properties.configuration.ingress.fqdn -o tsv`  
> Then rebuild the frontend image with that URL.

---

## 📦 Project Structure

```
hotel-booking-system/
├── backend/
│   ├── api-gateway/             # Spring Cloud Gateway
│   ├── hotel-service/           # Hotel & room management + Flyway migrations
│   ├── search-service/          # Search, Redis caching, 15% discount
│   ├── booking-service/         # Reservations + Flyway migrations
│   ├── comment-service/         # Reviews & ratings (MongoDB)
│   ├── notification-service/    # Nightly capacity alerts + booking confirmations
│   └── ai-agent-service/        # OpenAI chat + live search integration
├── frontend/                    # Next.js 15 (App Router, TypeScript)
│   ├── app/                     # Pages and layouts
│   ├── components/              # Reusable UI components
│   └── lib/                     # API client, Supabase client
├── docs/
│   └── database-design.md       # ER diagram (Mermaid)
└── docker-compose.yml           # Full local stack
```

---

## 📊 Database Design

See [docs/database-design.md](docs/database-design.md) for the complete ER diagram.

Key tables: `hotels`, `rooms`, `room_availability`, `bookings`, `notification_log`  
MongoDB collection: `comments`

---

## ✅ Feature Checklist

| Feature | Status |
|---------|--------|
| Hotel CRUD (admin only) | ✅ |
| Room management (admin only) | ✅ |
| Availability management by date range | ✅ |
| Search by destination / dates / guests | ✅ |
| 15% discount for logged-in users | ✅ |
| Hotel map view | ✅ |
| Room booking + capacity decrement | ✅ |
| Paginated booking history | ✅ |
| User reviews with 5-star rating | ✅ |
| Service ratings radar chart | ✅ |
| Star distribution bar chart | ✅ |
| Paginated comment list | ✅ |
| Nightly capacity alert emails | ✅ |
| Booking confirmation notifications | ✅ |
| AI chatbot (OpenAI) with live search | ✅ |
| Redis caching for hotel details | ✅ |
| JWT authentication (Supabase) | ✅ |
| RBAC — ADMIN / USER roles | ✅ |
| Flyway DB migrations | ✅ |
| Docker Compose full stack | ✅ |

---

## 🧪 Manual Test Checklist

With the full stack running (`docker-compose up --build`):

1. **Search** — `POST /api/v1/search` with city name; compare guest vs. logged-in prices (15% discount).
2. **Hotel detail** — `GET /api/v1/search/{hotelId}`; confirm rooms and pricing appear.
3. **Booking** — Reserve a room; verify `GET /api/v1/bookings/user/{userId}` returns it.
4. **Comments** — Post a review with service sliders; open hotel detail → **Show Graphs** → star bar + radar.
5. **Admin panel** — Set `app_metadata.role=admin` in Supabase, open `/admin`, create a hotel and set availability.
6. **AI chat** — Ask "hotels in Istanbul for 2 guests"; confirm the response references real data.

---

## 📹 Demo Video

Add a ~5-minute walkthrough (unlisted YouTube or Panopto): _TBD_

---

## 📄 License

Academic project — SE 4458 Software Architecture & Design, Spring 2025/26.
