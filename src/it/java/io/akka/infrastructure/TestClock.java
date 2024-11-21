package io.akka.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class TestClock extends Clock {

  private Instant now;

  public TestClock(Instant now) {
    this.now = now;
  }

  public void setNow(Instant now) {
    this.now = now;
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return null;
  }

  @Override
  public Instant instant() {
    return now;
  }
}
