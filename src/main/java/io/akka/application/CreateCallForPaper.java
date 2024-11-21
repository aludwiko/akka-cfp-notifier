package io.akka.application;

import java.time.LocalDate;

public record CreateCallForPaper(String conferenceName, LocalDate deadline, String conferenceLink, String userName) {
}
