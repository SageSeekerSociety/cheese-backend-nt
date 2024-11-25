#!/bin/sh
systemctl start docker
docker restart elasticsearch postgres cheese_legacy
