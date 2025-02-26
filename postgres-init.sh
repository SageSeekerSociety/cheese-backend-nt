#!/bin/bash
set -e

echo "等待PostgreSQL配置文件创建..."
until [ -f /var/lib/postgresql/data/postgresql.conf ]
do
  sleep 1
done

echo "修改PostgreSQL配置..."
sed -i 's/max_connections = 100/max_connections = 1000/' /var/lib/postgresql/data/postgresql.conf
sed -i 's/shared_buffers = 128MB/shared_buffers = 2GB/' /var/lib/postgresql/data/postgresql.conf

echo "PostgreSQL配置已更新"