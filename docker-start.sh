#!/bin/sh

set -x

docker build . --tag pleo-antaeus

docker run -e OAUTH_ISSUER=https://dev-36600335.okta.com/oauth2/default \
  --publish 7000:7000 \
  --rm \
  --interactive \
  --tty \
  --volume pleo-antaeus-build-cache:/root/.gradle \
  pleo-antaeus
