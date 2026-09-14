package com.jeepclub.backend.billing.api.http.dto;

import com.jeepclub.backend.billing.api.http.dto.assignment.ChargeAssignmentResponse;
import com.jeepclub.backend.billing.api.http.dto.charge.MemberChargeSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.cycle.ChargeCycleSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.definition.ChargeDefinitionSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.payment.MemberPaymentSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.refund.MemberRefundSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/** OpenAPI-only schemas for the direct Spring Data pages returned by Billing. */
public final class BillingPageSchemas {

    private BillingPageSchemas() {
    }

    @Schema(name = "PageChargeDefinitionSummaryResponse", description = "Página Spring Data de definições de cobrança.")
    public record ChargeDefinitions(
            long totalElements, int totalPages, int size,
            List<ChargeDefinitionSummaryResponse> content,
            int number, Map<String, Object> sort, Map<String, Object> pageable,
            int numberOfElements, boolean first, boolean last, boolean empty
    ) {
    }

    @Schema(name = "PageChargeAssignmentResponse", description = "Página Spring Data de atribuições de cobrança.")
    public record ChargeAssignments(
            long totalElements, int totalPages, int size,
            List<ChargeAssignmentResponse> content,
            int number, Map<String, Object> sort, Map<String, Object> pageable,
            int numberOfElements, boolean first, boolean last, boolean empty
    ) {
    }

    @Schema(name = "PageChargeCycleSummaryResponse", description = "Página Spring Data de ciclos de cobrança.")
    public record ChargeCycles(
            long totalElements, int totalPages, int size,
            List<ChargeCycleSummaryResponse> content,
            int number, Map<String, Object> sort, Map<String, Object> pageable,
            int numberOfElements, boolean first, boolean last, boolean empty
    ) {
    }

    @Schema(name = "PageMemberChargeSummaryResponse", description = "Página Spring Data de cobranças de membros.")
    public record MemberCharges(
            long totalElements, int totalPages, int size,
            List<MemberChargeSummaryResponse> content,
            int number, Map<String, Object> sort, Map<String, Object> pageable,
            int numberOfElements, boolean first, boolean last, boolean empty
    ) {
    }

    @Schema(name = "PageMemberPaymentSummaryResponse", description = "Página Spring Data de pagamentos de membros.")
    public record MemberPayments(
            long totalElements, int totalPages, int size,
            List<MemberPaymentSummaryResponse> content,
            int number, Map<String, Object> sort, Map<String, Object> pageable,
            int numberOfElements, boolean first, boolean last, boolean empty
    ) {
    }

    @Schema(name = "PageMemberRefundSummaryResponse", description = "Página Spring Data de reembolsos de membros.")
    public record MemberRefunds(
            long totalElements, int totalPages, int size,
            List<MemberRefundSummaryResponse> content,
            int number, Map<String, Object> sort, Map<String, Object> pageable,
            int numberOfElements, boolean first, boolean last, boolean empty
    ) {
    }
}
