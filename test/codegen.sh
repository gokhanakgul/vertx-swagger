#!/usr/bin/env bash

export PROJECT_OUTPUT="../sample/profilem"
export DEFINITION_PATH="swagger-profile.yml"

export GROUP_ID="com.optimo"
export ARTIFACT_ID="profile"



java -cp swagger-codegen-cli-2.2.2.jar:vertx-swagger-codegen.jar io.swagger.codegen.SwaggerCodegen generate \
  -l java-vertx \
  -o ${PROJECT_OUTPUT} \
  -i ${DEFINITION_PATH} \
  --group-id ${GROUP_ID} \
  --artifact-id ${ARTIFACT_ID} \
  -DjdbcPersistence=true \
  -v