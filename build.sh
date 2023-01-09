#!/bin/bash
PROJECT_NAME=$1
ENV=$2

if [ "$#" -ne 2 ]; then
    echo "Usage: ./build.sh <project_name> <env>"
    echo "Example1: ./docker_buildx.sh captions-converter dev"
    echo "Example2: ./docker_buildx.sh captions-ingester prod"
    exit 1
fi

#if [ ! -d "$PROJECT_NAME" ]; then
#  echo "$PROJECT_NAME does not exist. Can't find directory.."
#  exit 1
#fi


if [ -z "${ENV}" ]; then
ENV=dev
fi

# pushd $PROJECT_NAME

PROJECT_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout)
IMAGE=''
if [ $ENV == "prod" ]; then
IMAGE="us.gcr.io/xumo-prod/${PROJECT_NAME}:${PROJECT_VERSION}"
else
IMAGE="us.gcr.io/xumo-ovp-ng/${PROJECT_NAME}:${PROJECT_VERSION}"
fi

PLATFORMS="linux/arm64,linux/amd64"
echo "Building Image ${IMAGE} for $PROJECT_NAME, for platforms $PLATFORMS"
mvn clean package -Ddockerfile.skip
docker buildx create --name builder_java
docker buildx use builder_java
# docker buildx inspect --bootstrap
docker buildx build --platform ${PLATFORMS} -t ${IMAGE} --push .
docker buildx stop builder_java

# popd