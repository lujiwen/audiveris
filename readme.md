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
``
scp /Users/lujiwen/go/src/audiveris-wrapper/build/libs/audiveries-wrapper-0.1.0.jar root@aliyun:/root/audiveris-wrapper/build/audiveries-wrapper.jar

``
http://47.98.138.29/


# troubleshooting

- java.lang.ClassNotFoundException: javax.xml.bind.JAXBException

reset java home to java 8 

- Could not initialize class de.intarsys.cwt.freetype.Freetype



``
Audiveris software is coded in Java but uses some external binaries (Leptonica and Tesseract today, plus certainly ND4J tomorrow). Mind the fact that, because of these binaries, the JRE and installer architectures must match. Otherwise Audiveris will fail to run, with error messages saying for example that jnilept library cannot be loaded.
To check your existing java environment, you can use the command: java -version from a terminal window.
``