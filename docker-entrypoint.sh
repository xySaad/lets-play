#!/bin/sh
set -e

# TLS: generate a self-signed certificate on first start (development / audit use).
# For a real deployment mount a CA-signed PKCS12 keystore at SSL_KEYSTORE_PATH instead.
if [ ! -f "$SSL_KEYSTORE_PATH" ]; then
  echo "[entrypoint] generating self-signed certificate at $SSL_KEYSTORE_PATH"
  keytool -genkeypair -alias letsplay -keyalg RSA -keysize 2048 -validity 365 \
    -storetype PKCS12 -keystore "$SSL_KEYSTORE_PATH" -storepass "$SSL_KEYSTORE_PASSWORD" \
    -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"
fi

exec java -jar app.jar
