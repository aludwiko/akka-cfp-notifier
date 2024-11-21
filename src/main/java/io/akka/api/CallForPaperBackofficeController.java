package io.akka.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import akka.javasdk.timer.TimerScheduler;
import io.akka.application.CallForPaperEntity;
import io.akka.application.Notify;
import io.akka.domain.CallForPaperReminder;

import java.util.concurrent.CompletionStage;

import static java.time.Duration.ofMillis;

@HttpEndpoint("/cfp/backoffice")
public class CallForPaperBackofficeController {

  private final ComponentClient componentClient;
  private final TimerScheduler timerScheduler;

  public CallForPaperBackofficeController(ComponentClient componentClient, TimerScheduler timerScheduler) {
    this.componentClient = componentClient;
    this.timerScheduler = timerScheduler;
  }


  @Post("/trigger-notification/{cfpId}")
  public CompletionStage<HttpResponse> triggerNotification(String cfpId) {

    return componentClient
      .forKeyValueEntity(cfpId)
      .method(CallForPaperEntity::get)
      .invokeAsync()
      .thenCompose(callForPaper ->
        timerScheduler.startSingleTimer(
          cfpId,
          ofMillis(1L),
          componentClient
            .forTimedAction()
            .method(Notify::runNotification)
            .deferred(CallForPaperReminder.of(callForPaper, 0))))
      .thenApply(__ -> HttpResponses.ok());
  }
}
