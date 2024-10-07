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

Sometimes, auto update may not work as expected. In this case, you should use `doc/script/dependency-delete.sh` to delete
your dependencies completely, and use `doc/script/dependency-start.sh` to start them again. Then, things should be fine.

### Production
In production, you need to migrate the database manually. In [design/DB/MIGRATION](design/DB/MIGRATION) directory, you can find SQL files that
migrate the database between released versions. But be careful, you need to figure out your current version precisely
and use the correct SQL files. You may need to execute several SQL files in order.

## Development

### About IDE

IntelliJ IDEA is the best Java/Kotlin IDE, and we recommend using it. Simply open the project in IntelliJ IDEA and it
works well without any additional configuration.

However, IDEA is not free. If you want a free alternative, VSCode is absolutely the best choice. To save your time from
configuring the mess of plugins and compatibility issues on Windows, we offer a DevContainer configuration for you. Just
install Docker (or Docker Desktop on Windows), install VSCode extension `ms-vscode-remote.remote-containers`, and open
the folder of this project in VSCode's dev container mode.

If you think container is still too heavy, you can install VSCode extensions according to [.devcontainer/devcontainer.json](.devcontainer/devcontainer.json).

Remember, IDE won't help you start dependencies. You still need to use `doc/script/dependency-start.sh` to start
the services.

### Valuable Experience

Fixing bugs can be time consuming. Luckily, we have learned some valuable experience from
that disgusting process, which can be found in [doc/experience.md](doc/experience.md). I hope they are helpful to you.
