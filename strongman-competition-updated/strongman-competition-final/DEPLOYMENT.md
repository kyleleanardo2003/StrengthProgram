# Deploying StrongmanCast Online

This app is now prepared to run online with Docker and PostgreSQL.

## Recommended First Host

Start with Render or Railway. Both can run a Dockerized Spring Boot app and provide PostgreSQL. Render's docs state Java apps can be deployed with Docker, and Railway has a Spring Boot guide plus managed PostgreSQL.

## Required Online Environment Variables

Set these on the web service:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:PORT/DATABASE
SPRING_DATASOURCE_USERNAME=your_database_user
SPRING_DATASOURCE_PASSWORD=your_database_password
ORGANIZER_USERNAME=your_admin_username
ORGANIZER_PASSWORD=your_admin_password
```

Most hosts also provide a `PORT` variable automatically. The production profile reads it with `server.port=${PORT:8080}`.

## Render Steps

1. Push this project to GitHub.
2. In Render, choose Blueprint and select this repo.
3. Render will read `render.yaml` and create:
   - a Docker web service
   - a PostgreSQL database
4. When Render asks for unsynced environment variables, enter:
   - `ORGANIZER_USERNAME`
   - `ORGANIZER_PASSWORD`
5. Deploy.

The blueprint provides `DATABASE_URL` automatically from the Render Postgres database. StrongmanCast converts that into Spring Boot's JDBC settings at startup.

## Railway Steps

1. Push this project to GitHub.
2. Create a Railway project from the GitHub repo.
3. Add a PostgreSQL database service.
4. Add the environment variables above to the Spring Boot service. Railway can use either:
   - `DATABASE_URL`, or
   - the three explicit `SPRING_DATASOURCE_*` variables.
5. Deploy.

## Important Notes

- Local development still uses H2 through `application.properties`.
- Online production uses PostgreSQL through `application-prod.properties`.
- Render can use `render.yaml` for a one-click-ish setup.
- Public competition records do not need a login.
- Organizer pages and data-changing actions still require the organizer username/password.
- Do not upload secret JSON files, database files, or `target/` build output.
