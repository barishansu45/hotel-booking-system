# Azure Deployment Guide

## Prerequisites

- Azure account
- Azure CLI installed
- Docker installed

## Services to Deploy

### 1. Azure Database for PostgreSQL
```bash
az postgres flexible-server create \
  --resource-group hotel-booking-rg \
  --name hotel-booking-db \
  --location eastus \
  --admin-user hoteldbadmin \
  --admin-password <your-password> \
  --sku-name Standard_B1ms \
  --version 16
```

### 2. Azure Cache for Redis
```bash
az redis create \
  --resource-group hotel-booking-rg \
  --name hotel-booking-cache \
  --location eastus \
  --sku Basic \
  --vm-size c0
```

### 3. Azure Cosmos DB (MongoDB API)
```bash
az cosmosdb create \
  --resource-group hotel-booking-rg \
  --name hotel-booking-cosmos \
  --kind MongoDB \
  --locations regionName=eastus
```

### 4. Azure Service Bus
```bash
az servicebus namespace create \
  --resource-group hotel-booking-rg \
  --name hotel-booking-servicebus \
  --location eastus \
  --sku Standard

az servicebus queue create \
  --resource-group hotel-booking-rg \
  --namespace-name hotel-booking-servicebus \
  --name new-reservations
```

### 5. Azure App Services (Backend)
```bash
# Create App Service Plan
az appservice plan create \
  --name hotel-booking-plan \
  --resource-group hotel-booking-rg \
  --location eastus \
  --sku B1 \
  --is-linux

# Deploy each service
az webapp create \
  --resource-group hotel-booking-rg \
  --plan hotel-booking-plan \
  --name hotel-service-app \
  --deployment-container-image-name <your-docker-image>
```

### 6. Azure Static Web Apps (Frontend)
```bash
az staticwebapp create \
  --name hotel-booking-frontend \
  --resource-group hotel-booking-rg \
  --location eastus \
  --source https://github.com/<your-repo> \
  --branch main \
  --app-location "/frontend" \
  --output-location ".next"
```

## Environment Variables

Set these in Azure App Service Configuration:

### Hotel Service
- DATABASE_URL
- DATABASE_USERNAME
- DATABASE_PASSWORD

### Search Service  
- REDIS_HOST
- REDIS_PASSWORD

### Booking Service
- AZURE_SERVICEBUS_CONNECTION_STRING

### Comment Service
- COSMOS_DB_URI

### Notification Service
- MAIL_HOST
- MAIL_USERNAME
- MAIL_PASSWORD

## Deployment Steps

1. Build Docker images
2. Push to Azure Container Registry
3. Deploy to Azure App Services
4. Configure environment variables
5. Setup custom domains
6. Enable SSL certificates

## Cost Estimate

- PostgreSQL: ~$25/month
- Redis: ~$16/month
- Cosmos DB: ~$24/month
- Service Bus: ~$10/month
- App Services (5 services): ~$50/month
- Static Web App: Free tier

**Total: ~$125/month**
