FROM amazoncorretto:22 AS runtime
EXPOSE 8080
RUN mkdir /app
COPY ./server/website/build/libs/*.jar /app/app.jar
#COPY ./composeApp/build/dist/wasmJs/productionExecutable/*.* /app/website/ TODO: Uncomment when building frontend is supported
ENTRYPOINT ["java","-jar","/app/app.jar"]