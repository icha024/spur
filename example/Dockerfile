#FROM java:8
FROM ofayau/openjdk:8-compact2
ADD demo.jar demo.jar
EXPOSE 8080
EXPOSE 8443
ENTRYPOINT ["java","-DPORT=8080","-jar","demo.jar"]
