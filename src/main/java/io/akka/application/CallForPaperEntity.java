package io.akka.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import io.akka.domain.CallForPaper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;


@ComponentId("call-for-paper")
public class CallForPaperEntity extends KeyValueEntity<CallForPaper> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Clock clock;

  public CallForPaperEntity(Clock clock) {
    this.clock = clock;
  }

  public Effect<CallForPaper> create(CreateCallForPaper createCallForPaper) {
    if (currentState() != null) {
      return effects().error("Cfp already exists " + commandContext().entityId());
    } else if (!isValid(createCallForPaper)) {
      logger.info("Invalid Cfp: {}", createCallForPaper);
      return effects().error("Invalid Cfp " + commandContext().entityId());
    } else {
      var callForPaper = new CallForPaper(
        commandContext().entityId(),
        createCallForPaper.conferenceName(),
        createCallForPaper.deadline(),
        createCallForPaper.conferenceLink(),
        createCallForPaper.userName(),
        Instant.now(clock)
      );

      logger.info("Creating new Cfp: {}", callForPaper);
      return effects()
        .updateState(callForPaper)
        .thenReply(callForPaper);
    }
  }

  private boolean isValid(CreateCallForPaper createCallForPaper) {
    LocalDate today = LocalDate.ofInstant(clock.instant(), clock.getZone());
    if (createCallForPaper.conferenceName() == null || createCallForPaper.conferenceName().isEmpty()) {
      return false;
    }
    if (createCallForPaper.deadline() == null) {
      return false;
    } else if (
      createCallForPaper.deadline().isAfter(today.plusDays(TimeUnit.SECONDS.toDays(21474835)))) {
      //see akka.actor.LightArrayRevolverScheduler#checkMaxDelay
      logger.info("Deadline too far in the future: {}", createCallForPaper.deadline());
      return false;
    }
    if (createCallForPaper.conferenceLink() == null || createCallForPaper.conferenceLink().isEmpty()) {
      return false;
    }
    return true;
  }

  public Effect<CallForPaper> get() {
    if (currentState() == null) {
      return effects().error("Cfp not found " + commandContext().entityId());
    } else {
      return effects().reply(currentState());
    }
  }

  public Effect<String> delete() {
    if (currentState() == null) {
      return effects().error("Cfp not found " + commandContext().entityId());
    } else {
      logger.info("Deleting Cfp: {}", currentState());
      return effects().deleteEntity().thenReply("Deleted");
    }
  }
}