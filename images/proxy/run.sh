#!/bin/sh

sed -i "s/__host__/$ROOT_PORT_80_TCP_ADDR/" /etc/nginx/sites-available/default
sed -i "s/__port__/$ROOT_PORT_80_TCP_PORT/" /etc/nginx/sites-available/default

nginx