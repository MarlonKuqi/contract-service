# Migration Guide: v1.x → v2.0.0

## 🚨 Important: Breaking Changes

Version 2.0.0 introduces **breaking changes** that require all API consumers to update their integrations.

**⚠️ `/v1/` is NO LONGER SUPPORTED in v2.0.0**

---

## 📊 What Changed

### 1. All Endpoints Moved to `/v2/`

Every API endpoint has moved from `/v1/` to `/v2/`.

| v1.x Endpoint | v2.0.0 Endpoint | Status |
|---------------|-----------------|--------|
| `POST /v1/clients` | `POST /v2/clients` | ✅ Moved |
| `GET /v1/clients/{id}` | `GET /v2/clients/{id}` | ✅ Moved |
| `PUT /v1/clients/{id}` | `PUT /v2/clients/{id}` | ✅ Moved |
| `DELETE /v1/clients/{id}` | `DELETE /v2/clients/{id}` | ✅ Moved |
| `POST /v1/contracts` | `POST /v2/contracts` | ✅ Moved |
| `GET /v1/contracts` | `GET /v2/contracts` | ✅ Moved |
| `GET /v1/contracts/{id}` | `GET /v2/contracts/{id}` | ✅ Moved |
| `PATCH /v1/contracts/{id}/cost` | `PATCH /v2/contracts/{id}/cost` | ✅ Moved |
| `GET /v1/contracts/sum` | `GET /v2/contracts/sum` | ✅ Moved |
| `POST /v1/clients/persons` | ❌ REMOVED | Use `POST /v2/clients` |
| `POST /v1/clients/companies` | ❌ REMOVED | Use `POST /v2/clients` |

### 2. Removed Endpoints

The following deprecated endpoints have been **permanently removed**:
- ❌ `POST /v1/clients/persons`
- ❌ `POST /v1/clients/companies`

**Replacement:** Use `POST /v2/clients` with type discriminator.

---

## 🔧 Migration Steps

### Step 1: Update Base URL

Change all API calls from `/v1/` to `/v2/`:

**Before:**
```java
String baseUrl = "https://api.vaudoise.ch/v1";
```

**After:**
```java
String baseUrl = "https://api.vaudoise.ch/v2";
```

### Step 2: Migrate Person Creation

**Before (v1.x):**
```http
POST /v1/clients/persons
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+41791234567",
  "birthDate": "1990-05-15"
}
```

**After (v2.0.0):**
```http
POST /v2/clients
Content-Type: application/json

{
  "type": "PERSON",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+41791234567",
  "birthDate": "1990-05-15"
}
```

### Step 3: Migrate Company Creation

**Before (v1.x):**
```http
POST /v1/clients/companies
Content-Type: application/json

{
  "name": "Acme Corp",
  "email": "contact@acme.com",
  "phone": "+41221234567",
  "companyIdentifier": "CHE-123.456.789"
}
```

**After (v2.0.0):**
```http
POST /v2/clients
Content-Type: application/json

{
  "type": "COMPANY",
  "name": "Acme Corp",
  "email": "contact@acme.com",
  "phone": "+41221234567",
  "companyIdentifier": "CHE-123.456.789"
}
```

### Step 4: Update All Other Endpoints

Simply replace `/v1/` with `/v2/` in all your API calls:

```diff
- GET /v1/clients/{id}
+ GET /v2/clients/{id}

- PUT /v1/clients/{id}
+ PUT /v2/clients/{id}

- DELETE /v1/clients/{id}
+ DELETE /v2/clients/{id}

- POST /v1/contracts?clientId={clientId}
+ POST /v2/contracts?clientId={clientId}

- GET /v1/contracts?clientId={clientId}
+ GET /v2/contracts?clientId={clientId}

- PATCH /v1/contracts/{id}/cost?clientId={clientId}
+ PATCH /v2/contracts/{id}/cost?clientId={clientId}

- GET /v1/contracts/sum?clientId={clientId}
+ GET /v2/contracts/sum?clientId={clientId}
```

---

## 💻 Code Examples

### Java (Spring RestTemplate)

**Before:**
```java
RestTemplate restTemplate = new RestTemplate();
String url = "https://api.vaudoise.ch/v1/clients/persons";

PersonRequest request = new PersonRequest();
request.setName("John Doe");
request.setEmail("john@example.com");
request.setPhone("+41791234567");
request.setBirthDate(LocalDate.of(1990, 5, 15));

ResponseEntity<PersonResponse> response = restTemplate.postForEntity(url, request, PersonResponse.class);
```

**After:**
```java
RestTemplate restTemplate = new RestTemplate();
String url = "https://api.vaudoise.ch/v2/clients";

ClientRequest request = new ClientRequest();
request.setType("PERSON");  // ADD THIS
request.setName("John Doe");
request.setEmail("john@example.com");
request.setPhone("+41791234567");
request.setBirthDate(LocalDate.of(1990, 5, 15));

ResponseEntity<ClientResponse> response = restTemplate.postForEntity(url, request, ClientResponse.class);
```

### JavaScript (Fetch API)

**Before:**
```javascript
const response = await fetch('https://api.vaudoise.ch/v1/clients/persons', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: 'John Doe',
    email: 'john@example.com',
    phone: '+41791234567',
    birthDate: '1990-05-15'
  })
});
```

**After:**
```javascript
const response = await fetch('https://api.vaudoise.ch/v2/clients', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    type: 'PERSON',  // ADD THIS
    name: 'John Doe',
    email: 'john@example.com',
    phone: '+41791234567',
    birthDate: '1990-05-15'
  })
});
```

### cURL

**Before:**
```bash
curl -X POST https://api.vaudoise.ch/v1/clients/persons \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+41791234567",
    "birthDate": "1990-05-15"
  }'
```

**After:**
```bash
curl -X POST https://api.vaudoise.ch/v2/clients \
  -H "Content-Type: application/json" \
  -d '{
    "type": "PERSON",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+41791234567",
    "birthDate": "1990-05-15"
  }'
```

---

## ✅ Checklist

Use this checklist to ensure complete migration:

- [ ] Updated base URL from `/v1/` to `/v2/` in configuration
- [ ] Replaced `POST /v1/clients/persons` with `POST /v2/clients` (type: "PERSON")
- [ ] Replaced `POST /v1/clients/companies` with `POST /v2/clients` (type: "COMPANY")
- [ ] Updated all other endpoint calls to use `/v2/`
- [ ] Updated integration tests
- [ ] Updated documentation
- [ ] Tested in development environment
- [ ] Tested in UAT environment
- [ ] Updated Swagger/OpenAPI client if using code generation
- [ ] Updated monitoring/logging to track v2 endpoints

---

## 🆘 Support

If you encounter issues during migration:

1. Check the [CHANGELOG.md](../CHANGELOG.md) for detailed changes
2. Review [API_VERSIONING.md](../API_VERSIONING.md) for API design principles
3. Contact the API team: [team-email@vaudoise.ch]

---

## 📅 Timeline

| Date | Event |
|------|-------|
| 2025-11-10 | v2.0.0-SNAPSHOT available in DEV |
| TBD | v2.0.0-SNAPSHOT available in UAT |
| TBD | v2.0.0 released to PRODUCTION |
| TBD | **v1.x decommissioned** |

**⚠️ Important:** After production release, `/v1/` will return `404 Not Found` or `410 Gone`.

