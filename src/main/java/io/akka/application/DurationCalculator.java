package io.akka.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DurationCalculator {

  public static Duration calculateDuration(Instant today, Integer howManyDaysBefore, LocalDate deadline, Clock clock) {
    var notificationAt = deadline.minusDays(howManyDaysBefore).atStartOfDay(clock.getZone()).plusHours(1).toInstant();

    Duration duration = Duration.between(today.truncatedTo(ChronoUnit.HOURS), notificationAt);
    if (duration.isPositive()) {
      return duration;
    } else {
      throw new IllegalArgumentException("Notification date is in the past, for howManyDaysBefore: " + howManyDaysBefore + " and deadline: " + deadline + " and today: " + today + " and notificationAt: " + notificationAt);
    }
  }
}
