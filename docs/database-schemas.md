# Hesap Backend — DB Schema

> Java entity klasslari asosida (R2DBC + PostgreSQL) tuzilgan.
> Har bir microservice o'z PostgreSQL schema'sida ishlaydi.
> Konvensiyalar: UUID primary key, soft delete (`deleted` boolean), audit (`created_date`/`last_modified_date`).

## Schema: `user` (service/main :8001)

### Table: `user.user` (UserEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `username` | text | |
| `first_name` | text | |
| `last_name` | text | |
| `mid_name` | text | |
| `phone` | text | |
| `email` | text | |
| `password` | text | |
| `auth_provider` | text | |
| `pinfl` | text | |
| `passport` | text | |
| `type` | varchar | enum: USER, CLIENT, ADMIN |
| `role` | varchar | enum: OWNER, MANAGER, EMPLOYEE |
| `login_device` | text | |
| `image` | text | profile image URL |
| `bio` | text | |
| `birthday` | text | |
| `is_man` | boolean | true=male, false=female |
| `second_phone` | text | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |
| `deleted` | boolean | soft delete |
| `version` | bigint | optimistic lock |

### Table: `user.company` (CompanyEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | → user.user.id |
| `type` | varchar | enum: INDIVIDUAL, LLC, JSC, CUSTOM |
| `name` | text | |
| `custom_name` | text | |
| `tin` | text | tax ID (STIR) |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `user.user_company` (UserCompanyEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | → user.user.id |
| `company_id` | uuid | → user.company.id |
| `role` | varchar | enum: OWNER, MANAGER, EMPLOYEE |
| `status` | varchar | enum: PENDING, ACTIVE, INACTIVE |
| `is_main` | boolean | |
| `is_active` | boolean | |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `user.device` (DeviceEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `uuid` | text | device UUID |
| `phone` | text | |
| `os_version` | text | |
| `os` | text | |
| `model` | text | |
| `brand` | text | |
| `type` | text | |
| `device` | text | |
| `fcm_token` | text | Firebase Cloud Messaging token |
| `is_verify_device` | boolean | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |
| `deleted` | boolean | soft delete |

### Table: `user.session` (SessionEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | → user.user.id |
| `device_id` | uuid | → user.device.id |
| `timestamp` | timestamp | session created |

### Table: `user.permission` (PermissionEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | → user.user.id |
| `company_id` | uuid | → user.company.id |
| `permissions` | text[] | array of Permission enum |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `user.permission_request` (PermissionRequestEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_from_id` | uuid | requestor |
| `user_to_id` | uuid | recipient |
| `user_info` | boolean | |
| `passport` | boolean | |
| `payability` | boolean | |
| `contract` | boolean | |
| `partner` | boolean | |
| `status` | varchar | enum: PENDING, ACCEPTED, REJECTED |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `user.user_permission` (UserPermissionEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_from_id` | uuid | grantor |
| `user_to_id` | uuid | grantee |
| `user_info` | boolean | |
| `passport` | boolean | |
| `payability` | boolean | |
| `contract` | boolean | |
| `partner` | boolean | |
| `active` | boolean | |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `user.myid_profile` (MyIdProfileEntity)

MyID identification SDK orqali olingan to'liq passport ma'lumotlari. ~55 ta ustun (qisqalik uchun guruh): shaxsiy (firstName, lastName, pinfl, birthDate, sex, ...), passport (passData, issuedBy, issuedDate, expiryDate, docType, ...), aloqalar (phone, email), doimiy ro'yxat (pr_mfy, pr_region, pr_district, pr_address, ...), vaqtinchalik ro'yxat (tr_mfy, tr_region, tr_address, tr_date_from, tr_date_till, ...).

### Table: `user.socials` (SocialEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | → user.user.id |
| `phone_2` | text | |
| `telegram` | text | |
| `instagram` | text | |
| `facebook` | text | |

### Table: `user.one_id_user` (OneIdUserEntity)

OneID dan kelgan passport ma'lumotlari. `user_id` PRIMARY KEY (1-to-1 user bilan).
Asosiy ustunlar: pin/login, surname/name/patronymic (latin/cyrillic/en), document/docNum, issueDate/endDate, birthDate/birthPlace/birthCountry, sex, nationality, citizenship, email/phone, manzil (country/region/district/address/cadastre), photo, isActive, isLegal.

### Table: `user.admin` (AdminEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `login` | text | |
| `password` | text | BCrypt hash |
| `role` | varchar | enum: AdminRole |
| `full_name` | text | |
| `is_active` | boolean | |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |
| `version` | bigint | optimistic lock |

---

## Schema: `billing` (service/billing :8002)

### Table: `billing.balance` (BalanceEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `unique_id` | uuid | userId yoki companyId |
| `balance` | double precision | |
| `balance_type` | varchar | enum: PREPAID, POSTPAID |
| `billing_type` | varchar | enum: TARIFF, PACKAGE, BALANCE |
| `expire_date` | timestamp | |
| `tariff_id` | uuid | → billing.tariff.id |
| `deleted` | boolean | soft delete |
| `activation_date` | timestamp | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |
| `version` | bigint | optimistic lock |

### Table: `billing.transaction` (TransactionEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | |
| `company_id` | uuid | |
| `balance_id` | uuid | → billing.balance.id |
| `duration` | integer | |
| `amount` | double precision | |
| `type` | varchar | enum: DEBIT, CREDIT |
| `billing_type` | varchar | enum: TARIFF, PACKAGE, BALANCE |
| `description` | text | |
| `timestamp` | timestamp | |

### Table: `billing.payme` (PaymeTransactionEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `paycom_id` | text | Paycom transaction ID |
| `paycom_time` | bigint | |
| `uuid` | uuid | |
| `billing_type` | varchar | enum: TARIFF, PACKAGE, BALANCE |
| `create_time` | bigint | |
| `perform_time` | bigint | |
| `cancel_time` | bigint | |
| `amount` | integer | |
| `state` | integer | |
| `reason` | integer | |

### Table: `billing.click` (ClickEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `uuid` | uuid | |
| `billing_type` | varchar | |
| `click_trans_id` | text | |
| `service_id` | bigint | |
| `click_paydoc_id` | bigint | |
| `amount` | bigint | |
| `action` | integer | |
| `error` | integer | |
| `merchant_trans_id` | text | |
| `error_note` | text | |
| `sign_string` | text | |
| `sign_time` | timestamp | |

### Table: `billing.payment` (PaymentEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `company_id` | uuid | |
| `payme_merchant_id` | text | |
| `payme_secret` | text | |
| `click_service_id` | bigint | |
| `click_merchant_id` | text | |
| `click_key` | text | |
| `uzum_api_key` | text | |
| `uzum_service_id` | text | |
| `uzum_login` | text | |
| `uzum_password` | text | |
| `uzum_secret_signature` | text | |
| `rahmat_application_id` | text | |
| `rahmat_secret` | text | |
| `rahmat_store_id` | integer | |
| `inn` | text | STIR |
| `created_date` | timestamp | audit |
| `deleted` | boolean | soft delete |
| `last_modified_date` | timestamp | audit |

### Table: `billing.tariff` (TariffEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `name_uz` | text | |
| `name_ru` | text | |
| `name_en` | text | |
| `description_uz` | text | |
| `description_ru` | text | |
| `description_en` | text | |
| `price` | double precision | |
| `stars` | integer | |
| `duration` | integer | |
| `type` | varchar | enum: TariffType |
| `templates` | text | JSON template config |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `billing.packages` (PackageEntity)

`tariff` jadvali bilan bir xil struktura (name_uz/ru/en, description_uz/ru/en, price, stars, duration, type, templates, deleted, audit).

### Table: `billing.promo` (PromoCodeEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `code` | text | promo code |
| `discount_type` | varchar | enum: FIXED, PERCENTAGE |
| `discount_value` | double precision | |
| `from_price` | double precision | minimum order |
| `from_date` | date | |
| `to_date` | date | |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `billing.scoring_history` (ScoringEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | |
| `card_id` | uuid | → billing.user_cards.id |
| `created_at` | timestamp | |
| `type` | varchar | enum: UZCARD, HUMO |
| `response` | jsonb | |
| `uzcard` | jsonb | |
| `humo` | jsonb | |
| `plum_scoring_id` | integer | Plum API scoring ID |
| `status` | varchar | enum: IN_PROGRESS, COMPLETED, FAILED |

### Table: `billing.user_cards` (UserCardEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | |
| `card_id` | bigint | Plum card ID |
| `card_number` | text | |
| `expire_date` | text | |
| `type` | varchar | enum: UZCARD, HUMO |
| `status` | integer | |
| `session` | integer | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `billing.user_used_document` (UserUsedDocumentEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | |
| `balance_id` | uuid | → billing.balance.id |
| `tariff_id` | uuid | → billing.tariff.id |
| `template_distribution_type` | varchar | |
| `template_id` | uuid | |
| `count` | integer | |
| `used` | integer | |
| `expired_date` | timestamp | |
| `type` | varchar | enum: PurchaseType |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |
| `version` | bigint | optimistic lock |

### Table: `billing.integration_credential` (IntegrationCredentialEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `provider` | varchar | enum: PLUM |
| `login` | text | |
| `password` | text | plaintext (admin CRUD orqali boshqariladi) |
| `base_url` | text | nullable |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

UNIQUE INDEX: `(provider) WHERE deleted = false`

---

## Schema: `document` (service/document :8005)

### Table: `document.template` (TemplateEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `template_id` | uuid | version yoki reference |
| `company_id` | uuid | owner |
| `name_uz` | text | |
| `name_ru` | text | |
| `name_en` | text | |
| `template_data` | text | JSON content |
| `status` | varchar | enum: ACTIVE, INACTIVE, ARCHIVED |
| `template_type` | varchar | enum: TemplateType |
| `amount` | double precision | |
| `witness_count` | integer | |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `document.template_field` (TemplateFieldEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `parent_key` | text | |
| `template_id` | uuid | → document.template.id |
| `name_uz` | text | |
| `name_ru` | text | |
| `name_en` | text | |
| `key_name` | text | field key |
| `type` | varchar | enum: TEXT, NUMBER, DATE, CHECKBOX, FILE |
| `position` | varchar | enum: TemplatePosition |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `document.template_application` (TemplateApplicationEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | applicant |
| `company_id` | uuid | |
| `name` | text | |
| `comment` | text | |
| `file_url` | text | |
| `contract_type` | text | |
| `price` | double precision | |
| `status` | varchar | enum: PENDING, APPROVED, REJECTED |
| `paid_status` | boolean | |
| `created_at` | timestamp | |
| `last_modified_at` | timestamp | |

### Table: `document.document` (DocumentEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `buyer_company_id` | uuid | null for C2C |
| `seller_company_id` | uuid | null for C2C |
| `buyer_user_id` | uuid | |
| `seller_user_id` | uuid | |
| `template_id` | uuid | → document.template.id |
| `number` | text | contract number |
| `status` | varchar | enum: CREATED → SIGNED_BY_BUYER/SELLER → COMPLETED/REJECTED/CANCELLED |
| `type` | varchar | enum: C2C, B2B, B2C |
| `price` | double precision | |
| `currency` | varchar | enum: USD, UZS, RUB, EUR |
| `initial_payment` | double precision | |
| `delivery_at` | timestamp | |
| `document_json` | text | |
| `deleted` | boolean | soft delete |
| `buyer_status` | varchar | enum: PENDING, ACCEPTED, REJECTED |
| `seller_status` | varchar | enum: PENDING, ACCEPTED, REJECTED |
| `created_by_user_id` | uuid | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |
| `version` | bigint | optimistic lock |

### Table: `document.contract_value` (DocumentValueEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `position` | integer | |
| `value` | text | |
| `document_id` | uuid | → document.document.id |
| `template_id` | uuid | → document.template.id |
| `template_field_id` | uuid | → document.template_field.id |
| `key_name` | text | |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `document.document_signature` (DocumentSignatureEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `document_id` | uuid | → document.document.id |
| `user_id` | uuid | signer |
| `signature` | text | |
| `action_type` | varchar | enum: SIGN, CANCEL |
| `created_date` | timestamp | |

### Table: `document.contract_witness` (DocumentWitnessEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `document_id` | uuid | → document.document.id |
| `witness_id` | uuid | → user.user.id |
| `status` | varchar | enum: PENDING, ACCEPTED, REJECTED |
| `created_date` | timestamp | |
| `last_modified_date` | timestamp | |

### Table: `document.contract_product` (DocumentProductEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `document_id` | uuid | → document.document.id |
| `company_id` | uuid | |
| `product_id` | uuid | → document.product.id |
| `amount` | double precision | quantity |
| `price` | double precision | unit price |
| `name_uz` | text | cloned from product |
| `name_ru` | text | |
| `name_en` | text | |
| `description` | text | |
| `mxic` | text | |
| `package_code` | text | |
| `product_unit` | text | |
| `package_id` | uuid | |
| `image` | text | |
| `barcode` | text | |
| `received_date` | timestamp | |
| `status` | varchar | enum: PENDING, DELIVERED |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |
| `version` | bigint | optimistic lock |

### Table: `document.product` (ProductEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `name_uz` | text | |
| `name_ru` | text | |
| `name_en` | text | |
| `description` | text | |
| `company_id` | uuid | |
| `mxic` | text | |
| `package_code` | text | |
| `product_unit` | text | |
| `package_id` | uuid | |
| `price` | double precision | |
| `image` | text | |
| `barcode` | text | |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |
| `version` | bigint | optimistic lock |

### Table: `document.contract_payment` (PaymentScheduleEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `buyer_company_id` | uuid | null for C2C |
| `seller_company_id` | uuid | null for C2C |
| `buyer_id` | uuid | |
| `seller_id` | uuid | |
| `document_id` | uuid | → document.document.id |
| `status` | varchar | enum: PENDING, PAID, CANCELLED, OVERDUE |
| `payment_completed_at` | timestamp | |
| `amount` | double precision | |
| `paid_amount` | double precision | |
| `currency` | varchar | enum: USD, UZS, RUB, EUR |
| `payment_date` | timestamp | due date |
| `previous_payment_date` | timestamp | old due date if delayed |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `document.paid_schedule` (PaidScheduleEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `buyer_company_id` | uuid | null for C2C |
| `seller_company_id` | uuid | null for C2C |
| `buyer_id` | uuid | |
| `seller_id` | uuid | |
| `document_id` | uuid | → document.document.id |
| `payment_schedule_id` | uuid | → document.contract_payment.id |
| `status` | varchar | |
| `amount` | double precision | actual paid amount |
| `currency` | varchar | enum: USD, UZS, RUB, EUR |
| `payment_date` | timestamp | |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `document.payment_schedule_request` (PaymentScheduleRequestEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `buyer_company_id` | uuid | null for C2C |
| `seller_company_id` | uuid | null for C2C |
| `user_id` | uuid | requester |
| `document_id` | uuid | → document.document.id |
| `status` | varchar | enum: PENDING, APPROVED, REJECTED |
| `type` | varchar | enum: PAYMENT, DELAY |
| `payment_schedule_id` | uuid | → document.contract_payment.id |
| `amount` | double precision | |
| `currency` | varchar | enum: USD, UZS, RUB, EUR |
| `payment_date` | timestamp | requested new date |
| `deleted` | boolean | soft delete |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `document.notice` (NoticeEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `document_id` | uuid | → document.document.id |
| `buyer_id` | uuid | |
| `seller_id` | uuid | |
| `name_uz` | text | |
| `name_ru` | text | |
| `name_en` | text | |
| `number` | integer | |
| `status` | varchar | enum: NoticeStatus |
| `template_json` | text | cloned template |
| `deleted` | boolean | soft delete |
| `created_by` | uuid | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `document.report` (ReportEntity)

`notice` jadvali bilan bir xil struktura (id, document_id, seller_id, buyer_id, name_uz/ru/en, number, status, template_json, deleted, created_by, audit).

---

## Schema: `file` (service/file :8004)

### Table: `file.cdn_data` (CdnDataEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | uploader |
| `content_length` | bigint | file size in bytes |
| `ext` | text | file extension |
| `folder` | text | storage folder |
| `content_url` | text | CDN URL |
| `created_date` | timestamp | |

---

## Schema: `log` (service/log :8003)

### Table: `log.user_log` (UserLogEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | |
| `reason` | varchar | enum: UserLogReason |
| `first_name` | text | |
| `last_name` | text | |
| `old_version` | jsonb | previous state |
| `new_version` | jsonb | new state |
| `different_fields` | text[] | changed field names |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `log.one_id_log` (OneIdLogEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | |
| `first_name` | text | |
| `last_name` | text | |
| `company_id` | uuid | |
| `request_time` | timestamp | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `log.eskiz_log` (EskizLogEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `phone` | text | |
| `content` | text | SMS content |
| `is_failed` | boolean | |
| `error` | text | |
| `timestamp` | timestamp | |
| `created_date` | timestamp | |

### Table: `log.myid_log` (MyIdLogEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `user_id` | uuid | |
| `company_id` | uuid | |
| `status` | text | |
| `error_message` | text | |
| `request` | text | JSON |
| `response` | text | JSON |
| `request_time` | timestamp | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `log.template_application_log` (TemplateApplicationLogEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `application_id` | uuid | → document.template_application.id |
| `company_id` | uuid | |
| `old_status` | varchar | enum: TemplateApplicationStatus |
| `new_status` | varchar | enum: TemplateApplicationStatus |
| `updated_by` | uuid | |
| `first_name` | text | |
| `last_name` | text | |
| `timestamp` | timestamp | |

---

## Schema: `notification` (service/notification :8006)

### Table: `notification.notification` (NotificationEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `data_id` | uuid | related entity ID |
| `type` | varchar | enum: NotificationType |
| `user_id` | uuid | recipient |
| `title_uz` | text | |
| `title_ru` | text | |
| `title_en` | text | |
| `body_uz` | text | |
| `body_ru` | text | |
| `body_en` | text | |
| `image` | text | |
| `is_viewed` | boolean | |
| `deleted` | boolean | soft delete |
| `created_user_id` | uuid | |
| `created_at` | timestamp | |
| `updated_at` | timestamp | |

### Table: `notification.blog` (BlogEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `title_uz` | text | |
| `title_ru` | text | |
| `title_en` | text | |
| `body_uz` | text | |
| `body_ru` | text | |
| `body_en` | text | |
| `image` | text | |
| `date` | timestamp | publication date |
| `is_home` | boolean | show on home page |
| `is_send` | boolean | sent as notification |
| `is_deleted` | boolean | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `notification.faq` (FaqEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `title_uz` | text | question |
| `title_ru` | text | |
| `title_en` | text | |
| `answer_uz` | text | |
| `answer_ru` | text | |
| `answer_en` | text | |
| `is_deleted` | boolean | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `notification.legal_document` (LegalDocumentEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `type` | varchar | enum: LegalDocumentType |
| `text_uz` | text | |
| `text_ru` | text | |
| `text_en` | text | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `notification.story` (StoryEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `title_uz` | text | |
| `title_ru` | text | |
| `title_en` | text | |
| `avatar` | text | |
| `items` | text | JSON array |
| `is_deleted` | boolean | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

### Table: `notification.story_view` (StoryViewEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `story_id` | uuid | → notification.story.id |
| `user_id` | uuid | |
| `viewed_at` | timestamp | |

### Table: `notification.sms_providers` (SmsProviderSettingEntity)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid | PRIMARY KEY |
| `eskiz_email` | text | Eskiz SMS email |
| `eskiz_secret` | text | |
| `token` | text | |
| `chat_id` | text | Telegram chat ID |
| `username` | text | |
| `created_date` | timestamp | audit |
| `last_modified_date` | timestamp | audit |

---

## Schema: `integration` (service/integration :8007)

**Status:** Faqat scaffold — hozircha entity yo'q.
