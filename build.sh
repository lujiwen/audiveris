docker run -it   --name build-audiveris \
-v /root/audiveris:/root/audiveris \
-w="/root/audiveris" \
openjdk:8-jdk-alpine  \
./gradlew build