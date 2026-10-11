FROM eclipse-temurin:21-jre-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app \
    && mkdir -p /opt/app/upload \
    && chown -R app:app /opt/app

WORKDIR /opt/app
COPY --chown=app:app --chmod=0444 app.jar /opt/app/app.jar
USER 10001:10001

EXPOSE 8888
HEALTHCHECK --interval=30s --timeout=8s --start-period=90s --retries=3 \
    CMD curl --fail --silent --show-error --max-time 5 http://127.0.0.1:8888/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]
