# C2C Document Flow

## Shartnoma hayot sikli

```
            ┌──────────────┐
            │   CREATE     │ (buyer yoki seller)
            └──────┬───────┘
                   │
          ┌────────▼────────┐
          │     DRAFT       │
          └────────┬────────┘
                   │  (birinchi tomon imzolaganda)
      ┌────────────┼────────────┐
      ▼                         ▼
┌─────────────┐          ┌─────────────┐
│ SIGNED_BY   │          │ SIGNED_BY   │
│ BUYER       │          │ SELLER      │
└──────┬──────┘          └──────┬──────┘
       │  (ikkinchi tomon imzolaganda)  │
       └────────────┬───────────────────┘
                    ▼
            ┌──────────────┐
            │  COMPLETED   │
            └──────────────┘

  Istalgan vaqtda:
  ┌──────────────┐     ┌──────────────┐
  │  REJECTED    │     │  CANCELLED   │
  │ (boshqa tomon)│     │ (yaratuvchi) │
  └──────────────┘     └──────────────┘
```

## Creation Flow
1. `POST /api/document/v1/c2c` — hujjat yaratish
2. Parallel saqlash: document + values + witnesses + payments
3. Notification: boshqa tomonga (buyer→seller yoki seller→buyer)
4. Notification: har bir guvohga (witness invite)

## Witness Flow
1. Guvoh notification oladi
2. OTP yuboriladi (`/send-verification-sms`)
3. Guvoh accept (`/witness/accept`) yoki reject (`/witness/reject`)
4. Creator ga notification yuboriladi

## Payment Flow
1. Document yaratilganda `PaymentSchedule` lar ham yaratiladi (PENDING)
2. Buyer to'lov qiladi → Seller ga notification
3. Seller tasdiqlaydi/rad etadi → Buyer ga notification
4. Kechiktirish so'rash mumkin (delay request)
