package io.akka.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static io.akka.application.DurationCalculator.calculateDuration;
import static org.assertj.core.api.Assertions.assertThat;

class DurationCalculatorTest {

  Clock clock = Clock.systemUTC();

  @Test
  public void shouldCalculateDuration() {
    //given
    LocalDate deadline = LocalDate.parse("2024-03-25");
    Instant expectedNotificationDateBefore7Days = Instant.parse("2024-03-18T01:00:00.00Z");
    Instant expectedNotificationDateBefore1Day = Instant.parse("2024-03-24T01:00:00.00Z");

    //when
    Instant now1 = Instant.parse("2024-01-20T10:12:12.00Z");
    Duration duration17 = calculateDuration(now1, 7, deadline, clock);
    Duration duration11 = calculateDuration(now1, 1, deadline, clock);
    Instant now2 = Instant.parse("2024-01-20T23:59:12.00Z");
    Duration duration27 = calculateDuration(now2, 7, deadline, clock);
    Duration duration21 = calculateDuration(now2, 1, deadline, clock);
    Instant now3 = Instant.parse("2024-01-20T00:12:12.00Z");
    Duration duration37 = calculateDuration(now3, 7, deadline, clock);
    Duration duration31 = calculateDuration(now3, 1, deadline, clock);

    //then
    assertThat(now1.plusSeconds(duration17.toSeconds()).truncatedTo(ChronoUnit.HOURS)).isEqualTo(expectedNotificationDateBefore7Days);
    assertThat(now2.plusSeconds(duration27.toSeconds()).truncatedTo(ChronoUnit.HOURS)).isEqualTo(expectedNotificationDateBefore7Days);
    assertThat(now3.plusSeconds(duration37.toSeconds()).truncatedTo(ChronoUnit.HOURS)).isEqualTo(expectedNotificationDateBefore7Days);
    assertThat(now1.plusSeconds(duration11.toSeconds()).truncatedTo(ChronoUnit.HOURS)).isEqualTo(expectedNotificationDateBefore1Day);
    assertThat(now2.plusSeconds(duration21.toSeconds()).truncatedTo(ChronoUnit.HOURS)).isEqualTo(expectedNotificationDateBefore1Day);
    assertThat(now3.plusSeconds(duration31.toSeconds()).truncatedTo(ChronoUnit.HOURS)).isEqualTo(expectedNotificationDateBefore1Day);
  }
}