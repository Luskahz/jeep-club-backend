package com.jeepclub.backend.billing.api.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingOpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiPublishesBillingHttpStorageAndFinancialContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/billing/charge-definitions']['post']['x-required-permissions'][0]")
                        .value("BILLING_CHARGE_DEFINITION_CREATE"))
                .andExpect(jsonPath("$['paths']['/billing/charge-definitions']['get']['x-required-permissions'][0]")
                        .value("BILLING_CHARGE_DEFINITION_READ"))
                .andExpect(jsonPath("$['paths']['/billing/charge-definitions/{chargeDefinitionId}/assignments/all-members']['post']['x-required-permissions'][0]")
                        .value("BILLING_CHARGE_ASSIGNMENT_CREATE"))
                .andExpect(jsonPath("$['paths']['/billing/charge-definitions/{chargeDefinitionId}/cycles']['post']['x-required-permissions'][0]")
                        .value("BILLING_CHARGE_CYCLE_GENERATE"))
                .andExpect(jsonPath("$['paths']['/billing/member-charges/{memberChargeId}/cancel']['patch']['x-required-permissions'][0]")
                        .value("BILLING_MEMBER_CHARGE_CANCEL"))
                .andExpect(jsonPath("$['paths']['/billing/member-payments/{paymentId}/confirm']['patch']['x-required-permissions'][0]")
                        .value("BILLING_PAYMENT_CONFIRM"))
                .andExpect(jsonPath("$['paths']['/billing/member-refunds/{refundId}/mark-as-refunded']['patch']['x-required-permissions'][0]")
                        .value("BILLING_REFUND_MARK_AS_REFUNDED"))
                .andExpect(jsonPath("$['paths']['/billing/charge-definitions']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseChargeDefinitionSummaryResponse"))
                .andExpect(jsonPath("$['paths']['/billing/charge-definitions/{chargeDefinitionId}/assignments']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseChargeAssignmentResponse"))
                .andExpect(jsonPath("$['paths']['/billing/charge-definitions/{chargeDefinitionId}/cycles']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseChargeCycleSummaryResponse"))
                .andExpect(jsonPath("$['paths']['/billing/me/member-charges']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseMemberChargeSummaryResponse"))
                .andExpect(jsonPath("$['paths']['/billing/member-payments']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseMemberPaymentSummaryResponse"))
                .andExpect(jsonPath("$['paths']['/billing/member-refunds']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseMemberRefundSummaryResponse"))
                .andExpect(jsonPath("$['paths']['/billing/member-charges']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseMemberChargeSummaryResponse"))
                .andExpect(jsonPath("$['paths']['/billing/users/me/member-refunds']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseMemberRefundSummaryResponse"))
                .andExpect(jsonPath("$['paths']['/billing/charge-cycles/{cycleId}/member-refunds']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageResponseMemberRefundSummaryResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseMemberChargeSummaryResponse']['properties']['content']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseMemberChargeSummaryResponse']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/MemberChargeSummaryResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseChargeAssignmentResponse']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/ChargeAssignmentResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseChargeCycleSummaryResponse']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/ChargeCycleSummaryResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseMemberPaymentSummaryResponse']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/MemberPaymentSummaryResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseMemberRefundSummaryResponse']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/MemberRefundSummaryResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageResponseMemberChargeSummaryResponse']['properties']['pageable']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['PageResponseMemberChargeSummaryResponse']['properties']['sort']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/billing/me/member-charges']['get']['parameters'][?(@.name == 'page')].schema.default")
                        .value(hasItem(0)))
                .andExpect(jsonPath("$['paths']['/billing/me/member-charges']['get']['parameters'][?(@.name == 'size')].schema.default")
                        .value(hasItem(20)))
                .andExpect(jsonPath("$['paths']['/billing/member-charges/{memberChargeId}/payments']['post']['requestBody']['content']['multipart/form-data']['schema']['$ref']")
                        .value("#/components/schemas/SubmitMemberPaymentRequest"))
                .andExpect(jsonPath("$['components']['schemas']['SubmitMemberPaymentRequest']['properties']['receiptFile']['type']")
                        .value("string"))
                .andExpect(jsonPath("$['components']['schemas']['SubmitMemberPaymentRequest']['properties']['receiptFile']['format']")
                        .value("binary"))
                .andExpect(jsonPath("$['paths']['/billing/member-payments/{paymentId}/receipt']['get']['responses']['200']['content']['application/octet-stream']['schema']['type']")
                        .value("string"))
                .andExpect(jsonPath("$['paths']['/billing/member-payments/{paymentId}/receipt']['get']['responses']['200']['content']['application/octet-stream']['schema']['format']")
                        .value("binary"))
                .andExpect(jsonPath("$['paths']['/billing/member-payments/{paymentId}/receipt']['get']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/billing/member-payments/{paymentId}/receipt']['get']['responses']['403']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/billing/member-charges/{memberChargeId}/payments']['post']['responses']['409']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/billing/charge-definitions']['get']['responses']['403']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['components']['schemas']['MemberPaymentResponse']['properties']['receiptUrl']['example']")
                        .value("/billing/member-payments/1/receipt"))
                .andExpect(content().string(containsString("/billing/member-payments/{paymentId}/receipt")))
                .andExpect(content().string(not(containsString("receiptStorageKey"))));
    }
}
