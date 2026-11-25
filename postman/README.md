## Setup

```bash
cp jar.example.json jar.json
run() {
  local f="$1"; shift
  npx postman-cli collection run "$f" -k --verbose --reporter-cli-no-summary \
    --cookie-jar jar.json --export-cookie-jar jar.json "$@"
}
```

## Usage Examples

```bash
run 02-login.request.yaml --env-var rootPassword=YOUR_ROOT_PASSWORD
run 05-product-get.request.yaml --env-var productId=THE_ID
run 08-user-create.request.yaml
```