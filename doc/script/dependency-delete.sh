#!/bin/sh
sudo systemctl start docker
sudo docker stop elasticsearch postgres cheese_legacy
sudo docker rm elasticsearch postgres cheese_legacy
sudo docker network rm cheese_network
