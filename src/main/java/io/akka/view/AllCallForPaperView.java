package io.akka.view;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.DeleteHandler;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import io.akka.application.CallForPaperEntity;
import io.akka.domain.CallForPaper;


@ComponentId("cfp_all_view")
public class AllCallForPaperView extends View {

  @Consume.FromKeyValueEntity(CallForPaperEntity.class)
  public static class CallForPaperViewUpdater extends TableUpdater<CallForPaperView> {

    public Effect<CallForPaperView> onChange(CallForPaper callForPaper) {
      return effects().updateRow(CallForPaperView.of(callForPaper));
    }

    @DeleteHandler
    public Effect<CallForPaperView> onDelete() {
      return effects().deleteRow();
    }
  }

  @Query("SELECT * as callForPaperViews FROM cfp_all ORDER BY deadlineInEpochDays ASC")
  public QueryEffect<CallForPaperList> getCallForPapers() {
    return queryResult();
  }

  @Query("SELECT * as callForPaperViews FROM cfp_all WHERE deadlineInEpochDays >= :nowEpochDays ORDER BY deadlineInEpochDays ASC")
  public QueryEffect<CallForPaperList> getOpenCallForPapers(long nowEpochDays) {
    return queryResult();
  }
}
