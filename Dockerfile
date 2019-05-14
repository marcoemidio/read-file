
FROM openjdk:8-jdk-alpine
VOLUME /tmp
#RUN apk add --update openssl && \
#    rm -rf /var/cache/apk/*
RUN apk add --update ttf-dejavu && rm -rf /var/cache/apk/*
ARG JAVA_OPTS
ENV JAVA_OPTS=$JAVA_OPTS
ENV KEYSTORE_PATH=/opt/conf/edpccikeyvault-signature-pkcs12-20190412.pfx
ADD target/read-file-0.0.1-SNAPSHOT.jar read-file.jar
COPY src/main/resources/edpccikeyvault-signature-pkcs12-20190412.pfx /opt/conf/
#COPY libs/jPDFProcess.v2018R1.10.jar /opt/libs/
EXPOSE 8080
ENTRYPOINT exec java $JAVA_OPTS -jar read-file.jar
# For Spring-Boot project, use the entrypoint below to reduce Tomcat startup time.
#ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar read-file.jar
