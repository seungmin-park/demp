echo ${SECURE_FILE} >> env.json
sed 's/*/"/g' env.json >> secureFile.json
qoddi buildpacks:set https://github.com/qoddi/qoddi-buildpack-gradle#previous-version
java -Dserver.port=$PORT $JAVA_OPTS -Dspring.profiles.active=prod -jar ./build/libs/demp-0.0.1-SNAPSHOT.jar