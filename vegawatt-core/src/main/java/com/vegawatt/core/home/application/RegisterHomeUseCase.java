package com.vegawatt.core.home.application;

import com.vegawatt.core.access.domain.HomeAccessService;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.time.BillingPeriodResolver;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.AssetRegistrationPublisher;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.home.domain.InvalidCatalogSelectionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterHomeUseCase {

    private final HomeRepository homeRepository;
    private final BillingAccountRepository billingAccountRepository;
    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final AssetRegistrationPublisher assetRegistrationPublisher;
    private final HomeAccessService homeAccessService;
    private final ClockProvider clockProvider;
    private final ApplianceCatalogRepository applianceCatalogRepository;

    public RegisterHomeUseCase(HomeRepository homeRepository, BillingAccountRepository billingAccountRepository,
                                HomeLiveStatePort homeLiveStatePort, ApplianceLiveStatePort applianceLiveStatePort,
                                AssetRegistrationPublisher assetRegistrationPublisher,
                                HomeAccessService homeAccessService, ClockProvider clockProvider,
                                ApplianceCatalogRepository applianceCatalogRepository) {
        this.homeRepository = homeRepository;
        this.billingAccountRepository = billingAccountRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.assetRegistrationPublisher = assetRegistrationPublisher;
        this.homeAccessService = homeAccessService;
        this.clockProvider = clockProvider;
        this.applianceCatalogRepository = applianceCatalogRepository;
    }

    @Transactional
    public Home execute(RegisterHomeCommand command) {
        Instant now = clockProvider.now();
        Home home = Home.register(command.name(), command.contactEmail(), command.energyQuotaKwh(),
                command.budgetQuotaTry(), command.baseTariffPerKwh(), command.penaltyTariffPerKwh(), now);

        for (RegisterHomeCommand.ApplianceCommand applianceCommand : command.appliances()) {
            home.addAppliance(buildAppliance(home.id(), applianceCommand));
        }

        homeRepository.save(home);

        String billingPeriod = BillingPeriodResolver.currentPeriod(now);
        billingAccountRepository.save(BillingAccount.open(home.id(), billingPeriod, now));

        homeAccessService.grantOwnership(home.id(), command.ownerUserId(), now);

        assetRegistrationPublisher.publish(home);

        initializeLiveState(home, now);

        return home;
    }

    private Appliance buildAppliance(UUID homeId, RegisterHomeCommand.ApplianceCommand applianceCommand) {
        if (applianceCommand.catalogItemId() == null) {
            return Appliance.create(homeId, applianceCommand.name(), applianceCommand.type(),
                    applianceCommand.safePowerLimitWatt(), applianceCommand.simulationMinWatt(),
                    applianceCommand.simulationMaxWatt());
        }

        ApplianceCatalogItem catalogItem = applianceCatalogRepository.findEnabledById(applianceCommand.catalogItemId())
                .orElseThrow(() -> new InvalidCatalogSelectionException(
                        "Unknown or disabled appliance catalog item: " + applianceCommand.catalogItemId()));

        if (!catalogItem.code().value().equals(applianceCommand.type())) {
            throw new InvalidCatalogSelectionException(
                    "Appliance type '" + applianceCommand.type() + "' does not match catalog item code '"
                            + catalogItem.code().value() + "'");
        }

        BigDecimal safePowerLimitWatt = applianceCommand.safePowerLimitWatt() != null
                ? applianceCommand.safePowerLimitWatt() : catalogItem.defaultSafePowerLimitWatt();
        BigDecimal simulationMinWatt = applianceCommand.simulationMinWatt() != null
                ? applianceCommand.simulationMinWatt() : catalogItem.defaultActiveMinWatt();
        BigDecimal simulationMaxWatt = applianceCommand.simulationMaxWatt() != null
                ? applianceCommand.simulationMaxWatt() : catalogItem.defaultActiveMaxWatt();

        return Appliance.createFromCatalog(homeId, applianceCommand.name(), catalogItem, safePowerLimitWatt,
                simulationMinWatt, simulationMaxWatt);
    }

    private void initializeLiveState(Home home, Instant now) {
        homeLiveStatePort.initialize(HomeLiveState.zero(home.id(), home.name(), now));
        for (Appliance appliance : home.appliances()) {
            applianceLiveStatePort.initialize(ApplianceLiveState.zero(home.id(), appliance.id(), appliance.name(),
                    appliance.type(), appliance.safePowerLimitWatt(), now));
        }
    }
}
