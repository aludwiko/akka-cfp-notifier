package io.akka.view;

import akka.javasdk.testkit.TestKitSupport;
import io.akka.application.CallForPaperEntity;
import io.akka.application.CreateCallForPaper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AllCallForPaperViewTest extends TestKitSupport {

  @Test
  public void shouldListOnlyOpenCfps() {
    //given
    String cfpId1 = "1";
    CreateCallForPaper callForPaper1 = new CreateCallForPaper("My conference 1", LocalDate.now().minusDays(1), "url", "andrzej");
    await(componentClient.forKeyValueEntity(cfpId1).method(CallForPaperEntity::create).invokeAsync(callForPaper1));

    String cfpId2 = "2";
    CreateCallForPaper callForPaper2 = new CreateCallForPaper("My conference 2", LocalDate.now().plusDays(1), "url", "andrzej");
    await(componentClient.forKeyValueEntity(cfpId2).method(CallForPaperEntity::create).invokeAsync(callForPaper2));

    String cfpId3 = "3";
    CreateCallForPaper callForPaper3 = new CreateCallForPaper("My conference 3", LocalDate.now().plusDays(1), "url", "andrzej");
    await(componentClient.forKeyValueEntity(cfpId3).method(CallForPaperEntity::create).invokeAsync(callForPaper3));


    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        CallForPaperList list = await(componentClient.forView().method(AllCallForPaperView::getOpenCallForPapers).invokeAsync(LocalDate.now().toEpochDay()));
        assertThat(list.callForPaperViews()).hasSize(2);
      });
  }
}