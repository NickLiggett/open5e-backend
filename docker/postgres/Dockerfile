# Postgres with the dump restore script baked in. The script is copied into the image, rather than bind-mounted,
# so it's executable whatever the host's file permissions or file sharing do (bind-mounted scripts fail on some
# Macs with "bad interpreter: Permission denied").
FROM postgres:18
COPY --chmod=755 restore-dump.sh /docker-entrypoint-initdb.d/restore-dump.sh
