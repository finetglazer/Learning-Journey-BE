# 1. When deploy, let's deploy no cache because of changing old code.
```bash
docker-compose up --build --force-recreate --remove-orphans -d 
```

# 2. If only a service


# needs to be updated, you can specify the service name.
```bash
docker-compose up --build --force-recreate --remove-orphans -d api-gateway-service order-service payment-service saga-orchestrator-service
```

```bash
docker-compose up --build --force-recreate --remove-orphans -d postgres-user-service postgres-scheduling-service postgres-saga-service
```

