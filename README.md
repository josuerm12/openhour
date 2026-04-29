# OpenHour

OpenHour is a full-stack appointment booking site for a small hair business. The frontend is static HTML/CSS/JavaScript and the backend is Spring Boot with REST APIs, H2 database persistence, and Stripe Checkout integration for deposits and donations.

## Run Locally

Start the backend:

```bash
cd backend
STRIPE_SECRET_KEY=sk_test_your_key_here \
ADMIN_USERNAME=owner \
ADMIN_PASSWORD=choose-a-local-password \
ADMIN_TOKEN=choose-a-long-random-token \
FRONTEND_URL=http://localhost:5500 \
mvn spring-boot:run
```

Start the frontend from the repo root:

```bash
python3 -m http.server 5500
```

Open:

```text
http://localhost:5500/frontend/index.html
```

Owner login:

```text
username: the value of ADMIN_USERNAME
password: the value of ADMIN_PASSWORD
```

There are no committed default owner credentials. Set `ADMIN_USERNAME`, `ADMIN_PASSWORD`, and `ADMIN_TOKEN` in your environment.

## Database

Local data is stored in an H2 database at `backend/data/openhour`. To enable the H2 console for local debugging only, start the backend with `H2_CONSOLE_ENABLED=true`:

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
