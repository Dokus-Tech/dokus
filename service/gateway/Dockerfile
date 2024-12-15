FROM amazoncorretto:22 AS runtime
EXPOSE 8080
RUN mkdir /app
COPY ./build/libs/*.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]