FROM openjdk:8
# FROM openjdk:8-jre-alpine
MAINTAINER jayxumo

ADD target/app-jar-with-dependencies.jar /app-jar-with-dependencies.jar

ENTRYPOINT exec java $JAVA_OPTS -jar /app-jar-with-dependencies.jar

