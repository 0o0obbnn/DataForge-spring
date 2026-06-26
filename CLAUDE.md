# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DataForge is a high-performance, flexible, and configurable test data generation tool built with Spring Boot. It consists of three main modules:

- **data-forge-core**: Core engine with 60+ data generators and SPI framework
- **data-forge-cli**: Command-line interface using Picocli
- **data-forge-web**: Spring Boot web application with REST API
- **data-forge-frontend**: React 19 + TypeScript frontend with Vite

## Development Commands

### Build and Test
```bash
# Full build with tests
./build.sh

# Quick build (Windows)
./build-quick.bat

# Run tests only
mvn test

# Run integration tests
mvn failsafe:integration-test

# Skip tests during build
mvn package -DskipTests
```

### Quality Assurance
```bash
# Run all quality checks (Checkstyle, PMD, SpotBugs, JaCoCo)
./quality-check.sh

# Skip tests in quality check
./quality-check.sh --skip-tests

# Generate quality reports only
./quality-check.sh --report-only

# Run with Maven profile
mvn verify -Pquality
```

### Web Application
```bash
# Start web server (Linux/macOS)
./run-web.sh

# Start web server (Windows)  
./run-web.bat

# Manual start
cd data-forge-web && mvn spring-boot:run

# Access at: http://localhost:8080/dataforge
```

### Frontend Development
```bash
cd data-forge-frontend

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Run tests
npm run test:unit

# Lint and format
npm run lint
npm run format
```

### CLI Usage
```bash
# Run examples
./run-example.sh

# Generate data directly
java -jar data-forge-cli/target/data-forge-cli-1.0.0-SNAPSHOT.jar \
  --count 10 --format csv --output data.csv --fields "id:uuid,name:name"

# Use config file
java -jar data-forge-cli/target/data-forge-cli-1.0.0-SNAPSHOT.jar \
  --config examples/basic-config.yml
```

## Architecture Overview

### Core Module Structure
- **generators/**: 60+ data generators implementing SPI pattern
- **spi/**: Service Provider Interface for extensible generator framework  
- **core/**: Generation engine, context management, caching
- **io/**: Output strategies (Console, CSV, JSON, SQL, File)
- **config/**: Configuration management and validation
- **validation/**: Data validation (ID cards, credit cards, etc.)

### Generator Extension Pattern
All generators implement `DataGenerator<T, C extends FieldConfig>`:
- Type-safe with generic constraints
- SPI-based registration via `META-INF/services`
- Configuration validation and context-aware generation
- Examples: `UuidGenerator`, `NameGenerator`, `PhoneGenerator`

### Web API Architecture
- REST endpoints at `/api/v1/dataforge/`
- DTO pattern with assemblers for clean separation
- Async task processing with rate limiting
- OpenAPI/Swagger documentation
- Global exception handling

### Configuration System
- YAML-based configuration with validation
- Support for field relationships and context sharing
- Multiple output formats and encoding options
- Thread-safe concurrent generation

## Key Technical Details

### Java Version & Dependencies
- **Java 17** (required)
- **Spring Boot 3.2.1** with auto-configuration
- **Maven 3.6+** with multi-module structure
- **Picocli 4.7.5** for CLI interface

### Testing Strategy
- **Unit tests**: `*Test.java` with Surefire plugin
- **Integration tests**: `*IntegrationTest.java`, `*IT.java` with Failsafe plugin
- **Coverage target**: 70% line, 65% branch with JaCoCo
- **Test profiles**: `test`, `integration-test`

### Code Quality Tools
- **Checkstyle**: Java coding standards enforcement
- **PMD**: Code quality and best practices
- **SpotBugs**: Static analysis with FindSecBugs security plugin  
- **JaCoCo**: Test coverage analysis
- All configured with Maven profiles `quality` and `quality-report`

### Frontend Technology Stack
- **React 19** with Hooks and TypeScript
- **Vite** build tool with Hot Module Replacement

### Performance Considerations
- Multi-threaded generation with configurable thread pools
- Memory-efficient streaming for large datasets
- Caching with Caffeine for generator instances
- Async processing for web requests
- Backpressure handling for resource management

## Configuration Examples

### Basic CLI Generation
```bash
# Simple field generation
--fields "id:uuid,name:name,email:email"

# With parameters
--fields "id:uuid,name:name[type=CN,gender=MALE],phone:phone"
```

### YAML Configuration Structure
```yaml
dataforge:
  count: 1000
  threads: 4
  validate: true
  
  output:
    format: csv
    file: "data.csv"
    encoding: "UTF-8"
    
  fields:
    - name: "id"
      type: "uuid"
    - name: "name"  
      type: "name"
      params:
        type: "CN"
        gender: "ANY"
```

## Common Development Tasks

### Adding New Generator
1. Implement `DataGenerator<T, YourConfig>` interface
2. Create configuration class extending `FieldConfig`
3. Register in `META-INF/services/com.dataforge.generators.spi.DataGenerator`
4. Add unit tests following naming pattern
5. Update documentation

### Running Single Test Class
```bash
mvn test -Dtest=GeneratorFactoryTest
mvn test -Dtest=*IntegrationTest
```

### Debugging Web Application
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Frontend Development
```bash
# Hot reload development
cd data-forge-frontend && npm run dev

# Component testing
npm run test:unit -- --watch

# Type checking
npm run type-check
```

## Project File Structure

Key directories to understand:
- `data-forge-core/src/main/java/com/dataforge/generators/internal/`: All 60+ generators
- `data-forge-web/src/main/java/com/dataforge/web/controller/`: REST API controllers  
- `data-forge-frontend/src/components/`: React components
- `examples/`: Configuration file examples for all generator types
- `output/`: Generated data files (gitignored)

## Environment Requirements

- **JDK 17+** (enforced by Maven)
- **Maven 3.6+** (enforced by Maven Enforcer Plugin)
- **Node.js 16+** (for frontend development)
- **Memory**: 2GB+ recommended for large dataset generation