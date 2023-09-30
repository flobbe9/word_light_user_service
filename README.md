# vorspiel_userservice
REST service handling user related logic for the vorspiel API. Uses Java 17 with Spring Boot and MySQL.

## Run
1. Database and Mailserver: ```docker-compose -f 'docker-compose.dev.yml' up -d```<br>
2. Api: ```docker-compose up``` <br>

The Api depends on the database and the mailserver (MySQL and maildev docker images) and may not work properly if step 1 is skipped.

## Endpoint documentation
<!-- TODO: add prod version when going live -->
http://localhost:4002/swagger-ui/index.html#/