FROM amazoncorretto:22 AS runtime
EXPOSE 8080
RUN mkdir /app
COPY ./service/gateway/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]