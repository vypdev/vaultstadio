#!/bin/bash
#
# VaultStadio Federation Key Generator
#
# Generates Ed25519 key pairs for federation between VaultStadio instances.
# Keys are output in Base64 format suitable for configuration.
#
# Usage:
#   ./scripts/generate-federation-keys.sh
#   ./scripts/generate-federation-keys.sh --output /path/to/keys
#
# Environment variables will be output to stdout and can be added to .env

set -euo pipefail

OUTPUT_DIR="${1:-}"

echo "==============================================="
echo "VaultStadio Federation Key Generator"
echo "==============================================="
echo ""

# Check for required tools
if ! command -v openssl &> /dev/null; then
    echo "Error: openssl is required but not installed."
    exit 1
fi

# Create temporary directory for key files
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# Generate Ed25519 key pair
echo "Generating Ed25519 key pair..."
openssl genpkey -algorithm Ed25519 -out "$TEMP_DIR/private.pem" 2>/dev/null
openssl pkey -in "$TEMP_DIR/private.pem" -pubout -out "$TEMP_DIR/public.pem" 2>/dev/null

# Convert to DER format and then Base64
PRIVATE_KEY_DER=$(openssl pkey -in "$TEMP_DIR/private.pem" -outform DER 2>/dev/null | base64 | tr -d '\n')
PUBLIC_KEY_DER=$(openssl pkey -in "$TEMP_DIR/private.pem" -pubout -outform DER 2>/dev/null | base64 | tr -d '\n')

echo "Keys generated successfully!"
echo ""
echo "==============================================="
echo "Add these to your .env or docker-compose.yml:"
echo "==============================================="
echo ""
echo "FEDERATION_PRIVATE_KEY=$PRIVATE_KEY_DER"
echo ""
echo "FEDERATION_PUBLIC_KEY=$PUBLIC_KEY_DER"
echo ""
echo "==============================================="
echo ""

# Optionally save to file
if [ -n "$OUTPUT_DIR" ]; then
    mkdir -p "$OUTPUT_DIR"
    echo "FEDERATION_PRIVATE_KEY=$PRIVATE_KEY_DER" > "$OUTPUT_DIR/federation.env"
    echo "FEDERATION_PUBLIC_KEY=$PUBLIC_KEY_DER" >> "$OUTPUT_DIR/federation.env"
    echo "Keys saved to: $OUTPUT_DIR/federation.env"
    echo ""
    echo "IMPORTANT: Keep federation.env secure! The private key must not be shared."
fi

echo "SECURITY NOTES:"
echo "  - Never commit private keys to version control"
echo "  - Store private keys securely (e.g., secrets manager)"
echo "  - Each VaultStadio instance needs its own key pair"
echo "  - Share only the PUBLIC key with other instances"
