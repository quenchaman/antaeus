#!/bin/sh

set -x

docker build . --tag pleo-antaeus

docker run \
  --publish 7000:7000 \
  --rm \
  --interactive \
  --tty \
  --volume pleo-antaeus-build-cache:/root/.gradle \
  pleo-antaeus
