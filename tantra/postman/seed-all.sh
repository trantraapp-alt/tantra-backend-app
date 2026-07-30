#!/usr/bin/env bash
# Seed the entire catalog by running the Postman collections in order with Newman (Postman's CLI).
# Prereqs: the app is running, and (for a clean run) a fresh DB — forms are created, not upserted,
# so re-running against an existing DB will conflict on the unique (category, listingType, version).
#
# Usage:
#   ./seed-all.sh                       # seeds against http://localhost:8080
#   ./seed-all.sh http://192.168.1.5:8080
#
# Needs Node.js. Uses `npx newman` (fetched on demand — no global install required).

set -euo pipefail
BASE_URL="${1:-http://localhost:8080}"
DIR="$(cd "$(dirname "$0")" && pwd)"

# Order matters: catalog modules first, then the shared business-profile form, then sample addresses.
COLLECTIONS=(
  "00_Agriculture_Seed"        # agriculture module -> Marketplace/Services/Repair + dropdowns + forms
  "12_AnimalLivestock_Seed"    # animal_livestock module -> Veterinary + Poultry/Fishery/Animal
  "11_BusinessProfile"         # business-profile form + profile types (+ verification demo)
  "10_Address"                 # sample addresses for the test user (optional)
)

echo "Seeding Tantra catalog against ${BASE_URL}"
for c in "${COLLECTIONS[@]}"; do
  echo ""
  echo "==================== ▶ ${c} ===================="
  npx --yes newman run "${DIR}/${c}.postman_collection.json" \
    --env-var "baseUrl=${BASE_URL}" \
    --reporters cli
done

echo ""
echo "✅ Done. Catalog seeded."
