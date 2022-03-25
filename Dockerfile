FROM clojure:openjdk-17-lein-alpine

WORKDIR /usr/src/app

RUN apk add --no-cache git

RUN ln -s "/opt/openjdk-17/bin/java" "/bin/monkey"

ENV OTEL_TRACES_EXPORTER none

COPY project.clj /usr/src/app/
RUN lein deps

COPY conf/main/logback.xml /usr/src/app/
COPY . /usr/src/app

RUN lein uberjar && \
    cp target/monkey-standalone.jar .

ENTRYPOINT ["monkey", "-Dlogback.configurationFile=/etc/iplant/de/logging/monkey-logging.xml", "-javaagent:/usr/src/app/opentelemetry-javaagent.jar", "-Dotel.resource.attributes=service.name=monkey", "-cp", ".:monkey-standalone.jar", "monkey.core"]
CMD ["--help"]

ARG git_commit=unknown
ARG version=unknown
ARG descriptive_version=unknown

LABEL org.cyverse.git-ref="$git_commit"
LABEL org.cyverse.version="$version"
LABEL org.cyverse.descriptive-version="$descriptive_version"
LABEL org.label-schema.vcs-ref="$git_commit"
LABEL org.label-schema.vcs-url="https://github.com/cyverse-de/monkey"
LABEL org.label-schema.version="$descriptive_version"
