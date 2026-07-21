package com.vegawatt.core.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.access.domain.HomeAccessService;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.AssetRegistrationPublisher;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterHomeUseCaseTest {

    @Mock
    private HomeRepository homeRepository;

    @Mock
    private BillingAccountRepository billingAccountRepository;

    @Mock
    private HomeLiveStatePort homeLiveStatePort;

    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;

    @Mock
    private AssetRegistrationPublisher assetRegistrationPublisher;

    @Mock
    private HomeAccessService homeAccessService;

    @Mock
    private ClockProvider clockProvider;

    @Test
    void registersHomeWithAppliancesAndInitializesLiveState() {
        when(clockProvider.now()).thenReturn(Instant.parse("2026-07-20T10:00:00Z"));
        when(homeRepository.save(any(Home.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billingAccountRepository.save(any(BillingAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterHomeUseCase useCase = new RegisterHomeUseCase(homeRepository, billingAccountRepository,
                homeLiveStatePort, applianceLiveStatePort, assetRegistrationPublisher, homeAccessService,
                clockProvider);

        UUID ownerUserId = UUID.randomUUID();
        RegisterHomeCommand command = new RegisterHomeCommand(
                "Ayşe'nin Evi", "ayse@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"),
                List.of(new RegisterHomeCommand.ApplianceCommand("Buzdolabı", "REFRIGERATOR",
                        new BigDecimal("2200"), new BigDecimal("100"), new BigDecimal("2000"))),
                ownerUserId);

        Home result = useCase.execute(command);

        assertThat(result.name()).isEqualTo("Ayşe'nin Evi");
        assertThat(result.appliances()).hasSize(1);
        verify(homeRepository).save(result);
        verify(assetRegistrationPublisher).publish(result);
        verify(homeLiveStatePort).initialize(any());
        verify(applianceLiveStatePort).initialize(any());
        verify(homeAccessService).grantOwnership(result.id(), ownerUserId, Instant.parse("2026-07-20T10:00:00Z"));
    }
}
