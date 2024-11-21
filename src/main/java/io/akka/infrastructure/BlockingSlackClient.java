package io.akka.infrastructure;

import com.google.gson.Gson;
import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.RichTextElement;
import com.slack.api.model.block.element.RichTextSectionElement.Emoji;
import com.slack.api.model.block.element.RichTextSectionElement.Link;
import com.slack.api.model.block.element.RichTextSectionElement.Text;
import com.slack.api.model.view.View;
import com.slack.api.util.json.GsonFactory;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import com.typesafe.config.Config;
import io.akka.application.SlackClient;
import io.akka.application.SlackResponse;
import io.akka.domain.CallForPaperReminder;
import io.akka.view.CallForPaperView;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.Blocks.input;
import static com.slack.api.model.block.Blocks.richText;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.asRichTextElements;
import static com.slack.api.model.block.element.BlockElements.datePicker;
import static com.slack.api.model.block.element.BlockElements.plainTextInput;
import static com.slack.api.model.block.element.BlockElements.richTextList;
import static com.slack.api.model.block.element.BlockElements.richTextSection;
import static com.slack.api.model.block.element.BlockElements.staticSelect;
import static com.slack.api.model.view.Views.view;
import static com.slack.api.model.view.Views.viewClose;
import static com.slack.api.model.view.Views.viewSubmit;
import static com.slack.api.model.view.Views.viewTitle;
import static com.slack.api.webhook.WebhookPayloads.payload;

@Component
@Profile("production")
public class BlockingSlackClient implements SlackClient {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Gson gson = GsonFactory.createSnakeCase(SlackConfig.DEFAULT);

  private final Slack slack = Slack.getInstance(SlackConfig.DEFAULT);
  private final Config config;
  private String openViewUrl = "https://slack.com/api/views.open";

  public BlockingSlackClient(Config config) {
    this.config = config;
  }

  @Override
  public CompletionStage<SlackResponse> postNewCfp(CallForPaperView callForPaperView) {
    RichTextElement info = Text.builder().text("The " + callForPaperView.conferenceName() + " conference call for papers added. Deadline ends on " + callForPaperView.deadline() + ". More info: ").build();
    RichTextElement link = Link.builder().url(callForPaperView.conferenceLink()).build();

    LayoutBlock newCfp = richText(b -> b.elements(asElements(
      richTextSection(s -> s.elements(asRichTextElements(info, link)))
    )));

    Payload payload = payload(p -> p.blocks(List.of(newCfp)).unfurlLinks(true).unfurlMedia(true));
    try {
      WebhookResponse response = slack.send(config.getString("cfp.notifier.webhook"), payload);
      return CompletableFuture.completedFuture(new SlackResponse.Response(response.getCode(), response.getMessage() + "-" + response.getBody()));
    } catch (IOException e) {
      return CompletableFuture.completedFuture(new SlackResponse.Failure(500, "Unexpected exception", e));
    }
  }

  public String getCfpsListPayload(List<CallForPaperView> openCallForPapers) {
    Payload payload = cfpsListPayload(openCallForPapers);
    return gson.toJson(payload);
  }

  private static Payload cfpsListPayload(List<CallForPaperView> openCallForPapers) {
    RichTextElement header = Text.builder().text("Open call for papers:").build();
    List<RichTextElement> openCfps = openCallForPapers.stream().map(cfp -> {
      var link = Link.builder().text(cfp.conferenceName()).url(cfp.conferenceLink()).build();
      var date = Text.builder().text(" ends on " + cfp.deadline()).build();
      return richTextSection(s -> s.elements(asRichTextElements(link, date)));
    }).collect(Collectors.toList());

    LayoutBlock openCfpList = richText(b -> b.elements(asElements(
      richTextSection(s -> s.elements(asRichTextElements(header))),
      richTextList(l -> l.elements(openCfps).style("ordered"))
    )));

    return payload(p -> p.blocks(List.of(openCfpList)));
  }

  @Override
  public CompletionStage<SlackResponse> openCfpsToDelete(List<CallForPaperView> openCallForPapers, String triggerId, String callbackId, String cfpIdField) {

    View modalView = view(v -> v
      .type("modal")
      .title(viewTitle(t -> t.type("plain_text").text("Delete call for papers")))
      .callbackId(callbackId)
      .submit(viewSubmit(vs -> vs.type("plain_text").text("Delete")))
      .close(viewClose(vc -> vc.type("plain_text").text("Cancel")))
      .blocks(asBlocks(
        section(s -> s
          .text(plainText("Select a call for papers to delete"))
          .blockId(cfpIdField)
          .accessory(staticSelect(ss -> ss.actionId(cfpIdField)
            .placeholder(plainText("Select cfp"))
            .options(openCallForPapers.stream().map(cfp ->
                OptionObject.builder()
                  .text(PlainTextObject.builder().text(cfp.conferenceName() + " " + cfp.deadline()).build())
                  .value(cfp.id()).build())
              .collect(Collectors.toList()))
          ))))));

    Modal obj = new Modal(triggerId, modalView);

    String jsonString = gson.toJson(obj);

    return postJsonBody(openViewUrl, jsonString);
  }

  private CompletableFuture<SlackResponse> postJsonBody(String url, String jsonString) {
//    logger.debug("Posting json body to slack: {}, token {}", url, config.getString("cfp.notifier.bot-oauth-token"));
    try (Response response = slack.getHttpClient().postCamelCaseJsonBodyWithBearerHeader(url,
      config.getString("cfp.notifier.bot-oauth-token"),
      jsonString)) {
      return CompletableFuture.completedFuture(new SlackResponse.Response(response.code(), response.message() + "-" + response.body()));
    } catch (IOException exception) {
      return CompletableFuture.completedFuture(new SlackResponse.Failure(500, "Unexpected exception", exception));
    }
  }

  public CompletionStage<SlackResponse> openAddCfp(String triggerId, String callbackId, String conferenceNameField, String conferenceLinkField, String conferenceCfpDeadlineField) {

    View modalView = view(v -> v
      .type("modal")
      .title(viewTitle(t -> t.type("plain_text").text("Add call for papers")))
      .callbackId(callbackId)
      .submit(viewSubmit(vs -> vs.type("plain_text").text("Add")))
      .close(viewClose(vc -> vc.type("plain_text").text("Cancel")))
      .blocks(asBlocks(
        input(i -> i
          .label(plainText("Conference name"))
          .blockId(conferenceNameField)
          .element(plainTextInput(pti -> pti.actionId(conferenceNameField).placeholder(plainText("Enter conference name"))))),
        input(i -> i
          .label(plainText("Conference link"))
          .blockId(conferenceLinkField)
          .element(plainTextInput(pti -> pti.actionId(conferenceLinkField).placeholder(plainText("Enter conference link"))))),
        input(i -> i
          .label(plainText("Call for papers deadline"))
          .blockId(conferenceCfpDeadlineField)
          .element(datePicker(dp -> dp
            .actionId(conferenceCfpDeadlineField)
            .placeholder(plainText("Select a deadline")))))
      )));

    Modal obj = new Modal(triggerId, modalView);

    String jsonString = gson.toJson(obj);
    return postJsonBody(openViewUrl, jsonString);
  }

  @Override
  public CompletionStage<SlackResponse> notifyAboutOpenCfp(CallForPaperReminder reminder) {
    RichTextElement emoji = Emoji.builder().name("mega").build();
    RichTextElement info = Text.builder().text(" " + reminder.conferenceName() + " conference call for papers ends within " + reminder.howManyDaysLeft() + " days (" + reminder.deadline() + "). Remember to submit your talk at ").build();
    RichTextElement link = Link.builder().url(reminder.conferenceLink()).build();

    LayoutBlock cfpReminder = richText(b -> b.elements(asElements(
      richTextSection(s -> s.elements(asRichTextElements(emoji, info, link)))
    )));

    Payload payload = payload(p -> p.blocks(List.of(cfpReminder)).unfurlLinks(false).unfurlMedia(false));
    try {
      WebhookResponse response = slack.send(config.getString("cfp.notifier.webhook"), payload);
      return CompletableFuture.completedFuture(new SlackResponse.Response(response.getCode(), response.getMessage() + "-" + response.getBody()));
    } catch (IOException e) {
      return CompletableFuture.completedFuture(new SlackResponse.Failure(500, "Unexpected exception", e));
    }
  }


  private static PlainTextObject plainText(String text) {
    return PlainTextObject.builder().text(text).build();
  }
}
