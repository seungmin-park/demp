echo ${SECURE_FILE} >> env.json
sed 's/*/"/g' env.json >> secureFile.json
java -Dserver.port=$PORT $JAVA_OPTS -Dspring.profiles.active=prod -jar ./build/libs/demp-0.0.1-SNAPSHOT.jar