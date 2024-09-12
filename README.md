# cheese-backend-nt
Welcome to the cheese-backend-nt project!

## Environment
You need to set up several things before you can compile and run this project.

### Prerequisites
To get started, you need to install JDK 21. Make sure you have the correct version by running ```java -version``` and
```javac -version```. Incorrect version will result in all sorts of errors.

You also need to set up several services. We recommend using Docker to run these services. Make sure you have installed
Docker and run ```doc/script/dependency-start.sh``` and ```doc/script/dependency-restart.sh```
in Unix shell or the bash in Docker Desktop to start the services. (You may need to remove ```sudo``` and run the commands
one by one manually if you want to do it on Windows in Docker Desktop)

### Build
To build the project, run ```./mvnw install``` in Unix shell or PowerShell. This will generate API interfaces from the
OpenAPI specification in ```design/API/NT-API.yml```, compile the project, and run tests.

### Run
After the previous step, you will find the jar file in the ```target``` directory. Run
```java -jar ./target/cheese-0.0.1-SNAPSHOT.jar``` (replace the jar file name with the actual one) to run this project.

### Format
To format the code, run ```./mvnw spotless:apply``` in Unix shell or PowerShell.

## Database Migration

### Test
You do not need to migrate the database manually during testing. In ```pom.xml```, we set ```spring.jpa.hibernate.ddl-auto```
to ```update``` when Maven is running tests. This means that Hibernate will automatically create tables and columns in
the database.

### Production
In production, you need to migrate the database manually. Our build system will generate ```design/DB/CREATE.sql``` each
time you run ```./mvnw install```. This file is added to git, so you can see how the schema changes.
