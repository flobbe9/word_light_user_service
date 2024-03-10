# word_light_user_service
REST service handling user related logic for the Word light API. Uses Java 17 with Spring Boot.

# Run 
### Locally
```docker-compose -f docker-compose.local.yml up``` <br>
Call this inside project root folder of repository inside dev or stage branch. <br>

### Dockerhub
```docker-compose up``` <br>
Call this with .env file in same folder as docker-compose.yml. <br>

### The whole thing
```docker-compose -f docker-compose.all.yml up``` <br>
Call this with .env file in same folder as docker-compose.all.yml file. <br>
Will start the whole microservice including frontend etc. using images from Dockerhub. No further configuration needed. Access api at https://localhost

# More documentation
Run api, then visit http://localhost:4002 or https://localhost:4002 (if run on stage branch)