package io.akka.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViewSubmissionParserTest {


  @Test
  public void shouldParseViewSubmissionPayload() {
    //given
    var payload = "{\"type\":\"view_submission\",\"team\":{\"id\":\"T06A2ECBTSR\",\"domain\":\"lightbend-test\"},\"user\":{\"id\":\"U06AJ1JA509\",\"username\":\"andrzej.ludwikowski\",\"name\":\"andrzej.ludwikowski\",\"team_id\":\"T06A2ECBTSR\"},\"api_app_id\":\"A06A5FB58HG\",\"token\":\"123\",\"trigger_id\":\"6395422326006.6342488401909.cb29b769e06aaa44605296497a118706\",\"view\":{\"id\":\"V06CGQQM41W\",\"team_id\":\"T06A2ECBTSR\",\"type\":\"modal\",\"blocks\":[{\"type\":\"section\",\"block_id\":\"my-id\",\"text\":{\"type\":\"plain_text\",\"text\":\"Select+a+call+for+papers+to+delete\",\"emoji\":true},\"accessory\":{\"type\":\"static_select\",\"action_id\":\"selectedCfp\",\"placeholder\":{\"type\":\"plain_text\",\"text\":\"Select+cfp\",\"emoji\":true},\"options\":[{\"text\":{\"type\":\"plain_text\",\"text\":\"scalaconf+2023-12-14\",\"emoji\":true},\"value\":\"55b0f53e-8e07-434c-84e9-ce0840110043\"},{\"text\":{\"type\":\"plain_text\",\"text\":\"scalaconf+2023-12-14\",\"emoji\":true},\"value\":\"cb803a1c-efe8-4c76-9402-9bfa477e0157\"}]}}],\"private_metadata\":\"\",\"callback_id\":\"view-id\",\"state\":{\"values\":{\"my-id\":{\"selectedCfp\":{\"type\":\"static_select\",\"selected_option\":{\"text\":{\"type\":\"plain_text\",\"text\":\"scalaconf+2023-12-14\",\"emoji\":true},\"value\":\"55b0f53e-8e07-434c-84e9-ce0840110043\"}}}}},\"hash\":\"1703772528.UxFOJEil\",\"title\":{\"type\":\"plain_text\",\"text\":\"Delete+call+for+papers\",\"emoji\":false},\"clear_on_close\":false,\"notify_on_close\":false,\"close\":{\"type\":\"plain_text\",\"text\":\"Cancel\",\"emoji\":false},\"submit\":{\"type\":\"plain_text\",\"text\":\"Delete\",\"emoji\":false},\"previous_view_id\":null,\"root_view_id\":\"V06CGQQM41W\",\"app_id\":\"A06A5FB58HG\",\"external_id\":\"\",\"app_installed_team_id\":\"T06A2ECBTSR\",\"bot_id\":\"B069YSYV6PQ\"},\"response_urls\":[],\"is_enterprise_install\":false,\"enterprise\":null}";

    //when
    ViewSubmission viewSubmission = ViewSubmissionParser.parse(payload);

    //then

    assertThat(viewSubmission.type()).isEqualTo("view_submission");
    assertThat(viewSubmission.token()).isEqualTo("123");
    assertThat(viewSubmission.user().username()).isEqualTo("andrzej.ludwikowski");
    assertThat(viewSubmission.view().callbackId()).isEqualTo("view-id");
    assertThat(viewSubmission.view().state()
      .getValues().get("my-id").get("selectedCfp")
      .getSelectedOption().getValue()).isEqualTo("55b0f53e-8e07-434c-84e9-ce0840110043");
  }

}