FROM ubuntu

# Install Nginx.
RUN \
  apt-get update && \
  sudo apt-get install -y software-properties-common python-software-properties && \
  add-apt-repository -y ppa:nginx/stable && \
  apt-get update && \
  apt-get install -y nano && \
  apt-get install -y nginx && \
  rm -rf /var/lib/apt/lists/* && \
  echo "\ndaemon off;" >> /etc/nginx/nginx.conf && \
  chown -R www-data:www-data /var/lib/nginx

# Define mountable directories.
VOLUME ["/etc/nginx/sites-enabled", "/etc/nginx/certs", "/etc/nginx/conf.d", "/var/log/nginx", "/var/www/html"]

ADD ok /var/www/html/ok

ADD start.sh /start.sh

# Define working directory.
WORKDIR /etc/nginx

# Define default command.
CMD sh /start.sh

# Expose ports.
EXPOSE 80
EXPOSE 443