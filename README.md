# OpenHour

OpenHour is a full-stack appointment booking site for a small hair business. The frontend is static HTML/CSS/JavaScript and the backend is Spring Boot with REST APIs, H2 database persistence, and Stripe Checkout integration for deposits and donations.

## Run Locally

Start the backend:

```bash
cd backend
STRIPE_SECRET_KEY=sk_test_your_key_here FRONTEND_URL=http://localhost:5500 mvn spring-boot:run
```

Start the frontend from the repo root:

```bash
python3 -m http.server 5500
```

Open:

```text
http://localhost:5500/frontend/index.html
```

Owner login defaults:

```text
username: owner
password: openhour
```

You can override these with `ADMIN_USERNAME` and `ADMIN_PASSWORD`.

## Database

Local data is stored in an H2 database at `backend/data/openhour`. The H2 console is available while the backend is running:

```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/openhour
Username: sa
Password: leave blank
```

## Main API Areas

- `GET /api/services`
- `GET /api/availability?start=YYYY-MM-DD&end=YYYY-MM-DD`
- `PUT /api/availability`
- `POST /api/appointments/checkout`
- `POST /api/appointments/{id}/confirm?session_id=...`
- `GET /api/appointments`
- `PATCH /api/appointments/{id}/cancel`
- `PATCH /api/appointments/{id}/move`
- `POST /api/donations/checkout`
- `GET /api/activity`
