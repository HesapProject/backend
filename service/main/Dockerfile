FROM eclipse-temurin:21-jre

ARG version=undefined
ENV VERSION=${version} \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 \
    -Djava.awt.headless=true \
    -Dsun.net.inetaddr.ttl=10 \
    -Djava.net.preferIPv4Stack=true \
    -XX:+AlwaysActAsServerClassMachine \
    -XX:+PerfDisableSharedMem \
    -XX:+UseStringDeduplication \
    -XX:+UseNUMA \
    -XX:+AlwaysPreTouch \
    -Dio.netty.leakDetectionLevel=simple \
    -XX:+ExplicitGCInvokesConcurrent \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Xmx4g"

LABEL version=${VERSION}

COPY service/main/build/libs/main-$VERSION.jar /main.jar

EXPOSE 8001
EXPOSE 8081

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /main.jar" ]
