# 🔴 Güvenlik Açıkları ve 🔵 Geliştirme Önerileri

Bu dosya, microservice-auth projesi için tespit edilen güvenlik açıklarını ve geliştirme önerilerini içermektedir. Her madde ayrı bir GitHub Issue olarak açılabilir.

---

## 🔴 KRİTİK GÜVENLİK AÇIKLARI

### 1. [SECURITY] Hardcoded JWT Secret Key
**Öncelik:** 🔴 Kritik  
**Etiketler:** `security`, `critical`, `configuration`

**Açıklama:**
`docker-compose.yml` ve `application.yml` dosyalarında JWT secret key hardcoded olarak yazılmış:
```
JWT_SECRET=mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong2024
```

**Risk:**
- Kaynak kod public olduğu için secret key herkes tarafından görülebilir
- Saldırganlar bu key ile sahte JWT token üretebilir

**Çözüm Önerisi:**
- Secret key'i environment variable olarak dışarıdan alın
- Kubernetes Secrets veya HashiCorp Vault kullanın
- Her environment için farklı secret key kullanın

---

### 2. [SECURITY] Hardcoded Database Credentials
**Öncelik:** 🔴 Kritik  
**Etiketler:** `security`, `critical`, `configuration`

**Açıklama:**
Database kullanıcı adı ve şifresi hardcoded:
```yaml
POSTGRES_USER: postgres
POSTGRES_PASSWORD: postgres
```

**Risk:**
- Production ortamında varsayılan credentials kullanılması ciddi güvenlik riski
- Veritabanına yetkisiz erişim

**Çözüm Önerisi:**
- Environment variable kullanın: `${DB_PASSWORD:?error}`
- Secrets management tool kullanın
- Production için güçlü şifre politikası uygulayın

---

### 3. [SECURITY] RabbitMQ Default Credentials
**Öncelik:** 🔴 Kritik  
**Etiketler:** `security`, `critical`, `rabbitmq`

**Açıklama:**
RabbitMQ varsayılan credentials ile çalışıyor:
```yaml
RABBITMQ_DEFAULT_USER: guest
RABBITMQ_DEFAULT_PASS: guest
```

**Risk:**
- Message queue'ya yetkisiz erişim
- Mesaj dinleme/değiştirme

**Çözüm Önerisi:**
- Güçlü kullanıcı adı ve şifre kullanın
- Environment variable ile yapılandırın

---

### 4. [SECURITY] Grafana Weak Admin Password
**Öncelik:** 🟠 Yüksek  
**Etiketler:** `security`, `monitoring`

**Açıklama:**
Grafana admin şifresi zayıf:
```yaml
GF_SECURITY_ADMIN_PASSWORD=admin123
```

**Çözüm Önerisi:**
- Güçlü şifre kullanın
- İlk giriş sonrası şifre değiştirmeyi zorunlu kılın

---

### 5. [SECURITY] Role Assignment Vulnerability
**Öncelik:** 🔴 Kritik  
**Etiketler:** `security`, `critical`, `auth`

**Açıklama:**
`RegisterRequest` DTO'sunda kullanıcı kendi rolünü belirleyebilir:
```java
@Schema(description = "User role (defaults to USER if not specified)")
private Role role;
```

**Risk:**
- Herhangi bir kullanıcı ADMIN rolüyle kayıt olabilir
- Privilege escalation saldırısı

**Çözüm Önerisi:**
- Register endpoint'inden role field'ını kaldırın
- Admin kullanıcı sadece mevcut admin tarafından oluşturulabilsin
- Ayrı bir admin registration endpoint'i (korumalı) oluşturun

---

### 6. [SECURITY] Missing Rate Limiting
**Öncelik:** 🟠 Yüksek  
**Etiketler:** `security`, `api-gateway`, `enhancement`

**Açıklama:**
API Gateway'de rate limiting mekanizması bulunmuyor.

**Risk:**
- Brute force saldırıları (login denemesi)
- DDoS saldırıları
- Resource exhaustion

**Çözüm Önerisi:**
- Spring Cloud Gateway RateLimiter ekleyin
- Redis tabanlı rate limiting implementasyonu
- IP bazlı ve kullanıcı bazlı limit

---

### 7. [SECURITY] Missing Brute Force Protection
**Öncelik:** 🟠 Yüksek  
**Etiketler:** `security`, `auth`, `enhancement`

**Açıklama:**
Login endpoint'inde brute force koruması yok.

**Risk:**
- Sınırsız login denemesi yapılabilir
- Şifre tahmin saldırıları

**Çözüm Önerisi:**
- Account lockout mekanizması (5 başarısız deneme sonrası)
- Progressive delay (artan bekleme süresi)
- CAPTCHA entegrasyonu
- IP bazlı geçici engelleme

---

### 8. [SECURITY] No Email Verification Token Expiry
**Öncelik:** 🟡 Orta  
**Etiketler:** `security`, `auth`

**Açıklama:**
Email doğrulama token'larının süresi dolmuyor:
```java
user.setEmailVerified(true);
user.setEmailVerificationToken(null);
// Token expiry yok!
```

**Risk:**
- Eski token'lar sonsuza kadar kullanılabilir
- Token leak durumunda hesap ele geçirilebilir

**Çözüm Önerisi:**
- `emailVerificationTokenExpiry` field'ı ekleyin
- Token süresi 24-48 saat olsun
- Süresi dolan token'lar için yeniden gönderme mekanizması

---

### 9. [SECURITY] Password Policy Too Weak
**Öncelik:** 🟡 Orta  
**Etiketler:** `security`, `auth`, `validation`

**Açıklama:**
Sadece minimum 8 karakter zorunluluğu var:
```java
@Size(min = 8, message = "Password must be at least 8 characters")
private String password;
```

**Çözüm Önerisi:**
- Büyük/küçük harf zorunluluğu
- En az bir rakam
- En az bir özel karakter
- Yaygın şifreleri engelleyin (password123 vb.)
- Custom `@StrongPassword` annotation

---

### 10. [SECURITY] Sensitive Data in Logs
**Öncelik:** 🟡 Orta  
**Etiketler:** `security`, `logging`

**Açıklama:**
Email adresleri loglarda görünüyor:
```java
log.info("User registered: {}", savedUser.getEmail());
log.info("Password reset requested for: {}", user.getEmail());
```

**Risk:**
- Log aggregation sistemlerinde PII (Personal Identifiable Information) sızıntısı
- GDPR/KVKK uyumsuzluğu

**Çözüm Önerisi:**
- Email adreslerini maskeleyin: `j***@example.com`
- Sensitive data logging policy uygulayın
- Structured logging ile sensitive field'ları filtreleyin

---

### 11. [SECURITY] CORS Configuration Too Permissive
**Öncelik:** 🟡 Orta  
**Etiketler:** `security`, `api-gateway`

**Açıklama:**
```yaml
allowedHeaders: "*"
```

**Çözüm Önerisi:**
- Sadece gerekli header'lara izin verin
- Production için origin'leri whitelist yapın

---

### 12. [SECURITY] Missing Security Headers
**Öncelik:** 🟡 Orta  
**Etiketler:** `security`, `api-gateway`, `enhancement`

**Açıklama:**
Response'larda güvenlik header'ları eksik.

**Çözüm Önerisi:**
Şu header'ları ekleyin:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security`
- `Content-Security-Policy`

---

### 13. [SECURITY] JWT Token Stored Only in Response
**Öncelik:** 🟡 Orta  
**Etiketler:** `security`, `auth`, `frontend`

**Açıklama:**
Token sadece response body'de dönüyor, frontend'de localStorage'da saklanması XSS riski oluşturur.

**Çözüm Önerisi:**
- HttpOnly cookie option ekleyin
- Refresh token'ı HttpOnly cookie'de saklayın
- Access token'ı memory'de tutun

---

## 🔵 FONKSİYONEL GELİŞTİRMELER

### 14. [FEATURE] Unit Test Eksikliği
**Öncelik:** 🔴 Kritik  
**Etiketler:** `testing`, `quality`

**Açıklama:**
Projede hiç test dosyası bulunmuyor (`src/test` klasörleri boş).

**Yapılması Gerekenler:**
- AuthService için unit testler
- JwtService için unit testler
- Controller integration testleri
- Repository testleri
- Minimum %80 code coverage hedefi

---

### 15. [FEATURE] Integration Test Eksikliği
**Öncelik:** 🟠 Yüksek  
**Etiketler:** `testing`, `quality`

**Açıklama:**
End-to-end ve integration testler yok.

**Yapılması Gerekenler:**
- Testcontainers ile PostgreSQL, RabbitMQ testi
- API endpoint testleri
- Authentication flow testleri

---

### 16. [FEATURE] Account Lockout Mechanism
**Öncelik:** 🟠 Yüksek  
**Etiketler:** `security`, `enhancement`

**Açıklama:**
Başarısız login denemelerini takip eden ve hesabı kilitleyen mekanizma yok.

**Yapılması Gerekenler:**
- `failedLoginAttempts` field'ı User entity'sine ekleyin
- `lockedUntil` timestamp field'ı ekleyin
- 5 başarısız denemeden sonra 30 dk kilit
- Admin tarafından kilit açma özelliği

---

### 17. [FEATURE] Refresh Token Rotation
**Öncelik:** 🟠 Yüksek  
**Etiketler:** `security`, `enhancement`

**Açıklama:**
Refresh token kullanıldığında token rotation uygulanmalı.

**Mevcut Durum:** Eski token revoke ediliyor ama yeni token ile önceki token arasında bağlantı yok.

**Öneriler:**
- Token ailesi (family) kavramı ekleyin
- Aynı aileden token reuse tespit edilirse tüm aileyi revoke edin
- Token theft detection

---

### 18. [FEATURE] 2FA (Two-Factor Authentication)
**Öncelik:** 🟡 Orta  
**Etiketler:** `security`, `enhancement`, `feature`

**Açıklama:**
İki faktörlü kimlik doğrulama desteği ekleyin.

**Yapılması Gerekenler:**
- TOTP (Google Authenticator) desteği
- SMS/Email OTP option
- 2FA enable/disable endpoint'leri
- Backup kodları

---

### 19. [FEATURE] OAuth2/Social Login
**Öncelik:** 🟡 Orta  
**Etiketler:** `enhancement`, `feature`

**Açıklama:**
Social login entegrasyonu ekleyin.

**Yapılması Gerekenler:**
- Google OAuth2
- GitHub OAuth2
- Facebook OAuth2
- LinkedIn OAuth2

---

### 20. [FEATURE] Session Management
**Öncelik:** 🟡 Orta  
**Etiketler:** `security`, `enhancement`

**Açıklama:**
Kullanıcıların aktif session'larını görmesi ve yönetmesi.

**Yapılması Gerekenler:**
- Aktif session listesi endpoint'i
- Tek bir session'ı sonlandırma
- Tüm session'ları sonlandırma
- Session metadata (device, IP, location)

---

### 21. [FEATURE] Password History
**Öncelik:** 🟢 Düşük  
**Etiketler:** `security`, `enhancement`

**Açıklama:**
Kullanıcının son N şifresini tekrar kullanmasını engelleyin.

**Yapılması Gerekenler:**
- PasswordHistory entity
- Son 5 şifreyi sakla
- Şifre değişikliğinde kontrol et

---

### 22. [FEATURE] Audit Logging
**Öncelik:** 🟠 Yüksek  
**Etiketler:** `security`, `compliance`, `enhancement`

**Açıklama:**
Güvenlik olaylarını loglamak için audit trail.

**Yapılması Gerekenler:**
- AuditLog entity (timestamp, action, userId, IP, userAgent)
- Login/logout logları
- Password reset logları
- Failed login attempt logları
- Admin action logları

---

### 23. [FEATURE] Email Template Localization
**Öncelik:** 🟢 Düşük  
**Etiketler:** `enhancement`, `i18n`

**Açıklama:**
Email template'leri sadece tek dilde.

**Yapılması Gerekenler:**
- Kullanıcı dil tercihi (User entity'de locale field)
- Çoklu dil desteği template'lerde
- i18n message bundles

---

### 24. [FEATURE] Health Check Improvements
**Öncelik:** 🟡 Orta  
**Etiketler:** `devops`, `monitoring`

**Açıklama:**
Health endpoint'leri basit durumda.

**Yapılması Gerekenler:**
- Custom health indicators
- Database connection health
- RabbitMQ connection health
- External service health (Eureka)
- Readiness vs Liveness ayrımı

---

### 25. [FEATURE] API Versioning Strategy
**Öncelik:** 🟢 Düşük  
**Etiketler:** `api`, `enhancement`

**Açıklama:**
API versiyonlama `/api/v1/` şeklinde yapılmış, ancak versiyon geçiş stratejisi belgelenmemiş.

**Yapılması Gerekenler:**
- Versiyon deprecation policy
- Header-based versioning alternatifi
- Backward compatibility guidelines

---

### 26. [FEATURE] Request/Response Logging Filter
**Öncelik:** 🟡 Orta  
**Etiketler:** `logging`, `debugging`

**Açıklama:**
Request/response logları yok.

**Yapılması Gerekenler:**
- Request logging filter (exclude sensitive data)
- Correlation ID (trace ID) ekleme
- Response time logging

---

### 27. [FEATURE] Circuit Breaker Implementation
**Öncelik:** 🟡 Orta  
**Etiketler:** `resilience`, `enhancement`

**Açıklama:**
Servisler arası iletişimde circuit breaker yok.

**Yapılması Gerekenler:**
- Resilience4j entegrasyonu
- Fallback mekanizmaları
- Timeout configuration

---

### 28. [FEATURE] Centralized Configuration
**Öncelik:** 🟡 Orta  
**Etiketler:** `devops`, `enhancement`

**Açıklama:**
Spring Cloud Config Server kullanılmıyor.

**Yapılması Gerekenler:**
- Config Server ekleyin
- Git-based configuration
- Environment-specific configs
- Encryption for sensitive properties

---

### 29. [FEATURE] Dead Letter Queue Handling
**Öncelik:** 🟡 Orta  
**Etiketler:** `rabbitmq`, `reliability`

**Açıklama:**
RabbitMQ'da DLQ yapılandırması yok.

**Yapılması Gerekenler:**
- Dead letter exchange
- Failed message retry policy
- DLQ monitoring

---

### 30. [FEATURE] Prometheus Metrics
**Öncelik:** 🟡 Orta  
**Etiketler:** `monitoring`, `enhancement`

**Açıklama:**
Prometheus metrikleri eksik.

**Yapılması Gerekenler:**
- Micrometer/Prometheus entegrasyonu
- Custom business metrics
- JVM metrics
- HTTP request metrics
- Grafana dashboard'ları

---

### 31. [FEATURE] User Profile Management
**Öncelik:** 🟢 Düşük  
**Etiketler:** `feature`, `enhancement`

**Açıklama:**
Kullanıcı profil yönetimi endpoint'leri eksik.

**Yapılması Gerekenler:**
- GET /api/v1/users/me (current user info)
- PUT /api/v1/users/me (update profile)
- PUT /api/v1/users/me/password (change password)
- DELETE /api/v1/users/me (account deletion)

---

### 32. [FEATURE] Admin User Management
**Öncelik:** 🟡 Orta  
**Etiketler:** `feature`, `admin`

**Açıklama:**
Admin kullanıcı yönetim paneli endpoint'leri yok.

**Yapılması Gerekenler:**
- GET /api/v1/admin/users (list users)
- PUT /api/v1/admin/users/{id}/activate
- PUT /api/v1/admin/users/{id}/deactivate
- PUT /api/v1/admin/users/{id}/role
- User search/filter

---

### 33. [FEATURE] Token Blacklist/Revocation
**Öncelik:** 🟠 Yüksek  
**Etiketler:** `security`, `enhancement`

**Açıklama:**
Access token logout sonrası hala geçerli kalıyor.

**Yapılması Gerekenler:**
- Redis tabanlı token blacklist
- Logout'ta access token'ı blacklist'e ekle
- Token validation'da blacklist kontrolü

---

### 34. [FEATURE] Database Migration Tool
**Öncelik:** 🟠 Yüksek  
**Etiketler:** `database`, `devops`

**Açıklama:**
`ddl-auto: update` production için uygun değil.

**Yapılması Gerekenler:**
- Flyway veya Liquibase entegrasyonu
- Version-controlled migrations
- Rollback support

---

### 35. [FEATURE] API Documentation Improvements
**Öncelik:** 🟢 Düşük  
**Etiketler:** `documentation`, `api`

**Açıklama:**
Swagger dokümantasyonu mevcut ama eksikler var.

**Yapılması Gerekenler:**
- Error response örnekleri
- Authentication header örnekleri
- Example request/response bodies
- API changelog

---

### 36. [FEATURE] Containerization Improvements
**Öncelik:** 🟡 Orta  
**Etiketler:** `devops`, `docker`

**Açıklama:**
Dockerfile optimizasyonu yapılabilir.

**Yapılması Gerekenler:**
- Multi-stage build
- Non-root user
- Layer caching optimization
- Health check in Dockerfile
- Resource limits

---

### 37. [FEATURE] Kubernetes Manifests
**Öncelik:** 🟢 Düşük  
**Etiketler:** `devops`, `k8s`

**Açıklama:**
Kubernetes deployment dosyaları yok.

**Yapılması Gerekenler:**
- Deployment manifests
- Service definitions
- ConfigMap/Secrets
- Ingress configuration
- HPA (Horizontal Pod Autoscaler)
- Helm charts

---

### 38. [FEATURE] CI/CD Pipeline
**Öncelik:** 🟡 Orta  
**Etiketler:** `devops`, `ci-cd`

**Açıklama:**
GitHub Actions veya benzeri CI/CD pipeline yok.

**Yapılması Gerekenler:**
- Build & test pipeline
- Docker image build & push
- Security scanning (SAST/DAST)
- Deployment automation

---

### 39. [BUG] Exception Handling Improvements
**Öncelik:** 🟡 Orta  
**Etiketler:** `bug`, `enhancement`

**Açıklama:**
GlobalExceptionHandler bazı exception'ları yakalamıyor.

**Yapılması Gerekenler:**
- `RuntimeException` handler
- `DataAccessException` handler
- `ConnectException` handler (RabbitMQ down)
- Stack trace'leri production'da gizle

---

### 40. [FEATURE] Email Queue Retry Mechanism
**Öncelik:** 🟡 Orta  
**Etiketler:** `reliability`, `mail-service`

**Açıklama:**
Email gönderimi başarısız olursa retry mekanizması yok.

**Yapılması Gerekenler:**
- Exponential backoff retry
- Maximum retry count
- Failed email alerting
- Manual retry endpoint

---

## 📋 Öncelik Sıralaması

### 🔴 Kritik (Hemen Yapılmalı)
1. #1 - Hardcoded JWT Secret
2. #2 - Hardcoded Database Credentials
3. #3 - RabbitMQ Default Credentials
4. #5 - Role Assignment Vulnerability
5. #14 - Unit Test Eksikliği

### 🟠 Yüksek (1-2 Hafta İçinde)
6. #4 - Grafana Weak Password
7. #6 - Missing Rate Limiting
8. #7 - Missing Brute Force Protection
9. #15 - Integration Test Eksikliği
10. #16 - Account Lockout
11. #17 - Refresh Token Rotation
12. #22 - Audit Logging
13. #33 - Token Blacklist
14. #34 - Database Migration Tool

### 🟡 Orta (Sprint Planlamasında)
15-30. Diğer maddeler

### 🟢 Düşük (Backlog)
31-40. Enhancement ve yeni özellikler

---

## 🏷️ Label Önerileri

```
security       - Güvenlik ile ilgili
critical       - Kritik öncelikli
enhancement    - İyileştirme
feature        - Yeni özellik
bug            - Hata düzeltme
testing        - Test ile ilgili
documentation  - Dokümantasyon
devops         - DevOps/CI-CD
monitoring     - İzleme/Logging
api-gateway    - API Gateway servisi
auth           - Auth servisi
mail-service   - Mail servisi
configuration  - Yapılandırma
database       - Veritabanı
rabbitmq       - RabbitMQ
```

---

*Bu dosya `microservice-auth` projesinin kod incelemesi sonucu oluşturulmuştur.*  
*Tarih: 10 Aralık 2025*
