ARG VERSION_TOMCAT=9.0

FROM tomcat:${VERSION_TOMCAT}
ARG WAR_FILENAME
COPY ${WAR_FILENAME} /usr/local/tomcat/webapps/WebMIaS.war
