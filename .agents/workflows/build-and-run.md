---
description: Loyihani build qilish va ishga tushirish
---

# Build va Run

// turbo-all

## 1. Gradle build
```bash
cd /home/elmurod/Desktop/mimsoft/hesap-business-backend
./gradlew clean build -x test
```

## 2. Faqat bitta service build
```bash
./gradlew :service:document:build -x test
```

## 3. Docker Compose bilan ishga tushirish
```bash
docker compose up -d
```

## 4. Faqat bitta service restart
```bash
docker compose restart document
```

## 5. Loglarni ko'rish
```bash
docker compose logs -f document
```

## Service Portlari
| Service | Port |
|---------|------|
| Gateway | 8000 |
| User | 8001 |
| Billing | 8002 |
| Log | 8003 |
| File | 8004 |
| Document | 8005 |
| Notification | 8006 |
| RabbitMQ UI | 25672 |
