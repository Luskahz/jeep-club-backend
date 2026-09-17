package com.jeepclub.backend.billing.api.http.dto;

import com.jeepclub.backend.billing.api.http.dto.assignment.ChargeAssignmentResponse;
import com.jeepclub.backend.billing.api.http.dto.charge.MemberChargeSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.cycle.ChargeCycleSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.definition.ChargeDefinitionSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.payment.MemberPaymentSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.refund.MemberRefundSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** OpenAPI views of the stable paginated runtime contract for Billing DTOs. */
public final class BillingPageSchemas {

    private BillingPageSchemas() {
    }

    @Schema(name = "PageResponseChargeDefinitionSummaryResponse")
    public record ChargeDefinitions(List<ChargeDefinitionSummaryResponse> content, int number, int size,
                                    long totalElements, int totalPages, int numberOfElements,
                                    boolean first, boolean last, boolean empty) {
    }

    @Schema(name = "PageResponseChargeAssignmentResponse")
    public record ChargeAssignments(List<ChargeAssignmentResponse> content, int number, int size,
                                    long totalElements, int totalPages, int numberOfElements,
                                    boolean first, boolean last, boolean empty) {
    }

    @Schema(name = "PageResponseChargeCycleSummaryResponse")
    public record ChargeCycles(List<ChargeCycleSummaryResponse> content, int number, int size,
                               long totalElements, int totalPages, int numberOfElements,
                               boolean first, boolean last, boolean empty) {
    }

    @Schema(name = "PageResponseMemberChargeSummaryResponse")
    public record MemberCharges(List<MemberChargeSummaryResponse> content, int number, int size,
                                long totalElements, int totalPages, int numberOfElements,
                                boolean first, boolean last, boolean empty) {
    }

    @Schema(name = "PageResponseMemberPaymentSummaryResponse")
    public record MemberPayments(List<MemberPaymentSummaryResponse> content, int number, int size,
                                 long totalElements, int totalPages, int numberOfElements,
                                 boolean first, boolean last, boolean empty) {
    }

    @Schema(name = "PageResponseMemberRefundSummaryResponse")
    public record MemberRefunds(List<MemberRefundSummaryResponse> content, int number, int size,
                                long totalElements, int totalPages, int numberOfElements,
                                boolean first, boolean last, boolean empty) {
    }
}
