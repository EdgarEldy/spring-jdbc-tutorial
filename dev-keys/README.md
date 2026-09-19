# Development JWT keys

RSA 2048 key pair used ONLY for local development and for the automated tests
(RS256, PKCS#8 private key, X.509 public key, PEM). They are public on purpose:
never use them anywhere real.

They live here, at the repository root, and never under `src/main/resources`,
so they are never packaged inside the WAR.

Production keys are generated separately into `./keys` (ignored by git) and
mounted into the container, see `docker-compose.yml`. There is no default key:
the application refuses to start without `app.jwt.private-key-location` and
`app.jwt.public-key-location`.

## Development admin bootstrap (optional)

There is no seeded user. To get a first administrator locally, export these
variables BEFORE starting Tomcat (dummy values, development only, never set them
in a test or production environment):

    export APP_BOOTSTRAP_ADMIN_EMAIL=admin@example.test
    export APP_BOOTSTRAP_ADMIN_PASSWORD=dev-only-Admin-1

When both are present and no user has that email, the account is created enabled
with the seeded ADMIN role. When absent, nothing happens. A failure here never
stops the application from starting.
