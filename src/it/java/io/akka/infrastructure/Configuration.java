package io.akka.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@org.springframework.context.annotation.Configuration
@Profile("it-test")
public class Configuration {

  @Bean
  public Clock clock() {
    return new TestClock(Clock.systemUTC().instant());
  }
}
