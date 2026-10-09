# Deploying to a server

The whole app on one server (a DigitalOcean Droplet, 2 vCPU / 4 GB), run with Docker Compose and deployed by GitHub
Actions. It runs at <https://dnddms.com>, and was first tried end to end on a local machine with a stand-in domain: the
stack starts, people sign in through Keycloak, and the backend accepts their tokens.

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

## What you need

- A **DigitalOcean Droplet** (Ubuntu 24.04) and a **domain** whose DNS is managed somewhere you can edit it (here, Cloudflare).
- An **email-sending service that accepts connections on a port DigitalOcean leaves open** (see [Email](#email)). Without
  one nobody can verify an account or reset a password.
- The two repositories on GitHub, with Actions enabled.

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

Check: `nslookup dnddms.com` shows the Droplet's IP. The email service's records are added in [Email](#email).

DigitalOcean's **Cloud Firewall** for the Droplet needs inbound SSH (22), HTTP (80) and HTTPS (443), and the usual outbound
rules (all TCP and UDP out). Nothing else should be open: the database and Keycloak are never published.

### 2. The deploy key

On your own computer (in PowerShell, with the OpenSSH client that Windows 11 includes):

```powershell
ssh-keygen -t ed25519 -f "$HOME\dnddms_deploy" -C "dnddms-deploy"
```

Press Enter twice for no passphrase: a passphrase would stop GitHub from using the key. This makes `dnddms_deploy` (private:
goes into GitHub) and `dnddms_deploy.pub` (public: goes onto the server).

### 3. Prepare the server

Log in to the Droplet as root (DigitalOcean's **Console** button, or `ssh root@<ip>`), then:

```sh
curl -fsSL https://raw.githubusercontent.com/NickLiggett/open5e-backend/main/deploy/server-setup.sh -o server-setup.sh
bash server-setup.sh --ssh-key "ssh-ed25519 AAAA...the line from dnddms_deploy.pub..."
```

It installs Docker, makes the `deploy` user, adds swap if there is none, turns on security updates and fail2ban, and
schedules the nightly backup. It can be run again safely. Then, from your computer, check that
`ssh -i "$HOME\dnddms_deploy" deploy@<ip>` works and that `docker ps` works there without `sudo`. Only after that, run the
script again with `--harden-ssh` added to turn off password and root logins.

From here on, work as the **`deploy`** user (`su - deploy` from root, or the `ssh` command above). Files made as root in
`/opt/dnddms` can't be read by the deploy workflow.

### 4. The server's settings

On the server, as `deploy`. This makes `.env` with random passwords and the mail settings; you then add the two things only
you know. **Run it once, before the first deploy**: Postgres takes its password from `.env` the first time its volume is
created, so changing `POSTGRES_PASSWORD` or `KEYCLOAK_DB_PASSWORD` later locks the app out of the database.

```sh
cd /opt/dnddms
cp .env.example .env
chmod 600 .env
for key in POSTGRES_PASSWORD KEYCLOAK_DB_PASSWORD KEYCLOAK_ADMIN_PASSWORD; do
  sed -i "s|^$key=.*|$key=$(openssl rand -hex 24)|" .env
done
read -r -p "Your email, for certificate notices: " v; sed -i "s|^ACME_EMAIL=.*|ACME_EMAIL=$v|" .env
read -r -s -p "Mail service password or API key: " v; echo; sed -i "s|^SMTP_PASSWORD=.*|SMTP_PASSWORD=$v|" .env; unset v
nano .env     # check SMTP_HOST, SMTP_PORT, SMTP_USERNAME and MAIL_FROM for your mail service (Email, below)
```

(`openssl rand -hex` makes letters and digits only, which is what the file needs: passwords must not contain `' " $ \`,
`|`, `&` or spaces.) Check nothing is missing, which prints no secrets:

```sh
for k in ACME_EMAIL POSTGRES_PASSWORD KEYCLOAK_DB_PASSWORD KEYCLOAK_ADMIN_PASSWORD SMTP_HOST SMTP_PORT SMTP_USERNAME SMTP_PASSWORD MAIL_FROM DOMAIN; do grep -q "^$k=.\+" .env && echo "ok       $k" || echo "MISSING  $k"; done
ls -l .env      # -rw------- ... deploy deploy
```

Keep `KEYCLOAK_ADMIN_PASSWORD` (and `POSTGRES_PASSWORD`) in a password manager; they are also in `.env`.

### 5. GitHub

In the **open5e-backend** repository: Settings → Environments → New environment → `production`. Add these **environment** secrets
(not repository secrets):

| Secret | Value |
|---|---|
| `DEPLOY_HOST` | the Droplet's IPv4 address |
| `DEPLOY_USER` | `deploy` |
| `DEPLOY_SSH_KEY` | the contents of `dnddms_deploy`: the **private** key, all lines, from `-----BEGIN OPENSSH PRIVATE KEY-----` to the `END` line. In PowerShell: `Get-Content "$HOME\dnddms_deploy" -Raw \| Set-Clipboard` |
| `DEPLOY_KNOWN_HOSTS` | one line, **the server's** address and host key: `161.35.60.33 ssh-ed25519 AAAA...`. In PowerShell: `(ssh-keyscan -t ed25519 161.35.60.33 2>$null \| Select-String 'ssh-ed25519').Line \| Set-Clipboard`. Its address must be the same as `DEPLOY_HOST`. This is *not* the `.pub` file of the deploy key |

Optionally require yourself as a reviewer on the environment, so a deploy waits for your approval.

### 6. Publish the images, then deploy

1. Merge the pull requests in both repositories. Pushing to `main` runs **Publish image** in each, which
   puts `ghcr.io/nickliggett/open5e-backend` and `ghcr.io/nickliggett/initiative-tracker` on GitHub's registry.
2. The first time, GitHub makes each package private. For each (your profile → Packages → the package → Package settings →
   Change visibility) make it **public**: nothing secret is in them, and the server can then pull them without a login.
   (Or put `GHCR_USER` and `GHCR_TOKEN`, a token with `read:packages`, in `.env`.)
3. Actions tab of open5e-backend → **Deploy** → Run workflow, with the default tags. The first run takes several minutes:
   Keycloak sets itself up and Caddy gets the certificates. It ends with `Up.` and a list of five running containers.
4. **Load the default content, on the server:** `cd /opt/dnddms && ./import-content.sh` (one to three minutes). Until you do,
   searches for creatures, items and spells find nothing; the API's counts are zero.
5. Open `https://dnddms.com`, create an account, and confirm the email address.

If a secret is wrong, the Deploy workflow stops at **Set up SSH** and names it (`.github/scripts/setup-ssh.sh` checks
each one). Line endings and stray spaces from Windows are cleaned up; what it can't fix is an empty secret, a `.pub`
file instead of the private key, a key for a different address than `DEPLOY_HOST`, or a key that isn't the server's.

## Email

Verification, password-reset and invitation emails are sent through SMTP by Keycloak and by the backend.

**DigitalOcean blocks outgoing connections on ports 25, 465 and 587** from Droplets (to stop spam), so a mail service
that only offers those ports, as PurelyMail does, cannot be used for sending from here unless you ask DigitalOcean's support to
lift the block. Use a service that also accepts mail on a port they leave open. This deployment uses **Resend**:

| Setting | Value |
|---|---|
| `SMTP_HOST` | `smtp.resend.com` |
| `SMTP_PORT` | `2587` (STARTTLS; `2465` is the implicit-TLS one). The app and Keycloak are set up for STARTTLS |
| `SMTP_USERNAME` | `resend` |
| `SMTP_PASSWORD` | a Resend API key restricted to sending from your domain |
| `MAIL_FROM` | `noreply@dnddms.com`, on the domain verified in Resend |

Set up: make a Resend account, add your domain (Domains → Add Domain) and add the DNS records it lists in Cloudflare as
**DNS only**. They are on the `send` subdomain and at `resend._domainkey`, so they don't touch the records of a mailbox
host on the same domain. Wait for **Verified**, then make the API key.

To find which ports a server can use, from the Droplet (`portquiz.net` listens on every port):

```sh
for p in 443 25 465 587 2465 2525 2587 8025; do timeout 6 bash -c "</dev/tcp/portquiz.net/$p" 2>/dev/null && echo "port $p: open" || echo "port $p: blocked"; done
```

### Changing the mail settings afterwards

There are two copies of the settings. The backend (invitation emails) reads `.env`. **Keycloak keeps its own in its
database, written the first time it started, and ignores `.env` after that.** To change both, edit `.env`, then:

```sh
cd /opt/dnddms
set -a; . ./.env; set +a
K="docker compose -f compose.prod.yaml exec -T keycloak /opt/keycloak/bin/kcadm.sh"
$K config credentials --server http://localhost:8080 --realm master --user "$KEYCLOAK_ADMIN_USER" --password "$KEYCLOAK_ADMIN_PASSWORD"
$K update realms/open5e -s smtpServer.host="$SMTP_HOST" -s smtpServer.port="$SMTP_PORT" -s smtpServer.user="$SMTP_USERNAME" -s "smtpServer.password=$SMTP_PASSWORD" -s smtpServer.auth=true -s smtpServer.starttls=true -s smtpServer.ssl=false -s "smtpServer.from=$MAIL_FROM"
docker compose -f compose.prod.yaml up -d backend
```

This works without opening the admin console to the internet. To look at the result: `$K get realms/open5e | grep -A 12 '"smtpServer"'`
(the password shows as `**********`; `--fields smtpServer` alone prints an empty `{ }`, which is only how `kcadm` filters).

## Day to day

- **Deploy a new version:** merging to `main` publishes new images; run **Deploy** to put them on the server.
- **Roll back:** run **Deploy** with the tags of the earlier version (`sha-1234567`; each Publish run lists its tags). The
  server keeps the last deployed tags in `/opt/dnddms/.last-deploy`. Database changes made by a newer version are not undone.
- **Logs:** `cd /opt/dnddms && docker compose -f compose.prod.yaml logs -f --tail=100 backend` (or `keycloak`, `caddy`).
- **State:** `docker compose -f compose.prod.yaml ps`, and `docker stats --no-stream` for memory.
- **Load the Open5e content:** `./import-content.sh` (try `./import-content.sh dry-run` first). It runs a second copy of the
  backend for a minute and reads the snapshot of Open5e's API that is in the image, so it doesn't call `api.open5e.com`. To
  get newer content from Open5e, refresh the snapshot in the repository and deploy (see "Refreshing default content" in the
  backend README); `./import-content.sh apply all api` fetches it live instead.
- **Load new custom content** (after deploying a version with a changed `custom-content/*.json`, or changing a private file):
  `./import-content.sh apply custom`. It applies only the custom content, doesn't need the Open5e API, and leaves Open5e's rows alone.

## Private content

Books and other material that isn't ours to publish stay out of the repository and off the public site. They are JSON files in
`/opt/dnddms/private-content` on the server (a folder only `deploy` can read, mounted into the backend), and each is loaded as
the **own documents of one user** (see "Custom content" in the main README for the format). Nobody else sees it until that
user shares it on the Sharing page.

```sh
# from your computer: the file has "owner": "<your username on the site>" at the top
scp private-content/my-book.json deploy@161.35.60.33:/opt/dnddms/private-content/
# on the server (you must have signed in to the site once, so the account exists):
cd /opt/dnddms && ./import-content.sh apply custom
```

Then, on the site: **Sharing → your document → share with a username or email**, as a viewer. To take it down, delete the file,
then delete the document on the Sharing page (an import doesn't remove a document whose file is gone). Back up
`/opt/dnddms/private-content` along with the databases: it is on no one else's machine.

## Keycloak's admin console

`https://auth.dnddms.com/admin` is only open to the addresses in `ADMIN_IPS` in `.env`. To use it, put your public IP there
(search "what is my IP"), then apply it: `docker compose -f compose.prod.yaml up -d`. Log in as `KEYCLOAK_ADMIN_USER`. Set
`ADMIN_IPS=127.0.0.1` again when done. The realm file is read only the first time Keycloak starts; later changes
(a new redirect address, mail settings) are made in the console, or with `kcadm` as above, or by recreating the database.

### Verifying an account by hand

When mail isn't working yet, or someone never gets their email, you can mark an account's address verified:

```sh
cd /opt/dnddms
set -a; . ./.env; set +a
K="docker compose -f compose.prod.yaml exec -T keycloak /opt/keycloak/bin/kcadm.sh"
$K config credentials --server http://localhost:8080 --realm master --user "$KEYCLOAK_ADMIN_USER" --password "$KEYCLOAK_ADMIN_PASSWORD"
$K get users -r open5e --fields id,username,email,emailVerified      # find the id
$K update users/THE-ID -r open5e -s emailVerified=true
```

Only do this for people you know: the check exists so that an invitation can't be claimed with an address someone doesn't own.

## Backups

`backup.sh` runs every night at 03:15 and keeps 14 days of dumps of both databases in `/var/backups/dnddms`. **They are on
the same server as the data**, so copy them off it: turn on DigitalOcean's Droplet backups (about 20% of the plan price), or
`scp` them somewhere regularly. To restore, see the comment at the top of `backup.sh`.

## When something is wrong

- **The site doesn't load, or the browser warns about the certificate:** `docker compose -f compose.prod.yaml logs caddy`. The usual
  causes are DNS not pointing at the server yet, the Cloudflare orange cloud being on, or ports 80/443 closed in the
  DigitalOcean firewall.
- **502 for a minute after a deploy:** the backend is still starting. If it stays, read the backend's logs.
- **Searches find nothing:** the default content hasn't been imported (`./import-content.sh`).
- **Everyone is signed out or sign-in fails with "invalid issuer":** `DOMAIN` in `.env` changed after Keycloak was first
  started; its stored address is the old one.
- **"Failed to send email, please try again later" when signing up:** Keycloak couldn't send. Read its log:
  `docker compose -f compose.prod.yaml logs --since 30m keycloak 2>&1 | grep -iE "smtp|mail|authenticat|timed out|refused" | tail`.
  `Connect timed out` means the port is blocked (see [Email](#email)); an authentication error means the password Keycloak holds is
  wrong (change it as in "Changing the mail settings afterwards"). The account was still made; once mail works, signing in offers to
  resend the email.
- **Verification emails don't arrive but nothing is wrong in the log:** the mail service's dashboard shows whether it was sent and
  delivered; then the spam folder, then the domain's SPF/DKIM/DMARC records.
- **The Deploy workflow can't read `.env`, or fails with "permission denied" there:** the file was made as root. As root:
  `chown deploy:deploy /opt/dnddms/.env && chmod 600 /opt/dnddms/.env`.
- **The app can't connect to the database after you changed a password in `.env`:** Postgres kept the password it was created with.
  Put the old value back (or reset it inside Postgres).
- **Out of memory:** `docker stats`, then `dmesg | grep -i oom`. The limits in `compose.prod.yaml` are a starting point.
