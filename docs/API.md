# API Documentation — KotlinAdmin

Base URL: `/api/v1`
Auth: `Authorization: Bearer <JWT>` (dapatkan via `POST /api/v1/auth/login`)
Content-Type: `application/json`

> **Postman collection:** [`docs/postman/KotlinAdmin.postman_collection.json`](postman/KotlinAdmin.postman_collection.json).
> Import ke Postman, lalu set variable `base_url` (default `http://localhost:8002` — sesuai `APP_PORT` di `application.conf`).

---

## Auth

| Method | Path | Route Name | Deskripsi |
|--------|------|------------|-----------|
| POST | `/api/v1/auth/login` | `api.v1.auth.login` | Login, return JWT token |
| POST | `/api/v1/auth/logout` | `api.v1.auth.logout` | Logout, blacklist token |
| GET | `/api/v1/auth/me` | `api.v1.auth.me` | Info user aktif (JWT required) |
| POST | `/api/v1/auth/register` | `api.v1.auth.register` | Register user baru |
| POST | `/api/v1/auth/reset/request` | `api.v1.auth.reset.request` | Minta OTP reset password |
| POST | `/api/v1/auth/reset/process` | `api.v1.auth.reset.process` | Proses reset password dengan OTP |

### POST /api/v1/auth/login

Request:
```json
{ "email": "admin@admin.com", "password": "12345678" }
```

Response 200:
```json
{
  "status": "success",
  "message": "Login successful",
  "data": {
    "token": "eyJhbGci...",
    "user": { "id": "...", "name": "Admin", "email": "admin@admin.com", "roles": ["Administrator"] }
  }
}
```

Response 401:
```json
{ "status": "error", "message": "Invalid credentials" }
```

### POST /api/v1/auth/logout

Headers: `Authorization: Bearer <token>`

Response 200:
```json
{ "status": "success", "message": "Logged out successfully" }
```

---

## Access — User

Path pattern: `/api/v1/access/user` (verbose paths, **bukan** REST `GET /:id`).

| Method | Path | Route Name | Deskripsi |
|--------|------|------------|-----------|
| GET | `/api/v1/access/user` | `api.v1.access.user.index` | List users (paginated + filter) |
| POST | `/api/v1/access/user/store` | `api.v1.access.user.store` | Buat user baru |
| GET | `/api/v1/access/user/{id}/edit` | `api.v1.access.user.edit` | Detail user |
| PUT | `/api/v1/access/user/{id}/update` | `api.v1.access.user.update` | Update user |
| DELETE | `/api/v1/access/user/{id}/delete` | `api.v1.access.user.delete` | Hapus user |
| POST | `/api/v1/access/user/delete_selected` | `api.v1.access.user.delete_selected` | Hapus multiple |

### GET /api/v1/access/user

Query params:
- `q_name` — filter by name (case-insensitive)
- `q_email` — filter by email
- `q_code` — filter by code
- `q_status` — `Active` | `Inactive`
- `q_role` — filter by role name
- `q_page` — page number (default: 1)
- `q_page_size` — items per page (default: 10)

Response 200:
```json
{
  "status": "success",
  "data": [
    {
      "id": "uuid", "code": "ADM001", "name": "Admin",
      "email": "admin@admin.com", "status": "Active",
      "roles": [{ "id": "uuid", "name": "Administrator" }]
    }
  ],
  "pagination": {
    "page": 1, "pageSize": 10, "total": 1,
    "totalPages": 1, "hasNext": false, "hasPrev": false
  }
}
```

### POST /api/v1/access/user/store

```json
{
  "code": "USR001", "name": "John Doe",
  "email": "john@example.com", "phone": "08123456789",
  "password": "secret1234", "status": "Active",
  "timezone": "Asia/Jakarta", "role_ids": ["uuid-role"]
}
```

### POST /api/v1/access/user/delete_selected

```json
{ "selected": ["uuid1", "uuid2"] }
```

---

## Access — Role

| Method | Path | Route Name | Deskripsi |
|--------|------|------------|-----------|
| GET | `/api/v1/access/role` | `api.v1.access.role.index` | List roles |
| POST | `/api/v1/access/role/store` | `api.v1.access.role.store` | Buat role |
| GET | `/api/v1/access/role/{id}/edit` | `api.v1.access.role.edit` | Detail role |
| PUT | `/api/v1/access/role/{id}/update` | `api.v1.access.role.update` | Update role |
| DELETE | `/api/v1/access/role/{id}/delete` | `api.v1.access.role.delete` | Hapus role |
| POST | `/api/v1/access/role/delete_selected` | `api.v1.access.role.delete_selected` | Hapus multiple |
| GET | `/api/v1/access/role/{id}/permission` | `api.v1.access.role.permission` | List permission role |
| GET | `/api/v1/access/role/{id}/permission/{pid}/assign` | `api.v1.access.role.permission.assign` | Assign permission |
| POST | `/api/v1/access/role/{id}/permission/assign_selected` | `api.v1.access.role.permission.assign_selected` | Assign bulk |
| GET | `/api/v1/access/role/{id}/permission/{pid}/unassign` | `api.v1.access.role.permission.unassign` | Unassign permission |
| POST | `/api/v1/access/role/{id}/permission/unassign_selected` | `api.v1.access.role.permission.unassign_selected` | Unassign bulk |

---

## Access — Permission

| Method | Path | Route Name | Deskripsi |
|--------|------|------------|-----------|
| GET | `/api/v1/access/permission` | `api.v1.access.permission.index` | List permissions |
| POST | `/api/v1/access/permission/store` | `api.v1.access.permission.store` | Buat permission |
| GET | `/api/v1/access/permission/{id}/edit` | `api.v1.access.permission.edit` | Detail permission |
| PUT | `/api/v1/access/permission/{id}/update` | `api.v1.access.permission.update` | Update permission |
| DELETE | `/api/v1/access/permission/{id}/delete` | `api.v1.access.permission.delete` | Hapus permission |
| POST | `/api/v1/access/permission/delete_selected` | `api.v1.access.permission.delete_selected` | Hapus multiple |

---

## Response Format

### Success
```json
{ "status": "success", "message": "...", "data": { ... } }
```

### Success (list)
```json
{
  "status": "success",
  "data": [ ... ],
  "pagination": {
    "page": 1, "pageSize": 10, "total": 100,
    "totalPages": 10, "hasNext": true, "hasPrev": false, "offset": 0
  }
}
```

### Error
```json
{ "status": "error", "message": "...", "errors": { "field": "message" } }
```

---

## Notes

- API routes gunakan HTTP method langsung (PUT/DELETE) — tidak perlu `?_method` override (itu hanya untuk web form browser).
- Authentication: semua `/api/v1/*` kecuali `/auth/login`, `/auth/register`, `/auth/reset/*` memerlukan `Authorization: Bearer <token>`.
- Rate limit: `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/reset/*` dibatasi per IP.
- Named routes API = simetris web: `api.v1.access.user.*` ↔ `admin.v1.access.user.*`
