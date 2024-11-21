package io.akka.application;

public sealed interface SlackResponse {
  record Response(int code, String message) implements SlackResponse {
  }

  record Failure(int code, String message, Throwable exception) implements SlackResponse {
  }
}
