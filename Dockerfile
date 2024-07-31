FROM openjdk:8
VOLUME /tmp
EXPOSE 8443
ENV TZ America/Los_Angeles
ADD target/college-adaptor-*.jar app.jar
ADD entrypoint.sh .
RUN chmod 755 ./entrypoint.sh
COPY api /api
COPY groovy-scripts /groovy-scripts
COPY jars /jars
RUN bash -c 'touch /app.jar'

#add properties for miscode to support local dev use-case
ARG miscode
ENV miscode ${miscode:-001}
RUN ln -sf /usr/bin/python3 /usr/bin/python
RUN apt update && apt -y install python3-venv

ENTRYPOINT ["./entrypoint.sh", "java -Djasypt.encryptor.password=${JASYPT_PASSWORD} -jar /app.jar -DsisType=${SISTYPE} -DSPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"]
