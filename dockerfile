#FROM redis/redis-stack:latest
#
#RUN mkdir -p /usr/local/etc/redis && \
#    echo "\
#requirepass systempass\n\
#appendonly yes\n\
#save 60 1000\n\
#loglevel notice\n\
#timeout 300\n\
#databases 1\n\
#maxmemory 256mb\n\
#maxmemory-policy allkeys-lru\n\
#" > /usr/local/etc/redis/redis-stack.conf
#
#CMD ["redis-stack-server", "/usr/local/etc/redis/redis-stack.conf"]