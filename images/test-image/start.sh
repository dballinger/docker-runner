#!/bin/sh

echo $TEST_VAR > /var/www/html/vars.txt
cp /etc/resolv.conf /var/www/html/dns.txt

nginx