package io.akka.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
@Profile("production")
public class Infrastructure {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
