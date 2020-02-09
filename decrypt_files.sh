#!/bin/sh

gpg --quiet --batch --yes --decrypt --passphrase="$SERVICES_PASSWORD" --output app/google-services.json app/google-services.json.gpg
