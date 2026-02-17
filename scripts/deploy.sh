#!/bin/bash
set -e

ENVIRONMENT=${1:-development}

echo "Deploying VaultStadio to $ENVIRONMENT..."

if [ "$ENVIRONMENT" == "production" ]; then
    echo "Deploying to production..."
    # Add production deployment logic here
    # helm upgrade --install vaultstadio ./helm/vaultstadio -f helm/vaultstadio/values-prod.yaml
else
    echo "Deploying to development..."
    # Add development deployment logic here
    # helm upgrade --install vaultstadio ./helm/vaultstadio
fi

echo "Deployment complete!"
