FROM gradle:jdk17-alpine

WORKDIR /app

COPY . .

ENTRYPOINT gradle bootRun