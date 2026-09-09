# Nginx config — cdn.hesap.uz

`cdn.hesap.uz` subdomeni uchun nginx vhost. URL'larni file service'ga
(`http://127.0.0.1:8004`) yo'naltiradi, file service S3'dan o'qib qaytaradi.

## Tarkibi

| Fayl | Maqsadi |
|---|---|
| `cdn.hesap.uz.conf` | Asosiy vhost — `/etc/nginx/sites-available/` |
| `nginx-cdn-cache.conf` | proxy_cache_path zone — `/etc/nginx/conf.d/` |

## O'rnatish

### 1) Cache folder
```bash
sudo mkdir -p /var/cache/nginx/cdn
sudo chown -R www-data:www-data /var/cache/nginx/cdn
```

### 2) DNS
```
cdn.hesap.uz  A  95.182.117.231   # server IP
```

### 3) SSL sertifikat
```bash
# Avval HTTP only vhost (cdn.hesap.uz.conf'dan faqat 80-port block) yoqib qo'ying
sudo cp cdn.hesap.uz.conf /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/cdn.hesap.uz.conf /etc/nginx/sites-enabled/

# Certbot
sudo certbot certonly --nginx -d cdn.hesap.uz \
  --non-interactive --agree-tos -m admin@hesap.uz

# Auto-renew tekshirish
sudo certbot renew --dry-run
```

### 4) Cache config
```bash
sudo cp nginx-cdn-cache.conf /etc/nginx/conf.d/
```

### 5) Vhost'ni faollashtirish
```bash
sudo nginx -t   # syntax check
sudo systemctl reload nginx
```

### 6) Tekshirish
```bash
curl -I https://cdn.hesap.uz/contracts/test.pdf

# Kutilgan:
# HTTP/2 404      ← fayl yo'q (test)
# x-cache-status: MISS

# Backend ishlamasa:
# HTTP/2 502 Bad Gateway

# DNS noto'g'ri:
# curl: (6) Could not resolve host
```

## Routing qoidalar

| URL | Backend | Status |
|---|---|---|
| `/images/x.jpg` | `file:8004/api/files/v1/cdn/images/x.jpg` | ✓ |
| `/stories/y.mp4` | `file:8004/api/files/v1/cdn/stories/y.mp4` | ✓ |
| `/videos/z.mp4` | `file:8004/api/files/v1/cdn/videos/z.mp4` | ✓ |
| `/contracts/abc.pdf` | `file:8004/api/files/v1/cdn/contracts/abc.pdf` | ✓ |
| `/healthz` | nginx local | 200 ok |
| `/api/...` | — | 404 (faqat folder pattern) |
| `/anything-else` | — | 404 |

Boshqa folder'lar 404 — `images/stories/videos/contracts` regex bilan
cheklangan. Yangi folder qo'shsangiz (`location ~* ^/(...)`):

```nginx
location ~* ^/(images|stories|videos|contracts|YANGI_FOLDER)/(.+)$ {
```

Va backend `CdnFolderType` enum'ga ham qo'shing.

## Cache

- **Browser cache**: 1 yil immutable (file service `Cache-Control` header)
- **Nginx cache**: 30 kun (200 OK), 1 daq (404)
- **Cache key**: scheme + host + URI
- **`X-Cache-Status`** header response'da — `HIT`, `MISS`, `BYPASS`

## Cache'ni tozalash

Bitta fayl:
```bash
sudo rm -rf /var/cache/nginx/cdn
sudo systemctl reload nginx
```

Yoki bitta URL (nginx commercial only). Open-source nginx'da to'liq tozalash.

## Logs

```
/var/log/nginx/cdn.hesap.uz.access.log
/var/log/nginx/cdn.hesap.uz.error.log
```

Real-time tail:
```bash
sudo tail -f /var/log/nginx/cdn.hesap.uz.access.log
```

## Monitoring

### Cache hit ratio

```bash
awk '{print $NF}' /var/log/nginx/cdn.hesap.uz.access.log | sort | uniq -c
```

`$upstream_cache_status` log'ga yozish uchun nginx.conf'da:
```nginx
log_format cdn_combined '$remote_addr - $remote_user [$time_local] '
                       '"$request" $status $body_bytes_sent '
                       '"$http_referer" "$http_user_agent" '
                       'cache=$upstream_cache_status';

# Vhost ichida:
access_log /var/log/nginx/cdn.hesap.uz.access.log cdn_combined;
```

## Troubleshooting

### 502 Bad Gateway
```bash
sudo docker ps | grep file
sudo netstat -lnp | grep 8004
curl -I http://127.0.0.1:8004/healthz
```

File servisni qayta yoqing:
```bash
cd /opt/hesap && docker compose restart file
```

### 404 mavjud faylga
File service log'lari:
```bash
docker logs file-service --tail 50
```

S3 bucket'da faylni tekshiring (DO Spaces admin paneli).

### SSL xatoligi
```bash
sudo certbot certificates
sudo certbot renew --dry-run
```

### Slow response
- Backend `/api/files/v1/cdn/*` endpoint vaqtini tekshiring (file service log)
- S3 endpoint'ga tezligi (DO Spaces region: fra1 — Frankfurt)
- Nginx cache hit ratio (HIT yaxshi, MISS yomon)

---

# Nginx config — open.hesap.uz

Hamkorlar uchun public API subdomeni. `open.hesap.uz/v1/...` ni api-gateway'ning
(`http://127.0.0.1:8000`) `/openapi/v1/...` yo'liga o'giradi. **Alohida servis emas** —
mavjud document servisdagi endpointlarga chiroyli manzil.

| Fayl | Maqsadi |
|---|---|
| `open.hesap.uz.conf` | Vhost — `/etc/nginx/sites-available/` |

## O'rnatish

### 1) DNS
```
open.hesap.uz  A  95.182.117.234   # prod server IP
```

### 2) SSL sertifikat
```bash
sudo cp open.hesap.uz.conf /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/open.hesap.uz.conf /etc/nginx/sites-enabled/

sudo certbot certonly --nginx -d open.hesap.uz \
  --non-interactive --agree-tos -m admin@hesap.uz
```

### 3) Faollashtirish
```bash
sudo nginx -t
sudo systemctl reload nginx
```

### 4) Tekshirish
```bash
curl -i https://open.hesap.uz/healthz            # 200 ok
curl -i https://open.hesap.uz/api-docs           # 200, JSON spec
curl -i https://open.hesap.uz/v1/me              # 401 (kalitsiz)
curl -i https://open.hesap.uz/v1/me -H "X-API-Key: hsp_..."   # 200
curl -i https://open.hesap.uz/document/v1/templates           # 404 (yopiq)
```

## Routing qoidalar

| URL | Backend | Status |
|---|---|---|
| `/v1/contracts` | `gateway:8000/openapi/v1/contracts` | ✓ |
| `/v1/templates` | `gateway:8000/openapi/v1/templates` | ✓ |
| `/api-docs` | `gateway:8000/v3/api-docs/openapi` | ✓ |
| `/healthz` | nginx local | 200 ok |
| `/main/...`, `/document/...` | — | 404 (yopiq) |

Faqat `/v1/**` va `/api-docs` ochiq — kabinet/ichki endpointlar bu subdomendan
ko'rinmaydi.

## Rate limit

IP bo'yicha 20 r/s, burst 40 (`limit_req_zone openapi_rl`). Zone `http {}` blokda
bo'lishi kerak — config faylning boshida e'lon qilingan; agar nginx `limit_req_zone`
ni `server {}` dan tashqarida talab qilsa, o'sha qatorni `/etc/nginx/conf.d/`
ichidagi alohida faylga ko'chiring.

## Troubleshooting

### 502 Bad Gateway
```bash
docker ps | grep business-gateway
curl -I http://127.0.0.1:8000/openapi/v1/me
```

### 401 to'g'ri kalit bilan
main-service ishlab turganini tekshiring — kalit `/main/v1/local/api-keys/resolve`
orqali tekshiriladi:
```bash
docker logs main-service --tail 50
```
