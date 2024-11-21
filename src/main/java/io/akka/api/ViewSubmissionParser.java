package io.akka.api;

import com.google.gson.Gson;
import com.slack.api.SlackConfig;
import com.slack.api.util.json.GsonFactory;

public class ViewSubmissionParser {

  private static Gson gson = GsonFactory.createSnakeCase(SlackConfig.DEFAULT);

  public static ViewSubmission parse(String payload) {
    return gson.fromJson(payload, ViewSubmission.class);
  }
}
