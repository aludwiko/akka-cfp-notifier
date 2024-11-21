package io.akka;


import akka.javasdk.DependencyProvider;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.javasdk.client.ComponentClient;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;

@Setup
public class CfpSetup implements ServiceSetup {

  private static final Logger logger = LoggerFactory.getLogger(CfpSetup.class);

  private final ComponentClient componentClient;
  private final Config config;

  public CfpSetup(ComponentClient componentClient, Config config) { // <2>
    this.componentClient = componentClient;
    this.config = config;
  }

  @Override
  public DependencyProvider createDependencyProvider() {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(); // <1>
      ResourcePropertySource resourcePropertySource = new ResourcePropertySource(new ClassPathResource("application.properties"));
      context.getEnvironment().getPropertySources().addFirst(resourcePropertySource);
      context.registerBean(ComponentClient.class, () -> componentClient);
      context.registerBean(Config.class, () -> config);
      context.scan("io.akka");
      context.refresh();
      return context::getBean; // <2>
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}