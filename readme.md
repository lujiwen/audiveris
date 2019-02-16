# compilation environment requirement 

java8, if java version is higher, downgrade to version 8

on macos: 
list all available jdk :
``/usr/libexec/java_home -V``

and reset $JAVA_HOME to java 8

# how to deploy audiveris wrapper


cd /root/audiveris

git pull 


docker run -it   --name build-audiveris \
-v /root/audiveris:/root/audiveris \
-v /root/.gradle/:/root/.gradle/ \
-w="/root/audiveris" \
openjdk:8-jdk-alpine  \
./gradlew build

# how to run audiveris wrapper 

java -jar audiveris-wrapper/build/audiveries-wrapper.jar & 

the jar can be scp to remote host from a local artifact. but can not be build now on remote host, may limited by memory quota


