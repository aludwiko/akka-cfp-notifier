package io.akka.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.timedaction.TimedAction;
import io.akka.domain.CallForPaperReminder;

@ComponentId("notify-about-open-cfp")
public class Notify extends TimedAction {

  private final SlackClient slackClient;

  public Notify(SlackClient slackClient) {
    this.slackClient = slackClient;
  }

  public Effect runNotification(CallForPaperReminder callForPaperReminder) {
    return effects().asyncDone(slackClient.notifyAboutOpenCfp(callForPaperReminder).thenApply(__ -> Done.getInstance()));
  }
}
