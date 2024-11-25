#!/bin/sh
systemctl start docker
docker stop elasticsearch postgres cheese_legacy
docker rm elasticsearch postgres cheese_legacy
docker network rm cheese_network
