# Deploying to a server

The whole app on one server (a DigitalOcean Droplet, 2 vCPU / 4 GB), run with Docker Compose and deployed by GitHub
Actions. It has been tried end to end on a local machine with a stand-in domain: the stack starts, people sign in
through Keycloak, and the backend accepts their tokens.

```
                    https://dnddms.com         ┌──────────┐
 browser ──HTTPS──▶ Caddy ─ /api/* ──────────▶ │ backend  │──┐
              │       │                        └──────────┘  │   ┌──────────┐
              │       └ everything else ─────▶ frontend (nginx)  ├─▶│ Postgres │ (app + Keycloak databases)
              │                                               │   └──────────┘
              └──────▶ https://auth.dnddms.com ─────▶ Keycloak ┘
```

Caddy is the only thing facing the internet. It gets and renews the HTTPS certificates itself. The files here:

| File | What it is |
|---|---|
| `compose.prod.yaml` | The five containers, with memory limits (about 1.1 GB used when idle) |
| `Caddyfile` | HTTPS, and which address goes where; keeps Keycloak's admin console to `ADMIN_IPS` |
| `.env.example` | Every setting, to copy to `.env` on the server |
| `keycloak/open5e-realm.template.json` | The sign-in realm for production: only the web app's client, email verification on, brute-force protection, 10-character passwords, no test users |
| `postgres-init/` | Makes Keycloak's own database the first time Postgres starts |
| `server-setup.sh` | One-time server preparation |
| `deploy.sh` | Pulls the images and starts them (the Deploy workflow runs it) |
| `import-content.sh` | Loads the default Open5e content |
| `backup.sh` | Nightly database dump (the setup script schedules it) |

## One-time setup

### 1. DNS (Cloudflare)

In the Cloudflare dashboard, DNS → Records, for `dnddms.com`:

| Type | Name | Content | Proxy status |
|---|---|---|---|
| A | `@` | the Droplet's IPv4 | **DNS only** (grey cloud) |
| A | `auth` | the Droplet's IPv4 | **DNS only** (grey cloud) |

**Turn the orange cloud off.** While it is on, the names point at Cloudflare rather than your server, and Caddy can't get its
certificates the normal way. (It can be turned back on later, with SSL/TLS mode set to *Full (strict)*; remember that
the admin-console restriction then sees Cloudflare's addresses, not yours.)

Check: `nslookup dnddms.com` shows the Droplet's IP.

### 2. The deploy key

On your own computer:

```sh
ssh-keygen -t ed25519 -f dnddms_deploy -C "dnddms-deploy" -N ""
```

This makes `dnddms_deploy` (private: goes into GitHub, then delete it from your computer) and `dnddms_deploy.pub` (public:
goes onto the server).

### 3. Prepare the server

Log in to the Droplet as root (DigitalOcean's console, or `ssh root@<ip>`), then:

```sh
curl -fsSL https://raw.githubusercontent.com/NickLiggett/open5e-backend/main/deploy/server-setup.sh -o server-setup.sh
bash server-setup.sh --ssh-key "ssh-ed25519 AAAA...the line from dnddms_deploy.pub..."
```

(This works once the deployment pull request is merged to `main`; before that, paste the script's contents into a file
on the server.) It installs Docker, makes the `deploy` user, adds 2 GB of swap, turns on security updates and fail2ban,
and schedules the nightly backup. Then, from your computer, check that `ssh -i dnddms_deploy deploy@<ip>` works. Only
after that, run the script again with `--harden-ssh` added to turn off password and root logins.

### 4. The server's settings

Still on the server, as `deploy`:

```sh
cd /opt/dnddms
cp .env.example .env
chmod 600 .env
nano .env                # fill it in; make passwords with: openssl rand -base64 24
```

Passwords must not contain `' " $ \` or spaces. You need an email service for `SMTP_*` and `MAIL_FROM` (Brevo, Mailgun and
Resend have free tiers); without it nobody can verify an account or reset a password.

### 5. GitHub

In the **open5e-backend** repository: Settings → Environments → New environment → `production`. Add these secrets to it:

| Secret | Value |
|---|---|
| `DEPLOY_HOST` | the Droplet's IPv4 address |
| `DEPLOY_USER` | `deploy` |
| `DEPLOY_SSH_KEY` | the contents of `dnddms_deploy` (the private key, all lines) |
| `DEPLOY_KNOWN_HOSTS` | the output of `ssh-keyscan -t ed25519 <the Droplet's IP>` (run on your computer) |

Optionally require yourself as a reviewer on the environment, so a deploy waits for your approval.

### 6. Publish the images, then deploy

1. Merge the pull requests (CI, then the deployment ones) in both repositories. Pushing to `main` runs **Publish image** in each, which
   puts `ghcr.io/nickliggett/open5e-backend` and `ghcr.io/nickliggett/initiative-tracker` on GitHub's registry.
2. The first time, GitHub makes each package private. For each (your profile → Packages → the package → Package settings →
   Change visibility) make it **public**: nothing secret is in them, and the server can then pull them without a login.
   (Or put `GHCR_USER` and `GHCR_TOKEN`, a token with `read:packages`, in `.env`.)
3. Actions tab of open5e-backend → **Deploy** → Run workflow. The first run takes a few minutes: Keycloak sets itself up
   and Caddy gets the certificates.
4. Load the default content, on the server: `cd /opt/dnddms && ./import-content.sh` (about a minute).
5. Open `https://dnddms.com`, create an account, and confirm the email address.

## Day to day

- **Deploy a new version:** merging to `main` publishes new images; run **Deploy** to put them on the server.
- **Roll back:** run **Deploy** with the tags of the earlier version (`sha-1234567`; each Publish run lists its tags). The
  server keeps the last deployed tags in `/opt/dnddms/.last-deploy`. Database changes made by a newer version are not undone.
- **Logs:** `cd /opt/dnddms && docker compose -f compose.prod.yaml logs -f --tail=100 backend` (or `keycloak`, `caddy`).
- **State:** `docker compose -f compose.prod.yaml ps`, and `docker stats --no-stream` for memory.
- **Refresh the Open5e content:** `./import-content.sh` (try `./import-content.sh dry-run` first).

## Keycloak's admin console

`https://auth.dnddms.com/admin` is only open to the addresses in `ADMIN_IPS` in `.env`. To use it, put your public IP there
(search "what is my IP"), then apply it: `docker compose -f compose.prod.yaml up -d`. Log in as `KEYCLOAK_ADMIN_USER`. Set
`ADMIN_IPS=127.0.0.1` again when done. The realm file is read only the first time Keycloak starts; later changes
(a new redirect address, mail settings) are made in the console, or by recreating the database.

## Backups

`backup.sh` runs every night at 03:15 and keeps 14 days of dumps of both databases in `/var/backups/dnddms`. **They are on
the same server as the data**, so copy them off it: turn on DigitalOcean's Droplet backups (about 20% of the plan price), or
`scp` them somewhere regularly. To restore, see the comment at the top of `backup.sh`.

## When something is wrong

- **The site doesn't load, or the browser warns about the certificate:** `docker compose -f compose.prod.yaml logs caddy`. The usual
  causes are DNS not pointing at the server yet, the Cloudflare orange cloud being on, or ports 80/443 closed in the
  DigitalOcean firewall.
- **502 for a minute after a deploy:** the backend is still starting. If it stays, read the backend's logs.
- **Everyone is signed out or sign-in fails with "invalid issuer":** `DOMAIN` in `.env` changed after Keycloak was first
  started; its stored address is the old one.
- **Verification emails don't arrive:** check `SMTP_*` and `MAIL_FROM`, the mail service's DNS records, and the spam folder.
  Keycloak's mail settings were fixed in the realm at its first start; change them in the admin console afterwards.
- **Out of memory:** `docker stats`, then `dmesg | grep -i oom`. The limits in `compose.prod.yaml` are a starting point.
