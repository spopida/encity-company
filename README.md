# encity-company

## Essentials

### Configuration

Configuration of this microservice is done in two places:
1. Within `src/resources/application.properties` to avoid hard-coding of global values such as the name of the service.  I should consider retiring this - I'm not sure it's necessary.  Could just use constants?  This file is source-controlled
2. Outside of the application in a place specified by the run-time `spring.config.location` system property.  This file is *NOT* source-controlled as it contains deployment-specific properties, such as the server port, logging levels and so on; some of these are sensitive to leakage and should be protected (e.g. application keys and passwords).  In a local development environment, `spring.config.location` is specified somewhere in the IDE, (In IntelliJ this is done in *Edit Configuraitions*).  As an example, it could be set to `${HOME}/config/encity/company-service.properties`.  For non-IDE environments as script will have to be provided, which references a soft-coded location via an environment varible that is set externally.