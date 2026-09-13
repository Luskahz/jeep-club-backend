package com.jeepclub.backend.billing.api.http.controller;

import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.payment.PaymentReceiptNotFoundException;
import com.jeepclub.backend.billing.core.application.result.PaymentReceiptResult;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptService;
import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentReceiptSecurityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PaymentReceiptService paymentReceiptService;
    @MockitoBean JwtTokenParser jwtTokenParser;
    @MockitoBean UserAuthoritiesProvider userAuthoritiesProvider;
    @MockitoBean AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/billing/member-payments/1/receipt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerDownloadsOwnReceipt() throws Exception {
        authenticate("owner", 10L, List.of());
        when(paymentReceiptService.find(1L, 10L, false))
                .thenReturn(new PaymentReceiptResult(new byte[]{1, 2}, "application/pdf"));

        mockMvc.perform(get("/billing/member-payments/1/receipt")
                        .header("Authorization", "Bearer owner"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2}))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    void anotherUserIsForbiddenEvenWhenTheyKnowPaymentUrl() throws Exception {
        authenticate("other", 20L, List.of());
        when(paymentReceiptService.find(1L, 20L, false))
                .thenThrow(new MemberPaymentAccessDeniedException("Payment receipt does not belong to authenticated user."));

        mockMvc.perform(get("/billing/member-payments/1/receipt")
                        .header("Authorization", "Bearer other"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_PAYMENT_ACCESS_DENIED"));
    }

    @Test
    void adminWithoutBillingPaymentReadIsForbidden() throws Exception {
        authenticate("admin-no-read", 99L, List.of("BILLING_PAYMENT_CONFIRM"));
        when(paymentReceiptService.find(1L, 99L, false))
                .thenThrow(new MemberPaymentAccessDeniedException("Payment receipt access denied."));

        mockMvc.perform(get("/billing/member-payments/1/receipt")
                        .header("Authorization", "Bearer admin-no-read"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWithBillingPaymentReadDownloadsReceipt() throws Exception {
        authenticate("admin-read", 99L, List.of("BILLING_PAYMENT_READ"));
        when(paymentReceiptService.find(1L, 99L, true))
                .thenReturn(new PaymentReceiptResult(new byte[]{3}, "image/png"));

        mockMvc.perform(get("/billing/member-payments/1/receipt")
                        .header("Authorization", "Bearer admin-read"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void missingPaymentAndMissingReceiptReturnControlledNotFound() throws Exception {
        authenticate("owner", 10L, List.of());
        when(paymentReceiptService.find(404L, 10L, false))
                .thenThrow(new MemberPaymentNotFoundException("Member payment not found."));
        when(paymentReceiptService.find(2L, 10L, false))
                .thenThrow(new PaymentReceiptNotFoundException("Payment receipt file not found."));

        mockMvc.perform(get("/billing/member-payments/404/receipt")
                        .header("Authorization", "Bearer owner"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_PAYMENT_NOT_FOUND"));
        mockMvc.perform(get("/billing/member-payments/2/receipt")
                        .header("Authorization", "Bearer owner"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_RECEIPT_NOT_FOUND"));
    }

    @Test
    void legacyStorageKeyRouteIsNotAvailable() throws Exception {
        authenticate("other", 20L, List.of());

        mockMvc.perform(get("/billing/payment-receipts/2026/09/13/known-key.pdf")
                        .header("Authorization", "Bearer other"))
                .andExpect(status().isNotFound());
    }

    private void authenticate(String token, Long userId, List<String> authorities) {
        when(jwtTokenParser.parseAndValidate(token)).thenReturn(
                new JwtAuthenticatedUser(userId, 100L + userId, "Test User", Instant.now().plusSeconds(3600))
        );
        when(userAuthoritiesProvider.findAuthorityCodesByUserId(userId)).thenReturn(authorities);
    }
}
