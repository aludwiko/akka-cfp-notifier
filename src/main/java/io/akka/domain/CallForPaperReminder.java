package io.akka.domain;

public record CallForPaperReminder(String id, String conferenceName, String deadline, String conferenceLink,
                                   int howManyDaysLeft) {

  public static CallForPaperReminder of(CallForPaper callForPaper, int howManyDaysLeft) {
    return new CallForPaperReminder(callForPaper.id(), callForPaper.conferenceName(), callForPaper.deadline().toString(), callForPaper.conferenceLink(), howManyDaysLeft);
  }
}

