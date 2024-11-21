package io.akka.domain;

import java.time.Instant;
import java.time.LocalDate;

public record CallForPaper(String id,
                           String conferenceName,
                           LocalDate deadline,
                           String conferenceLink,
                           String createdBy,
                           Instant createdAt) {
}

