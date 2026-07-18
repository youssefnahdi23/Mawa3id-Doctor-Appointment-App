package com.mawa3id.donation;

import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.donation.dto.CreateDonationRequest;
import com.mawa3id.donation.dto.DonationConfigResponse;
import com.mawa3id.donation.dto.DonationResponse;
import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonationServiceTest {

    private DonationRepository donationRepository;
    private UserRepository userRepository;
    private PaymentGateway paymentGateway;
    private KonnectGateway konnectGateway;
    private DonationProperties properties;
    private DonationService service;

    @BeforeEach
    void setUp() {
        donationRepository = mock(DonationRepository.class);
        userRepository = mock(UserRepository.class);
        paymentGateway = mock(PaymentGateway.class);
        konnectGateway = mock(KonnectGateway.class);
        properties = new DonationProperties();
        properties.setEnabled(true);
        properties.setCurrency("usd");
        properties.setMinAmountMinor(100);
        properties.setPatreonUrl("https://patreon.com/mawa3id");
        service = new DonationService(donationRepository, userRepository, paymentGateway,
                konnectGateway, properties);

        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.createCheckoutSession(any()))
                .thenReturn(new PaymentGateway.CheckoutSession("cs_1", "https://pay/cs_1"));
        when(konnectGateway.createCheckoutSession(any()))
                .thenReturn(new PaymentGateway.CheckoutSession("kref_1", "https://konnect/kref_1"));
    }

    private void configureKonnect() {
        properties.getKonnect().setApiKey("key");
        properties.getKonnect().setWalletId("wallet");
    }

    private void configureRib() {
        properties.getRib().setAccountHolder("Mawa3id Association");
        properties.getRib().setBankName("BIAT");
        properties.getRib().setNumber("08 001 0000123456789 12");
    }

    private CreateDonationRequest request(long amount, String currency) {
        return new CreateDonationRequest(amount, currency, "Sam", "Keep it up!", null);
    }

    private CreateDonationRequest requestFor(long amount, DonationProvider provider) {
        return new CreateDonationRequest(amount, null, "Sam", "Keep it up!", provider);
    }

    @Test
    void anonymousDonationDefaultsCurrencyAndReturnsCheckoutUrl() {
        DonationResponse response = service.create(null, request(500, null));

        assertThat(response.status()).isEqualTo(DonationStatus.PENDING);
        assertThat(response.userId()).isNull();
        assertThat(response.checkoutUrl()).isEqualTo("https://pay/cs_1");

        ArgumentCaptor<Donation> saved = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(saved.capture());
        assertThat(saved.getValue().getCurrency()).isEqualTo("usd");
        assertThat(saved.getValue().getProvider()).isEqualTo(DonationProvider.STRIPE);
        assertThat(saved.getValue().getProviderSessionId()).isEqualTo("cs_1");
    }

    @Test
    void attributedDonationLinksUserAndNormalisesCurrency() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        DonationResponse response = service.create(7L, request(1000, "EUR"));

        assertThat(response.userId()).isEqualTo(7L);
        ArgumentCaptor<Donation> saved = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(saved.capture());
        assertThat(saved.getValue().getCurrency()).isEqualTo("eur");
        assertThat(saved.getValue().getUser()).isSameAs(user);
    }

    @Test
    void unknownAttributedUserIsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, request(500, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(donationRepository, never()).save(any());
    }

    @Test
    void belowMinimumIsRejected() {
        assertThatThrownBy(() -> service.create(null, request(50, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(
                        org.springframework.http.HttpStatus.BAD_REQUEST));
        verify(paymentGateway, never()).createCheckoutSession(any());
    }

    @Test
    void disabledFeatureIsUnavailable() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.create(null, request(500, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void konnectDonationRoutesToKonnectAndForcesTnd() {
        configureKonnect();

        DonationResponse response = service.create(null, requestFor(5000, DonationProvider.KONNECT));

        assertThat(response.checkoutUrl()).isEqualTo("https://konnect/kref_1");
        ArgumentCaptor<Donation> saved = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo(DonationProvider.KONNECT);
        assertThat(saved.getValue().getCurrency()).isEqualTo("tnd");
        assertThat(saved.getValue().getProviderSessionId()).isEqualTo("kref_1");
        verify(paymentGateway, never()).createCheckoutSession(any());
    }

    @Test
    void konnectUnconfiguredIsUnavailable() {
        assertThatThrownBy(() -> service.create(null, requestFor(5000, DonationProvider.KONNECT)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        verify(donationRepository, never()).save(any());
    }

    @Test
    void ribDonationIsRecordedPendingWithoutCheckout() {
        configureRib();

        DonationResponse response = service.create(null, requestFor(20000, DonationProvider.RIB));

        assertThat(response.status()).isEqualTo(DonationStatus.PENDING);
        assertThat(response.checkoutUrl()).isNull();
        ArgumentCaptor<Donation> saved = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo(DonationProvider.RIB);
        assertThat(saved.getValue().getCurrency()).isEqualTo("tnd");
        assertThat(saved.getValue().getProviderSessionId()).isNull();
        verify(paymentGateway, never()).createCheckoutSession(any());
        verify(konnectGateway, never()).createCheckoutSession(any());
    }

    @Test
    void ribUnconfiguredIsUnavailable() {
        assertThatThrownBy(() -> service.create(null, requestFor(20000, DonationProvider.RIB)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void konnectWebhookCompletedMarksSucceeded() {
        configureKonnect();
        Donation donation = new Donation(null, 5000, "tnd", DonationProvider.KONNECT, "Sam", null);
        donation.attachSession("kref_1");
        when(donationRepository.findByProviderSessionId("kref_1")).thenReturn(Optional.of(donation));
        when(konnectGateway.fetchPaymentStatus("kref_1"))
                .thenReturn(PaymentGateway.WebhookEvent.Type.COMPLETED);

        service.handleKonnectWebhook("kref_1");

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.SUCCEEDED);
        assertThat(donation.getProviderPaymentRef()).isEqualTo("kref_1");
    }

    @Test
    void konnectWebhookStillPendingLeavesDonationPending() {
        Donation donation = new Donation(null, 5000, "tnd", DonationProvider.KONNECT, "Sam", null);
        donation.attachSession("kref_1");
        when(donationRepository.findByProviderSessionId("kref_1")).thenReturn(Optional.of(donation));
        when(konnectGateway.fetchPaymentStatus("kref_1"))
                .thenReturn(PaymentGateway.WebhookEvent.Type.IGNORED);

        service.handleKonnectWebhook("kref_1");

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.PENDING);
    }

    @Test
    void konnectWebhookIgnoresUnknownRefAndBlank() {
        when(donationRepository.findByProviderSessionId("nope")).thenReturn(Optional.empty());

        service.handleKonnectWebhook("nope");
        service.handleKonnectWebhook("");
        service.handleKonnectWebhook(null);

        verify(konnectGateway, never()).fetchPaymentStatus(any());
    }

    @Test
    void konnectWebhookDoesNotTouchStripeDonations() {
        // A Stripe donation whose session id somehow matches must not be re-verified via Konnect.
        Donation donation = pending();
        donation.attachSession("cs_1");
        when(donationRepository.findByProviderSessionId("cs_1")).thenReturn(Optional.of(donation));

        service.handleKonnectWebhook("cs_1");

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.PENDING);
        verify(konnectGateway, never()).fetchPaymentStatus(any());
    }

    @Test
    void webhookCompletedMarksSucceeded() {
        Donation donation = pending();
        when(donationRepository.findByProviderSessionId("cs_1")).thenReturn(Optional.of(donation));

        service.handleWebhook(payloadFor(PaymentGateway.WebhookEvent.Type.COMPLETED, "cs_1", "pi_9"), "sig");

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.SUCCEEDED);
        assertThat(donation.getProviderPaymentRef()).isEqualTo("pi_9");
    }

    @Test
    void webhookExpiredMarksExpired() {
        Donation donation = pending();
        when(donationRepository.findByProviderSessionId("cs_1")).thenReturn(Optional.of(donation));

        service.handleWebhook(payloadFor(PaymentGateway.WebhookEvent.Type.EXPIRED, "cs_1", null), "sig");

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.EXPIRED);
    }

    @Test
    void webhookFailedMarksFailed() {
        Donation donation = pending();
        when(donationRepository.findByProviderSessionId("cs_1")).thenReturn(Optional.of(donation));

        service.handleWebhook(payloadFor(PaymentGateway.WebhookEvent.Type.FAILED, "cs_1", null), "sig");

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.FAILED);
    }

    @Test
    void webhookIgnoredTypeDoesNotTouchRepository() {
        service.handleWebhook(payloadFor(PaymentGateway.WebhookEvent.Type.IGNORED, "cs_1", null), "sig");
        verify(donationRepository, never()).findByProviderSessionId(any());
    }

    @Test
    void webhookWithoutSessionIdIsIgnored() {
        service.handleWebhook(payloadFor(PaymentGateway.WebhookEvent.Type.COMPLETED, null, null), "sig");
        verify(donationRepository, never()).findByProviderSessionId(any());
    }

    @Test
    void webhookForUnknownSessionIsNoOp() {
        when(donationRepository.findByProviderSessionId("cs_x")).thenReturn(Optional.empty());
        // Should not throw.
        service.handleWebhook(payloadFor(PaymentGateway.WebhookEvent.Type.COMPLETED, "cs_x", "pi"), "sig");
    }

    @Test
    void webhookIsIdempotentOnAlreadyFinalisedDonation() {
        Donation donation = pending();
        donation.markSucceeded("pi_first");
        when(donationRepository.findByProviderSessionId("cs_1")).thenReturn(Optional.of(donation));

        service.handleWebhook(payloadFor(PaymentGateway.WebhookEvent.Type.FAILED, "cs_1", null), "sig");

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.SUCCEEDED);
        assertThat(donation.getProviderPaymentRef()).isEqualTo("pi_first");
    }

    @Test
    void listForUserMapsDonations() {
        Donation donation = pending();
        when(donationRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(donation));

        List<DonationResponse> result = service.listForUser(7L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).checkoutUrl()).isNull();
    }

    @Test
    void configReflectsProperties() {
        DonationConfigResponse config = service.config();

        assertThat(config.cardEnabled()).isTrue();
        assertThat(config.currency()).isEqualTo("usd");
        assertThat(config.minAmountMinor()).isEqualTo(100);
        assertThat(config.patreonUrl()).isEqualTo("https://patreon.com/mawa3id");
        // Unconfigured local methods stay hidden.
        assertThat(config.konnectEnabled()).isFalse();
        assertThat(config.ribNumber()).isEmpty();
    }

    @Test
    void configSurfacesKonnectAndRibWhenConfigured() {
        configureKonnect();
        configureRib();

        DonationConfigResponse config = service.config();

        assertThat(config.konnectEnabled()).isTrue();
        assertThat(config.konnectCurrency()).isEqualTo("tnd");
        assertThat(config.ribAccountHolder()).isEqualTo("Mawa3id Association");
        assertThat(config.ribBankName()).isEqualTo("BIAT");
        assertThat(config.ribNumber()).isEqualTo("08 001 0000123456789 12");
    }

    @Test
    void masterSwitchHidesKonnectAndRib() {
        configureKonnect();
        configureRib();
        properties.setEnabled(false);

        DonationConfigResponse config = service.config();

        assertThat(config.cardEnabled()).isFalse();
        assertThat(config.konnectEnabled()).isFalse();
        assertThat(config.ribNumber()).isEmpty();
    }

    private Donation pending() {
        return new Donation(null, 500, "usd", DonationProvider.STRIPE, "Sam", "msg");
    }

    /** Drive the mocked gateway to return the desired parsed event. */
    private String payloadFor(PaymentGateway.WebhookEvent.Type type, String sessionId, String paymentRef) {
        String payload = type + "|" + sessionId + "|" + paymentRef;
        when(paymentGateway.verifyAndParse(payload, "sig"))
                .thenReturn(new PaymentGateway.WebhookEvent(type, sessionId, paymentRef));
        return payload;
    }
}
