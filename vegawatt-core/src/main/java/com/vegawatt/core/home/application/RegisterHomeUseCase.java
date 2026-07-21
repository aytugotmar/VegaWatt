package com.vegawatt.core.home.application;

import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.AssetRegistrationPublisher;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterHomeUseCase {

    private static final DateTimeFormatter BILLING_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM")
            .withZone(ZoneOffset.UTC);

    private final HomeRepository homeRepository;
    private final BillingAccountRepository billingAccountRepository;
    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final AssetRegistrationPublisher assetRegistrationPublisher;
    private final ClockProvider clockProvider;

    public RegisterHomeUseCase(HomeRepository homeRepository, BillingAccountRepository billingAccountRepository,
                                HomeLiveStatePort homeLiveStatePort, ApplianceLiveStatePort applianceLiveStatePort,
                                AssetRegistrationPublisher assetRegistrationPublisher, ClockProvider clockProvider) {
        this.homeRepository = homeRepository;
        this.billingAccountRepository = billingAccountRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.assetRegistrationPublisher = assetRegistrationPublisher;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public Home execute(RegisterHomeCommand command) {
        Instant now = clockProvider.now();
        Home home = Home.register(command.name(), command.contactEmail(), command.energyQuotaKwh(),
                command.budgetQuotaTry(), command.baseTariffPerKwh(), command.penaltyTariffPerKwh(), now);

        for (RegisterHomeCommand.ApplianceCommand applianceCommand : command.appliances()) {
            home.addAppliance(Appliance.create(home.id(), applianceCommand.name(), applianceCommand.type(),
                    applianceCommand.safePowerLimitWatt(), applianceCommand.simulationMinWatt(),
                    applianceCommand.simulationMaxWatt()));
        }

        homeRepository.save(home);

        String billingPeriod = BILLING_PERIOD_FORMAT.format(now);
        billingAccountRepository.save(BillingAccount.open(home.id(), billingPeriod, now));

        assetRegistrationPublisher.publish(home);

        initializeLiveState(home, now);

        return home;
    }

    private void initializeLiveState(Home home, Instant now) {
        homeLiveStatePort.initialize(HomeLiveState.zero(home.id(), home.name(), now));
        for (Appliance appliance : home.appliances()) {
            applianceLiveStatePort.initialize(ApplianceLiveState.zero(home.id(), appliance.id(), appliance.name(),
                    appliance.type(), appliance.safePowerLimitWatt(), now));
        }
    }
}
