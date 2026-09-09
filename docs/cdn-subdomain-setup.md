# cdn.hesap.uz subdomen sozlash

`cdn.hesap.uz` — public fayl CDN. Nginx reverse-proxy file service'ga
(`file:8004`) yo'naltiradi, file service S3 (DigitalOcean Spaces)'dan
fayl o'qib qaytaradi.

## Architecture

```
Browser/Mobile
   ↓
https://cdn.hesap.uz/contracts/uuid.pdf
   ↓
Nginx (server_name: cdn.hesap.uz)
   ↓ URL rewrite
   /contracts/uuid.pdf → /api/files/v1/cdn/contracts/uuid.pdf
   ↓ proxy_pass
file:8004 (file service)
   ↓ AWS SDK
DigitalOcean Spaces (cdn-hesap.fra1.digitaloceanspaces.com)
```

## Nima uchun file service orqali, S3 emas?

- **Markaziy boshqaruv** — kelajakda lokal disk, boshqa S3, yoki Cloudflare R2'ga
  o'tish — faqat file service kodi o'zgaradi
- **Access control kelajakda qo'shish oson** — hozir public, lekin endpoint
  authorized only qilish kerak bo'lsa, file service'da bir qator kod
- **Cache control** — file service Cache-Control header qaytaradi (1 yil immutable)
- **404 handling** — to'g'ri xato sahifa

## 1. DNS

DNS provider'da (Cloudflare/Route53):

```
Type: A
Name: cdn
Value: 95.182.117.231   # server IP
TTL: 300
```

CNAME'dan A record afzal — file service kelajakda CDN bo'lmasa ham ishlaydi.

## 2. SSL sertifikat (Let's Encrypt)

```bash
sudo certbot certonly --nginx -d cdn.hesap.uz \
  --non-interactive --agree-tos -m admin@hesap.uz
```

## 3. Nginx vhost

`/etc/nginx/sites-available/cdn.hesap.uz`:

```nginx
server {
    listen 80;
    server_name cdn.hesap.uz;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name cdn.hesap.uz;

    ssl_certificate /etc/letsencrypt/live/cdn.hesap.uz/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/cdn.hesap.uz/privkey.pem;

    # CORS (public CDN)
    add_header Access-Control-Allow-Origin "*" always;
    add_header Access-Control-Allow-Methods "GET, HEAD, OPTIONS" always;

    client_max_body_size 100M;

    # Faqat ma'lum folder'lar — xavfsizlik uchun
    location ~* ^/(images|stories|videos|contracts)/(.+)$ {
        # cdn.hesap.uz/contracts/uuid.pdf
        # → file:8004/api/files/v1/cdn/contracts/uuid.pdf
        rewrite ^/(.+)/(.+)$ /api/files/v1/cdn/$1/$2 break;

        proxy_pass http://127.0.0.1:8004;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto https;

        proxy_connect_timeout 10s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # Nginx cache (ixtiyoriy — file service ham Cache-Control qaytaradi)
        proxy_cache_valid 200 30d;
        proxy_cache_valid 404 1m;
    }

    # Boshqa hammasi — 404
    location / {
        return 404;
    }
}
```

Enable:

```bash
sudo ln -s /etc/nginx/sites-available/cdn.hesap.uz /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 4. Docker port mapping

Hozirgi `docker-compose.yml`'da file servis `8004:8004` ochiq —
nginx `http://127.0.0.1:8004`'ga ulanadi.

## 5. Tekshirish

```bash
curl -I https://cdn.hesap.uz/contracts/some-uuid.pdf

# Kutilayotgan:
# HTTP/2 200
# content-type: application/pdf
# cache-control: max-age=31536000, public, immutable
```

## 6. Backend konfiguratsiya

Default `CdnProperties.publicBaseUrl = https://cdn.hesap.uz`.

docker-compose.yml:

```yaml
file:
  environment:
    CDN_PUBLIC_BASE_URL: https://cdn.hesap.uz
```

Fallback (cdn.hesap.uz tayyor bo'lmasa):
```yaml
CDN_PUBLIC_BASE_URL: ""   # → legacy DO Spaces URL
```

## 7. Mavjud URL'larni migrate qilish (ixtiyoriy)

```sql
UPDATE file.cdn_data
SET content_url = REPLACE(
  content_url,
  'cdn-hesap.fra1.digitaloceanspaces.com',
  'cdn.hesap.uz'
);

-- Document service PDF cache (MR #127 dan keyin)
UPDATE document.document
SET pdf_url = REPLACE(
  pdf_url,
  'cdn-hesap.fra1.digitaloceanspaces.com',
  'cdn.hesap.uz'
)
WHERE pdf_url IS NOT NULL;
```
