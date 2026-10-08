#!/bin/bash
# One-time setup of a fresh Ubuntu 24.04 server (a DigitalOcean Droplet), run as root:
#
#   curl -fsSL https://raw.githubusercontent.com/NickLiggett/open5e-backend/main/deploy/server-setup.sh -o server-setup.sh
#   sudo bash server-setup.sh --ssh-key "ssh-ed25519 AAAA... deploy-key-comment"
#
# It installs Docker, makes a "deploy" user that the GitHub workflow logs in as (with the public half of the deploy key
# you pass), creates /opt/dnddms, adds swap, turns on automatic security updates and fail2ban, and schedules the nightly
# database backup. It can be run again safely. Add --harden-ssh to also turn off password and root logins over SSH, but
# only after you have checked that logging in as the deploy user with the key works, or you will lock yourself out.
set -euo pipefail

ssh_key=""
harden=false
while [ $# -gt 0 ]; do
  case "$1" in
    --ssh-key) ssh_key="${2:-}"; shift 2 ;;
    --harden-ssh) harden=true; shift ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done

[ "$(id -u)" -eq 0 ] || { echo "Run this as root (sudo bash server-setup.sh ...)" >&2; exit 1; }
[ -n "$ssh_key" ] || { echo "Pass the deploy key's PUBLIC half: --ssh-key \"ssh-ed25519 AAAA...\"" >&2; exit 2; }
case "$ssh_key" in ssh-ed25519\ *|ssh-rsa\ *|ecdsa-sha2-*) ;; *) echo "That doesn't look like a public key (it starts ssh-ed25519, ssh-rsa or ecdsa-...)" >&2; exit 2 ;; esac

export DEBIAN_FRONTEND=noninteractive

echo "== Packages"
apt-get update -qq
apt-get upgrade -y -qq
apt-get install -y -qq curl ca-certificates gettext-base unattended-upgrades fail2ban cron

echo "== Docker"
if ! command -v docker > /dev/null; then
  curl -fsSL https://get.docker.com | sh
fi
docker compose version

echo "== The deploy user"
if ! id deploy > /dev/null 2>&1; then
  adduser --disabled-password --gecos "" deploy
fi
usermod -aG docker deploy
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
touch /home/deploy/.ssh/authorized_keys
grep -qxF "$ssh_key" /home/deploy/.ssh/authorized_keys || echo "$ssh_key" >> /home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys
install -d -m 755 -o deploy -g deploy /opt/dnddms
install -d -m 750 -o deploy -g deploy /var/backups/dnddms
# Private content: JSON files for books and the like that aren't ours to publish. Only on this server, never in git.
# Readable by everyone, because the backend's container runs as another user (this server has only deploy and root).
install -d -m 755 -o deploy -g deploy /opt/dnddms/private-content
# The settings template, so that .env can be made before the first deploy copies the rest of the files here.
if [ ! -f /opt/dnddms/.env.example ]; then
  curl -fsSL https://raw.githubusercontent.com/NickLiggett/open5e-backend/main/deploy/.env.example -o /opt/dnddms/.env.example
  chown deploy:deploy /opt/dnddms/.env.example
fi

echo "== Swap"
if ! swapon --show | grep -q .; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile > /dev/null
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo 'vm.swappiness=10' > /etc/sysctl.d/99-swappiness.conf
  sysctl -q -p /etc/sysctl.d/99-swappiness.conf
fi

echo "== Security updates and fail2ban"
dpkg-reconfigure -f noninteractive unattended-upgrades
systemctl enable --now fail2ban

echo "== Nightly backup"
cat > /etc/cron.d/dnddms-backup <<'CRON'
15 3 * * * deploy cd /opt/dnddms && ./backup.sh >> /var/backups/dnddms/backup.log 2>&1
CRON
chmod 644 /etc/cron.d/dnddms-backup

if $harden; then
  echo "== Hardening SSH"
  install -d /etc/ssh/sshd_config.d
  cat > /etc/ssh/sshd_config.d/10-dnddms.conf <<'SSH'
PasswordAuthentication no
PermitRootLogin no
SSH
  sshd -t
  systemctl reload ssh
fi

echo
echo "Done. Next: as the deploy user, copy /opt/dnddms/.env.example to /opt/dnddms/.env and fill it in, add the GitHub"
echo "secrets, then run the Deploy workflow (see deploy/README.md)."
