package io.akka.api;

import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.FormData;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import akka.stream.Materializer;
import com.typesafe.config.Config;
import io.akka.application.CallForPaperEntity;
import io.akka.application.CreateCallForPaper;
import io.akka.application.SlackClient;
import io.akka.application.SlackResponse;
import io.akka.view.AllCallForPaperView;
import io.akka.view.CallForPaperList;
import io.akka.view.CallForPaperView;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static akka.javasdk.http.HttpResponses.internalServerError;
import static akka.javasdk.http.HttpResponses.ok;
import static java.util.concurrent.CompletableFuture.completedFuture;


@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/api/cfp")
public class CallForPaperController {

  public static final CompletableFuture<HttpResponse> FORBIDDEN = completedFuture(HttpResponses.of(StatusCodes.FORBIDDEN, ContentTypes.TEXT_PLAIN_UTF8, "Access denied".getBytes()));
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;
  private final SlackClient slackClient;
  private final Config config;
  private final Materializer materializer;
  final static String DELETE_CFP_CALLBACK_ID = "delete_cfp";
  final static String DELETE_CFP_ID_FIELD = "cfp_id";
  final static String ADD_CFP_CALLBACK_ID = "add_cfp";
  final static String CONFERENCE_NAME_FIELD = "conference_name";
  final static String CONFERENCE_LINK_FIELD = "conference_link";
  final static String CONFERENCE_CFP_DEADLINE_FIELD = "conference_cfp_deadline";


  public CallForPaperController(ComponentClient componentClient, SlackClient slackClient, Config config, Materializer materializer) {
    this.componentClient = componentClient;
    this.slackClient = slackClient;
    this.config = config;
    this.materializer = materializer;
  }

  @Post("/list")
  public CompletionStage<HttpResponse> postList(HttpRequest request) {

    return Unmarshaller.entityToWwwUrlEncodedFormData().unmarshal(request.entity(), materializer)
      .thenCompose(formData -> {
        Query query = getQuery(formData);
        if (notValidToken(query)) {
          return FORBIDDEN;
        }

        CompletionStage<CallForPaperList> cfps = componentClient
          .forView()
          .method(AllCallForPaperView::getOpenCallForPapers)
          .invokeAsync(LocalDate.now().toEpochDay());

        return cfps.thenApply(callForPaperList -> {
            var payload = slackClient.getCfpsListPayload(callForPaperList.callForPaperViews());
            return HttpResponses.of(StatusCodes.OK, ContentTypes.APPLICATION_JSON, payload.getBytes());
          }
        );
      });
  }

  private boolean notValidToken(Query query) {
    var token = query.getOrElse("token", "");
    return notValidToken(token);
  }

  private boolean notValidToken(String token) {
    boolean notValid = !config.getString("cfp.notifier.verification-token").equals(token);
    if (notValid) {
      logger.debug("Token not valid, rejecting request.");
    }
    return notValid;
  }

  @Post("/delete")
  public CompletionStage<HttpResponse> openDeleteView(HttpRequest request) {

    return Unmarshaller.entityToWwwUrlEncodedFormData().unmarshal(request.entity(), materializer)
      .thenCompose(formData -> {
        Query query = getQuery(formData);
        var triggerId = query.getOrElse("trigger_id", "");
        if (notValidToken(query)) {
          return FORBIDDEN;
        }

        CompletionStage<CallForPaperList> cfps = componentClient
          .forView()
          .method(AllCallForPaperView::getOpenCallForPapers)
          .invokeAsync(LocalDate.now().toEpochDay());

        CompletionStage<HttpResponse> openDeleteView = cfps.thenCompose(callForPaperList ->
            slackClient.openCfpsToDelete(callForPaperList.callForPaperViews(), triggerId, DELETE_CFP_CALLBACK_ID, DELETE_CFP_ID_FIELD))
          .thenApply(res -> switch (res) {
            case SlackResponse.Response response -> {
              if (response.code() != 200) {
                logger.error("Failed to open delete modal status: {}, msg: {}", response.code(), response.message());
                yield internalServerError("Failed to open cfp to delete");
              } else {
                yield ok();
              }
            }
            case SlackResponse.Failure failure -> {
              logger.error("open cfp to delete failed, status: {}, msg: {}, exception: {}", failure.code(), failure.message(), failure.exception());
              yield internalServerError("Failed to open cfp to delete");
            }
          });
        return openDeleteView;
      });
  }

  @Post("/add")
  public CompletionStage<HttpResponse> openAddView(HttpRequest request) {
    return Unmarshaller.entityToWwwUrlEncodedFormData().unmarshal(request.entity(), materializer)
      .thenCompose(formData -> {
        Query query = getQuery(formData);
        var triggerId = query.getOrElse("trigger_id", "");
        if (notValidToken(query)) {
          return FORBIDDEN;
        }
        logger.debug("Processing cfp add request, opening add dialog");
        return slackClient.openAddCfp(triggerId, ADD_CFP_CALLBACK_ID, CONFERENCE_NAME_FIELD, CONFERENCE_LINK_FIELD, CONFERENCE_CFP_DEADLINE_FIELD)
          .thenApply(res -> switch (res) {
            case SlackResponse.Response response -> {
              if (response.code() != 200) {
                logger.error("Failed to open add modal status: {}, msg: {}", response.code(), response.message());
                yield internalServerError("Failed to open add cfp");
              } else {
                yield ok();
              }
            }
            case SlackResponse.Failure failure -> {
              logger.error("open add cfp failed, status: {}, msg: {}, exception: {}", failure.code(), failure.message(), failure.exception());
              yield internalServerError("Failed to open add cfp");
            }
          });
      });
  }

  //temporal solution, waiting for: https://github.com/akka/akka-http/pull/4453
  private Query getQuery(FormData formData) {
    try {
      Field privateFields
        = FormData.class.getDeclaredField("fields");
      privateFields.setAccessible(true);
      return (Query) privateFields.get(formData);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Post("/submit")
  public CompletionStage<HttpResponse> submit(HttpRequest request) {

    return Unmarshaller.entityToWwwUrlEncodedFormData().unmarshal(request.entity(), materializer)
      .thenCompose(formData -> {
        var payload = getQuery(formData).getOrElse("payload", "");
        ViewSubmission viewSubmission = ViewSubmissionParser.parse(payload);

        logger.trace("Payload: {}", payload);
        logger.trace("View submission: {}", viewSubmission);

        if (notValidToken(viewSubmission.token())) {
          return FORBIDDEN;
        }

        if (viewSubmission.type().equals("view_submission")) {
          if (viewSubmission.view().callbackId().equals(DELETE_CFP_CALLBACK_ID)) {
            return handleDelete(viewSubmission);
          } else if (viewSubmission.view().callbackId().equals(ADD_CFP_CALLBACK_ID)) {
            return handleAdd(viewSubmission);
          } else {
            logger.info("Unknown callbackId {}", viewSubmission.view().callbackId());
            return completedFuture(ok());
          }
        } else {
          logger.debug("Ignoring view submission");
          return completedFuture(ok());
        }
      });
  }

  private CompletionStage<HttpResponse> handleDelete(ViewSubmission viewSubmission) {
    String cfpId = viewSubmission.view().state().getValues().get(DELETE_CFP_ID_FIELD).get(DELETE_CFP_ID_FIELD).getSelectedOption().getValue();
    CompletionStage<HttpResponse> deleteCfp = componentClient.forKeyValueEntity(cfpId).method(CallForPaperEntity::delete).invokeAsync()
      .handle((s, throwable) -> {
        if (throwable != null) {
          logger.error("Failed to delete cfp: " + cfpId, throwable);
          return internalServerError("Failed to delete cfp: " + cfpId);
        } else {
          logger.info("Deleted cfp: " + cfpId);
          return ok();
        }
      });
    return deleteCfp;
  }

  private CompletionStage<HttpResponse> handleAdd(ViewSubmission viewSubmission) {
    String conferenceName = viewSubmission.view().state().getValues().get(CONFERENCE_NAME_FIELD).get(CONFERENCE_NAME_FIELD).getValue();
    String conferenceLink = viewSubmission.view().state().getValues().get(CONFERENCE_LINK_FIELD).get(CONFERENCE_LINK_FIELD).getValue();
    String conferenceCfpDeadline = viewSubmission.view().state().getValues().get(CONFERENCE_CFP_DEADLINE_FIELD).get(CONFERENCE_CFP_DEADLINE_FIELD).getSelectedDate();
    var cfpId = UUID.randomUUID().toString();
    CreateCallForPaper callForPaper = new CreateCallForPaper(conferenceName, LocalDate.parse(conferenceCfpDeadline), conferenceLink, viewSubmission.user().username());

    CompletionStage<HttpResponse> addCfp = componentClient.forKeyValueEntity(cfpId)
      .method(CallForPaperEntity::create)
      .invokeAsync(callForPaper)
      .thenCompose(cfp -> {
        Optional<Span> span = startSpan("slack-api-post-new-cfp");
        return slackClient.postNewCfp(CallForPaperView.of(cfp)).thenApply(response -> {
            span.ifPresent(Span::end);
            return response;
          }
        );
      })
      .handle((result, throwable) -> {
        if (throwable != null) {
          logger.error("Failed to add cfp: " + callForPaper, throwable);
          return internalServerError("Failed to add cfp: " + callForPaper);
        } else {
          return switch (result) {
            case SlackResponse.Response response -> {
              if (response.code() != 200) {
                logger.error("Failed to post new cfp {}, status: {}, msg: {}", callForPaper, response.code(), response.message());
              } else {
                logger.info("Added cfp: " + callForPaper);
              }
              yield ok();
            }
            case SlackResponse.Failure failure -> {
              logger.error("Failed to post new cfp: " + callForPaper, failure.exception());
              yield ok();
            }
          };
        }
      });
    return addCfp;
  }

  private Optional<Span> startSpan(String spanName) {
//    var otelCurrentContext = actionContext().metadata().traceContext().asOpenTelemetryContext();
//    Optional<Span> span = actionContext().getOpenTelemetryTracer().map(tracer -> tracer
//      .spanBuilder(spanName)
//      .setParent(otelCurrentContext)
//      .setSpanKind(SpanKind.CLIENT)
//      .startSpan());
    return Optional.empty();
  }
}
