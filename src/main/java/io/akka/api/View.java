package io.akka.api;

import com.slack.api.model.view.ViewState;

public record View(String callbackId, ViewState state) {

}
