package io.akka.application;

import io.akka.domain.CallForPaperReminder;
import io.akka.view.CallForPaperView;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface SlackClient {

  String getCfpsListPayload(List<CallForPaperView> openCallForPapers);

  CompletionStage<SlackResponse> postNewCfp(CallForPaperView callForPaperView);

  CompletionStage<SlackResponse> openCfpsToDelete(List<CallForPaperView> openCallForPapers, String triggerId, String callbackId, String cfpIdField);

  CompletionStage<SlackResponse> openAddCfp(String triggerId, String callbackId, String conferenceNameField, String conferenceLinkField, String conferenceCfpDeadlineField);

  CompletionStage<SlackResponse> notifyAboutOpenCfp(CallForPaperReminder callForPaperReminder);
}
