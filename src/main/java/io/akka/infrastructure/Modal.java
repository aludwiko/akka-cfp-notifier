package io.akka.infrastructure;

import com.slack.api.model.view.View;

public class Modal {
  String triggerId;
  View view;

  public Modal(String triggerId, View view) {
    this.triggerId = triggerId;
    this.view = view;
  }

  public String getTriggerId() {
    return triggerId;
  }

  public void setTriggerId(String triggerId) {
    this.triggerId = triggerId;
  }
}
