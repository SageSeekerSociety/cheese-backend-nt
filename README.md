# cheese-backend-nt
Welcome to the cheese-backend-nt project!

## Environment
You need to set up several things before you can compile and run this project.

### Prerequisites
To get started, you need to install JDK 21. Make sure you have the correct version by running ```java -version``` and
```javac -version```. Incorrect version will result in all sorts of errors.

You also need to set up PostgreSQL and ElasticSearch. We recommend using Docker to run these services. Run the following
scripts in Unix shell or the bash in Docker Desktop to start the services:
```bash
#!/bin/sh
sudo systemctl start docker.service

sudo docker run -d \
    --name elasticsearch \
    -e discovery.type=single-node \
    -e xpack.security.enabled=true \
    -e ELASTIC_USERNAME=elastic \
    -e ELASTIC_PASSWORD=elastic \
    --health-cmd="curl http://localhost:9200/_cluster/health" \
    --health-interval=10s \
    --health-timeout=5s \
    --health-retries=10 \
    -p 9200:9200 \
    docker.elastic.co/elasticsearch/elasticsearch:8.12.1

sudo docker run -d \
    --name postgres \
    -e POSTGRES_PASSWORD=postgres \
    --health-cmd="pg_isready" \
    --health-interval=10s \
    --health-timeout=5s \
    --health-retries=5 \
    -p 5432:5432 \
    postgres
echo "Wait for 5 seconds please..."
sleep 5
sudo docker exec -i postgres bash << EOF
    sed -i -e 's/max_connections = 100/max_connections = 1000/' /var/lib/postgresql/data/postgresql.conf
    sed -i -e 's/shared_buffers = 128MB/shared_buffers = 2GB/' /var/lib/postgresql/data/postgresql.conf
EOF
sudo docker restart --time 0 postgres
```

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
