#!/bin/bash
# Used by the Deploy workflow: writes ~/.ssh/{deploy_key,known_hosts,config} from the production environment's secrets,
# after checking them, so that a wrongly pasted secret is reported by name rather than as a baffling SSH error.
# Needs DEPLOY_HOST, DEPLOY_USER, DEPLOY_SSH_KEY and DEPLOY_KNOWN_HOSTS in the environment. SSH_DIR is for tests.
set -euo pipefail

dir="${SSH_DIR:-$HOME/.ssh}"
fail() { echo "::error::$1"; exit 1; }

# Pasting from Windows can add carriage returns and stray spaces or newlines; none of them belong in any of these.
host="$(printf '%s' "${DEPLOY_HOST:-}" | tr -d '[:space:]')"
user="$(printf '%s' "${DEPLOY_USER:-}" | tr -d '[:space:]')"
[ -n "$host" ] || fail "The secret DEPLOY_HOST is empty or missing. Add it to the production environment (Settings -> Environments), as the server's address."
[ -n "$user" ] || fail "The secret DEPLOY_USER is empty or missing. Add it to the production environment, as: deploy"
[ -n "${DEPLOY_SSH_KEY:-}" ] || fail "The secret DEPLOY_SSH_KEY is empty or missing. Add the whole private key file to the production environment."
[ -n "${DEPLOY_KNOWN_HOSTS:-}" ] || fail "The secret DEPLOY_KNOWN_HOSTS is empty or missing. Add the output of: ssh-keyscan -t ed25519 <the server's address>"

mkdir -p "$dir"
chmod 700 "$dir" 2>/dev/null || true # (a Windows shell used to try this out can't)

printf '%s\n' "$DEPLOY_SSH_KEY" | tr -d '\r' > "$dir/deploy_key"
chmod 600 "$dir/deploy_key"
ssh-keygen -y -f "$dir/deploy_key" > /dev/null 2>&1 \
  || fail "DEPLOY_SSH_KEY is not a usable private key. Paste the whole file, from the -----BEGIN line to the -----END line, and not the .pub file. It must have no passphrase."

# Comments and blank lines are dropped. A line that is only a key (no address in front of it) gets the server's address.
printf '%s\n' "$DEPLOY_KNOWN_HOSTS" | tr -d '\r' | awk -v host="$host" '
  /^[[:space:]]*(#|$)/ { next }
  $1 ~ /^(ssh-|ecdsa-|sk-)/ { print host " " $0; next }
  { print }' > "$dir/known_hosts"
ssh-keygen -F "$host" -f "$dir/known_hosts" > /dev/null \
  || fail "DEPLOY_KNOWN_HOSTS has no host key for the address in DEPLOY_HOST. It should be one line, '<address> ssh-ed25519 AAAA...', from: ssh-keyscan -t ed25519 <address>, using the same address as DEPLOY_HOST (an IP address in one and a name in the other do not match)."

cat > "$dir/config" <<EOF
Host server
  HostName $host
  User $user
  IdentityFile $dir/deploy_key
  IdentitiesOnly yes
  StrictHostKeyChecking yes
  UserKnownHostsFile $dir/known_hosts
EOF
echo "SSH is set up for $user@(the server)."
