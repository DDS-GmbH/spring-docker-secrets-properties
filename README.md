# Docker Secret Processor
The Docker Secret Processor can be included into a project to load docker
secrets into spring boot application config files.

To use it, add it in your package management and set 
```properties
org.springframework.boot.env.EnvironmentPostProcessor=hung.org.DockerSecretProcessor
```

Secrets have to be referenced with the prefix `docker-secret-`, so an example file should
look like this:

```yaml
db:
  user: test
  secret: ${docker-secret-db-password}
```

Designed with reference to [kwonghung-YIP/spring-boot-docker-secret](https://github.com/kwonghung-YIP/spring-boot-docker-secret)