package io.akka.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.DeleteHandler;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.typesafe.config.Config;
import io.akka.domain.CallForPaper;
import io.akka.domain.CallForPaperReminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.akka.application.DurationCalculator.calculateDuration;
import static java.util.Arrays.stream;


@ComponentId("schedule-notification")
@Consume.FromKeyValueEntity(CallForPaperEntity.class)
public class ScheduleNotification extends Consumer {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Config config;
  private final Clock clock;
  private final ComponentClient componentClient;

  public ScheduleNotification(Config config, Clock clock, ComponentClient componentClient) {
    this.config = config;
    this.clock = clock;
    this.componentClient = componentClient;
  }

  public Effect onChange(CallForPaper callForPaper) {
    Instant now = clock.instant();
    LocalDate today = LocalDate.ofInstant(now, clock.getZone());
    List<Integer> intervals = getIntervals();
    List<Integer> applicableIntervals = intervals.stream().filter(applicableIntervals(callForPaper, today)).toList();
    logger.debug("Scheduling notifications for cfp: {} at intervals: {}, all intervals: {}", callForPaper, applicableIntervals, intervals);
    List<CompletionStage<Done>> timersSchedules = applicableIntervals.stream().map(howManyDaysBefore -> {
      logger.info("Scheduling notification for cfp: {} {} days before {}", callForPaper, howManyDaysBefore, callForPaper.deadline());
      return timers().startSingleTimer(
        timerName(howManyDaysBefore, callForPaper.id()),
        calculateDuration(now, howManyDaysBefore, callForPaper.deadline(), clock),
        componentClient.forTimedAction().method(Notify::runNotification).deferred(CallForPaperReminder.of(callForPaper, howManyDaysBefore)));
    }).toList();

    return effects().asyncDone(
      CompletableFuture.allOf(timersSchedules.toArray(new CompletableFuture<?>[0]))
        .thenApply(__ -> Done.getInstance())
    );
  }

  @DeleteHandler
  public Effect onDelete() {
    return effects().asyncDone(messageContext().metadata().asCloudEvent().subject().map(cfpId -> {
      logger.info("Deleting scheduled notifications for cfp: {}", cfpId);
      return timers().cancel(timerName(7, cfpId)).thenCompose(__ ->
        timers().cancel(timerName(1, cfpId)));
    }).orElse(CompletableFuture.completedStage(Done.getInstance())));
  }

  private static String timerName(Integer interval, String cfpId) {
    return "notifyAboutCfp-" + cfpId + "-" + interval;
  }

  private Predicate<Integer> applicableIntervals(CallForPaper callForPaper, LocalDate today) {
    return interval -> callForPaper.deadline().minusDays(interval).isAfter(today);
  }

  private List<Integer> getIntervals() {
    String intervalsStr = config.getString("cfp.notifier.notification-intervals");
    if (intervalsStr == null || intervalsStr.isEmpty()) {
      return List.of(7, 1);
    } else {
      return stream(intervalsStr.split(",")).map(Integer::parseInt).toList();
    }
  }
}
