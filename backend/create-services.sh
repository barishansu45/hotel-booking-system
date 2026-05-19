#!/bin/bash

# Create all Spring Boot services quickly

SERVICES=("search-service:8082" "booking-service:8083" "comment-service:8084" "notification-service:8085" "ai-agent-service:8086" "api-gateway:8080")

for service_info in "${SERVICES[@]}"; do
    IFS=':' read -r service port <<< "$service_info"
    echo "Creating $service on port $port..."
    
    cd "/Users/bhansuu/Desktop/hotel-booking-system/backend/$service"
    
    # Create directory structure
    mkdir -p src/main/java/com/hotelbooking/$(echo $service | sed 's/-//g')/{controller,service,repository,entity,dto,config}
    mkdir -p src/main/resources
    mkdir -p src/test/java/com/hotelbooking/$(echo $service | sed 's/-//g')
    
    echo "$service structure created"
done

echo "All services structure created!"
