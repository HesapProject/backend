# Service Integration Map

## Overview
```
┌─────────────┐
│  API Gateway │  :8000
│  (routing)   │
└──────┬───────┘
       │
       ├──→ User Service        :8001  ←─┐
       ├──→ Billing Service     :8002    │ WebClient
       ├──→ Log Service         :8003    │
       ├──→ File Service        :8004    │
       ├──→ Document Service    :8005  ──┘
       └──→ Notification Service :8006
```

## Sync Dependencies (WebClient)

| From | To | Purpose |
|------|----|---------|
| Document → User | User info olish (buyer/seller/witness) |
| Document → Billing | Tarif tekshirish, usage decrement |
| Billing → User | Kompaniya/user tekshirish |
| Billing → Document | Hujjat tekshirish |
| Log → User | User info olish |
| File → User | Auth tekshirish |

## Async Dependencies (RabbitMQ)

| From | To | Message |
|------|----|---------|
| User → Notification | Permission request/accept/reject |
| Document → Notification | Witness invite, doc created/signed/completed/rejected |
| Document → Notification | Payment paid/approved/rejected/delay |
| Document → Log | Hujjat operatsiyalari audit |

## Shared Module Dependencies
```
common ←── user, billing, document, notification, file, log
jms    ←── user, document, notification, log
```
