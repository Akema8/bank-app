#!/bin/sh
set -e
ZK="${ZK_HOST:-zookeeper:2181}"

echo "Waiting for ZooKeeper at $ZK..."
until zkCli.sh -server "$ZK" ls / >/dev/null 2>&1; do
    sleep 2
done
echo "ZooKeeper is ready."

echo "Seeding /config..."
zkCli.sh -server "$ZK" <<EOF
deleteall /config
create /config ""
create /config/application ""
create /config/cash ""
create /config/auth-server ""
create /config/bank-web ""

create /config/application/eureka.client.service-url.defaultZone "http://eureka-server:8761/eureka/"
create /config/application/spring.security.oauth2.resourceserver.jwt.jwk-set-uri "http://auth-server:9000/oauth2/jwks"

create /config/cash/spring.security.oauth2.client.provider.cash-client.token-uri "http://auth-server:9000/oauth2/token"

create /config/auth-server/auth.issuer-uri "http://localhost:9000"
create /config/auth-server/bank-web.redirect-uri "http://localhost:8080/login/oauth2/code/bank-web"

create /config/bank-web/spring.security.oauth2.client.provider.bank-web.authorization-uri "http://localhost:9000/oauth2/authorize"
create /config/bank-web/spring.security.oauth2.client.provider.bank-web.token-uri "http://auth-server:9000/oauth2/token"
create /config/bank-web/spring.security.oauth2.client.provider.bank-web.jwk-set-uri "http://auth-server:9000/oauth2/jwks"
create /config/bank-web/spring.security.oauth2.client.provider.bank-web.user-info-uri "http://auth-server:9000/userinfo"
create /config/bank-web/gateway.url "http://gateway:8090"
create /config/bank-web/auth.url "http://auth-server:9000"

quit
EOF

echo "Done."
