package com.docutools.dockersecretprocessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Add a new {@link org.springframework.core.env.PropertySource} for docker secrets stored in a directory, its path
 * specified in the {@link DockerSecretProcessor#BIND_PATH_PROP_NAME} property.
 *
 * <p>If you have a docker secret mounted to the file "db-password" it will be defined as "docker-secret-db-password"
 * property. If you want to use it as a value for "spring.datasource.password" you can use the following snippet in
 * your application.yml:
 *
 * <code>
 * spring.datasource.password: ${docker-secret-pw-password}
 * </code>
 *
 * <b>Attention:</b> Due to docker seemingly always adding a line break after each secret, this class trims every
 * property value.
 *
 * @author amp
 * @since 2021-10-12
 */
public class DockerSecretProcessor implements EnvironmentPostProcessor {
  private static final Logger log = LogManager.getLogger();

  private static final String BIND_PATH_PROP_NAME = "docker.secret.bind-path";

  private static String buildPropertyName(Path path) {
    var secretName = "docker-secret-" + path.getFileName();
    log.info("DockerSecretProcessor: Registered docker secret " + secretName);
    return secretName;
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    var bindPathProp = environment.getProperty(BIND_PATH_PROP_NAME);
    log.info("DockerSecretProcessor: value of \"docker.secret.bind-path\" property:" + bindPathProp);
    if (bindPathProp == null) {
      return;
    }

    Path bindPath = Paths.get(bindPathProp);
    if (!Files.isDirectory(bindPath)) {
      log.error("exiting DockerSecretProcessor: \"docker.secret.bind-path\"={} is not a directory%n", bindPathProp);
    }

    try {
      Map<String, Object> dockerSecrets = Files.list(bindPath).collect(Collectors.toMap(DockerSecretProcessor::buildPropertyName, path -> {
        try {
          return Files.readString(path).trim();
        } catch (IOException e) {
          log.error(e);
          throw new RuntimeException(e);
        }
      }));
      environment.getPropertySources().addLast(new MapPropertySource("docker-secrets", dockerSecrets));
    } catch (IOException e) {
      log.error("Failed to load docker secrets.", e);
    }
  }
}