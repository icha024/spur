FROM java:8
ADD gauth-totp-web.jar gauth-totp-web.jar
EXPOSE 8080
ENTRYPOINT ["java","-DPORT=8080","-jar","gauth-totp-web.jar"]
