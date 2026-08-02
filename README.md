# ShortNx

A URL shortener + expander built with plain Java Servlets, JDBC (HikariCP
pooling) against Supabase Postgres, and vanilla HTML/CSS/JS on the frontend.

## Project layout

```
shortnx/
├── pom.xml
├── sql/schema.sql                 ← run this in Supabase SQL editor first
└── src/main/
    ├── java/com/shortnx/
    │   ├── db/DBConfig.java       ← HikariCP pool, reads env vars only
    │   ├── util/                  ← CodeGenerator, ValidationUtil, PasswordUtil,
    │   │                            CsrfUtil, RateLimiter
    │   └── servlet/               ← Signup, Login, Logout, Shorten, Redirect,
    │                                 Links, Analytics, CsrfToken,
    │                                 SecurityHeadersFilter, AuthFilter
    └── webapp/
        ├── index.html, shorten.html, login.html, signup.html,
        │   dashboard.html, 404.html
        ├── css/style.css
        ├── js/app.js
        ├── robots.txt, sitemap.xml
        └── WEB-INF/web.xml
```

## Setup (Termux)

```bash
pkg install openjdk-17 maven wget
```

1. Run `sql/schema.sql` in the Supabase SQL editor.
2. Get your **Session pooler** connection string from Supabase →
   Project Settings → Database (port 6543, not the direct 5432 one —
   servlet containers under load exhaust direct connections fast).
3. Export credentials before starting Tomcat — never hardcode them:
   ```bash
   export DB_URL="jdbc:postgresql://<project>.pooler.supabase.com:6543/postgres"
   export DB_USER="postgres.xxxxxxxx"
   export DB_PASSWORD="..."
   ```
4. Build and deploy:
   ```bash
   mvn clean package
   cp target/shortnx.war $TOMCAT_HOME/webapps/
   $TOMCAT_HOME/bin/startup.sh
   ```
5. Visit `http://localhost:8080/shortnx/`.

## Security hardening included

- **SQL injection** — every query uses `PreparedStatement`, no string
  concatenation of user input anywhere.
- **Password storage** — bcrypt (work factor 12) via jBCrypt, never
  plaintext or fast hashes like SHA-256.
- **XSS** — server escapes any user input reflected into HTML
  (`ValidationUtil.escapeHtml`); the dashboard also escapes on the
  client since link data came from the DB, not the current request.
- **CSRF** — session-bound token required on every state-changing POST
  (`CsrfUtil`), fetched via `/api/csrf-token` and checked server-side.
- **Open-redirect / scheme injection** — only `http`/`https` long URLs
  are accepted, blocking `javascript:`/`file:` payloads.
- **IDOR** — every dashboard/analytics query filters by
  `user_id = ?` from the session, not a client-supplied id, so users
  can't read or delete each other's links by guessing an id.
- **Session fixation** — session is invalidated and reissued on login;
  cookies are `HttpOnly`, `Secure`, `SameSite=Strict`.
- **Rate limiting** — login, signup, and shorten endpoints are capped
  per-IP per-minute (`RateLimiter`) to slow brute force and abuse.
- **Security headers** — CSP, `X-Frame-Options`, `X-Content-Type-Options`,
  `Referrer-Policy`, HSTS applied to every response
  (`SecurityHeadersFilter`).
- **No enumeration** — login returns the same error for "no such user"
  and "wrong password".
- **Privacy in logging** — click logs store a SHA-256 hash of the IP,
  not the raw address.
- **Short code entropy** — codes are generated with `SecureRandom`, not
  `Math.random()`, so links can't be guessed/enumerated.
- **Known limitation to mention in interviews**: the rate limiter is
  in-memory, so it resets on restart and doesn't share state across
  multiple app instances — call this out and say you'd back it with
  Redis for a real multi-server deployment.

## Traditional SEO included

- Unique `<title>` and `<meta name="description">` per page.
- `<link rel="canonical">` on every indexable page.
- Open Graph + Twitter card tags on the home page.
- `WebApplication` JSON-LD structured data on the home page.
- Semantic HTML: one `<h1>` per page, proper heading hierarchy,
  `<nav>`/`<main>`/`<footer>` landmarks.
- `robots.txt` disallowing `/dashboard.html` and `/api/`, pointing to
  the sitemap.
- `sitemap.xml` listing indexable pages with priority/changefreq.
- `noindex` on login/signup/dashboard/404 — nothing behind auth gets
  crawled.
- Descriptive, keyword-relevant link/button text ("Shorten a link —
  it's free" rather than "Click here").
- 302 (not 301) redirects on short links, which also happens to keep
  click analytics accurate since browsers won't cache a 301.

## Talking points for your resume/interview

- Why `SecureRandom` over `Math.random()` for short codes.
- Why bcrypt over SHA-256 for passwords (slow-by-design vs fast hash).
- Why 302 over 301 for the redirect (cacheability vs analytics).
- Why click logging is fire-and-forget on a virtual thread instead of
  blocking the redirect.
- Why the connection pool uses Supabase's pooler endpoint instead of
  the direct Postgres port.
