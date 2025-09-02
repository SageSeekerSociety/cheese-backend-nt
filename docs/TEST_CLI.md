# Test CLI Usage Guide

## Purpose
Quickly create test data in the development database using test clients as CLI commands.

## How It Works
- The `CliTest` class contains various test methods that can be run individually
- Test methods are marked with `@EnabledIfSystemProperty(named = "cli", matches = "true")` to prevent accidental execution
- They only run when `-Dcli=true` is passed
- Using Maven to run specific test methods (`-Dtest=CliTest#methodName`) acts as executing a CLI command
- By default uses the `dev` profile (development database on port 5432)

## Quick Start

### Prerequisites
1. Start PostgreSQL and other services:
   ```bash
   docker-compose up -d
   ```
   
2. The development database is configured at:
   - URL: `jdbc:postgresql://localhost:5432/postgres`
   - Username: `postgres`
   - Password: `postgres`

### Basic Commands

#### Create a user and get token
```bash
./mvnw test -Dtest=CliTest#createUser -Dcli=true -Dspotless.check.skip=true
```

#### Create multiple users
```bash
# Create 5 users (default is 3)
./mvnw test -Dtest=CliTest#createMultipleUsers -Dcli=true -Dcli.count=5 -Dspotless.check.skip=true
```

#### Create a space with category
```bash
./mvnw test -Dtest=CliTest#createSpace -Dcli=true -Dspotless.check.skip=true
```

#### Create a team with members
```bash
./mvnw test -Dtest=CliTest#createTeam -Dcli=true -Dspotless.check.skip=true
```

#### Create full test environment
```bash
# Creates users, space, team, and task
./mvnw test -Dtest=CliTest#createFullEnvironment -Dcli=true -Dspotless.check.skip=true
```

#### Login with existing credentials
```bash
./mvnw test -Dtest=CliTest#loginWithCredentials -Dcli=true -Dcli.username=xxx -Dcli.password=yyy -Dspotless.check.skip=true
```

## Available Commands

### CliTest Methods
- `createUser` - Creates a single user and prints credentials + JWT token
- `createMultipleUsers` - Creates multiple users (use `-Dcli.count=N`)
- `createSpace` - Creates a space with owner and category
- `createTeam` - Creates a team with owner and members
- `createFullEnvironment` - Creates complete test setup (users, space, team, task)
- `loginWithCredentials` - Login with provided credentials (use `-Dcli.username=xxx -Dcli.password=xxx`)

## Optional Parameters
- `-Dcli.count=N` - Number of users to create (for createMultipleUsers)
- `-Dcli.username=xxx` - Username for login
- `-Dcli.password=xxx` - Password for login
- `-Dspotless.check.skip=true` - Skip code formatting checks (recommended for faster execution)

## Output Examples

### Create User Output
```
========================================
User created successfully!
========================================
User ID: 423
Username: NTTestUsername-577739724
Password: abc123456!!!
Email: test-8037404650@ruc.edu.cn
========================================
JWT Token:
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
========================================
```

### Create Full Environment Output
```
========================================
Full test environment created!
========================================
SPACE:
  ID: 1
  Name: Test Space 123456
  Category ID: 1
========================================
TEAM:
  ID: 1
  Name: Test Team 789012
========================================
TASK:
  ID: 1
  Title: Test Task 345678
========================================
USERS:
Owner:
  Username: NTTestUsername-111111
  Password: abc123456!!!
  Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Member 1:
  Username: NTTestUsername-222222
  Password: abc123456!!!
Member 2:
  Username: NTTestUsername-333333
  Password: abc123456!!!
========================================
```

## Tips
- The created users have long usernames (e.g., `NTTestUsername-577739724`) to avoid conflicts
- All users have the same default password: `abc123456!!!`
- Tokens are JWT tokens that can be used directly in API requests
- Data is created in the development database (port 5432) by default

## Architecture
- **CliTest** - Single entry point for all CLI commands
- **Client Classes** (UserClient, SpaceClient, TeamClient, TaskClient) - Reusable business logic
- **Spring Boot Test** - Provides dependency injection and database connections