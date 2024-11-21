package io.akka.api;

import akka.http.javadsl.model.StatusCodes;
import akka.javasdk.http.StrictResponse;
import akka.javasdk.testkit.TestKitSupport;
import akka.util.ByteString;
import com.google.gson.Gson;
import com.slack.api.SlackConfig;
import com.slack.api.model.block.RichTextBlock;
import com.slack.api.model.block.element.RichTextListElement;
import com.slack.api.model.block.element.RichTextSectionElement;
import com.slack.api.model.view.ViewState;
import com.slack.api.util.json.GsonFactory;
import com.slack.api.webhook.Payload;
import io.akka.application.CallForPaperEntity;
import io.akka.application.CreateCallForPaper;
import io.akka.view.AllCallForPaperView;
import io.akka.view.CallForPaperList;
import io.akka.view.CallForPaperView;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.model.ContentTypes.APPLICATION_X_WWW_FORM_URLENCODED;
import static io.akka.api.CallForPaperController.ADD_CFP_CALLBACK_ID;
import static io.akka.api.CallForPaperController.CONFERENCE_CFP_DEADLINE_FIELD;
import static io.akka.api.CallForPaperController.CONFERENCE_LINK_FIELD;
import static io.akka.api.CallForPaperController.CONFERENCE_NAME_FIELD;
import static io.akka.api.CallForPaperController.DELETE_CFP_CALLBACK_ID;
import static io.akka.api.CallForPaperController.DELETE_CFP_ID_FIELD;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;


class CallForPaperControllerIntegrationTest extends TestKitSupport {

  private final Gson gson = GsonFactory.createSnakeCase(SlackConfig.DEFAULT);

  private String token = "123";

  @Test
  public void shouldOpenAddView() {
    //given
    String triggerId = "trigger123";

    //when
    String body = "token=" + token + "&trigger_id=" + triggerId;
    StrictResponse<ByteString> response = await(httpClient.POST("/api/cfp/add")
      .withRequestBody(body)
      .withRequestBody(APPLICATION_X_WWW_FORM_URLENCODED, body.getBytes())
      .invokeAsync());

    //then
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  public void shouldOpenDeleteView() {
    //given
    String triggerId = "trigger123";

    //when
    String body = "token=" + token + "&trigger_id=" + triggerId;
    StrictResponse<ByteString> response = await(httpClient.POST("/api/cfp/delete")
      .withRequestBody(body)
      .withRequestBody(APPLICATION_X_WWW_FORM_URLENCODED, body.getBytes())
      .invokeAsync());

    //then
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  public void shouldAddCfp() {
    //given
    String conferenceName = "My conference";
    String conferenceLink = "url";
    String cfpDeadline = LocalDate.now().plusDays(20).toString();
    ViewState.Value conferenceNameValue = new ViewState.Value();
    conferenceNameValue.setValue(conferenceName);
    ViewState.Value conferenceLinkValue = new ViewState.Value();
    conferenceLinkValue.setValue(conferenceLink);
    ViewState.Value cfpDeadlinekValue = new ViewState.Value();
    cfpDeadlinekValue.setSelectedDate(cfpDeadline);
    Map<String, Map<String, ViewState.Value>> values = Map.of(CONFERENCE_NAME_FIELD, Map.of(CONFERENCE_NAME_FIELD, conferenceNameValue),
      CONFERENCE_LINK_FIELD, Map.of(CONFERENCE_LINK_FIELD, conferenceLinkValue),
      CONFERENCE_CFP_DEADLINE_FIELD, Map.of(CONFERENCE_CFP_DEADLINE_FIELD, cfpDeadlinekValue));
    View view = new View(ADD_CFP_CALLBACK_ID, ViewState.builder().values(values).build());
    ViewSubmission viewSubmission = new ViewSubmission("view_submission", token, new ViewSubmissionUser("andrzej"), view);

    //when
    var body = "payload=" + URLEncoder.encode(gson.toJson(viewSubmission), UTF_8);
    StrictResponse<ByteString> response = await(httpClient.POST("/api/cfp/submit")
      .withRequestBody(body)
      .withRequestBody(APPLICATION_X_WWW_FORM_URLENCODED, body.getBytes())
      .invokeAsync());


    //then
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        CallForPaperList callForPaperList = await(componentClient.forView().method(AllCallForPaperView::getCallForPapers).invokeAsync());
        assertThat(callForPaperList.callForPaperViews()).hasSize(1);
        CallForPaperView cfp = callForPaperList.callForPaperViews().getLast();
        assertThat(cfp.conferenceName()).isEqualTo(conferenceName);
        assertThat(cfp.conferenceLink()).isEqualTo(conferenceLink);
        assertThat(cfp.deadline()).isEqualTo(cfpDeadline);
      });
  }

  @Test
  public void shouldDeleteCfp() {
    //given
    String cfpId = UUID.randomUUID().toString();
    await(componentClient.forKeyValueEntity(cfpId)
      .method(CallForPaperEntity::create)
      .invokeAsync(new CreateCallForPaper("My conference", LocalDate.now().plusDays(20), "url", "andrzej")));


    ViewState.SelectedOption selectedOption = new ViewState.SelectedOption();
    selectedOption.setValue(cfpId);
    ViewState.Value cfpIdValue = new ViewState.Value();
    cfpIdValue.setSelectedOption(selectedOption);
    Map<String, Map<String, ViewState.Value>> values = Map.of(DELETE_CFP_ID_FIELD, Map.of(DELETE_CFP_ID_FIELD, cfpIdValue));
    View view = new View(DELETE_CFP_CALLBACK_ID, ViewState.builder().values(values).build());
    ViewSubmission viewSubmission = new ViewSubmission("view_submission", token, new ViewSubmissionUser("andrzej"), view);

    //when
    var body = "payload=" + URLEncoder.encode(gson.toJson(viewSubmission), UTF_8);
    StrictResponse<ByteString> response = await(httpClient.POST("/api/cfp/submit")
      .withRequestBody(body)
      .withRequestBody(APPLICATION_X_WWW_FORM_URLENCODED, body.getBytes())
      .invokeAsync());


    //then
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    StrictResponse<ByteString> getCfpResponse = await(httpClient.GET("/cfp/" + cfpId).invokeAsync());
    assertThat(getCfpResponse.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  public void shouldListCfps() {
    //given
    String cfpId1 = "1";
    CreateCallForPaper callForPaper1 = new CreateCallForPaper("My conference 1", LocalDate.now().plusDays(3), "url", "andrzej");
    await(componentClient.forKeyValueEntity(cfpId1).method(CallForPaperEntity::create).invokeAsync(callForPaper1));

    String cfpId2 = "2";
    CreateCallForPaper callForPaper2 = new CreateCallForPaper("My conference 2", LocalDate.now().plusDays(1), "url", "andrzej");
    await(componentClient.forKeyValueEntity(cfpId2).method(CallForPaperEntity::create).invokeAsync(callForPaper2));

    String cfpId3 = "3";
    CreateCallForPaper callForPaper3 = new CreateCallForPaper("My conference 3", LocalDate.now().plusDays(2), "url", "andrzej");
    await(componentClient.forKeyValueEntity(cfpId3).method(CallForPaperEntity::create).invokeAsync(callForPaper3));

    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        //when
        String body = "token=" + token;
        StrictResponse<ByteString> response = await(httpClient.POST("/api/cfp/list")
          .withRequestBody(body)
          .withRequestBody(APPLICATION_X_WWW_FORM_URLENCODED, body.getBytes())
          .invokeAsync());

        //then
        assertThat(response.status()).isEqualTo(StatusCodes.OK);
        Payload payload = gson.fromJson(response.body().utf8String(), Payload.class);
        RichTextBlock richTextBlock = (RichTextBlock) payload.getBlocks().get(0);
        RichTextListElement richTextElements = (RichTextListElement) richTextBlock.getElements().get(1);
        List<String> conferenceNames = richTextElements.getElements().stream()
          .map(el -> (RichTextSectionElement) el)
          .map(el -> (RichTextSectionElement.Link) el.getElements().get(0))
          .map(link -> link.getText())
          .limit(3) //avoid data from other tests
          .toList();

        assertThat(conferenceNames).containsExactly("My conference 2", "My conference 3", "My conference 1");
      });
  }

}