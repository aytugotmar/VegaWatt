package com.vegawatt.core.home.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.AssetRegistrationPublisher;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.home.domain.HomeRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds a single appliance to an already-registered home — the "I forgot a device" flow,
 * as opposed to {@link RegisterHomeUseCase} which seeds appliances at home creation time. */
@Service
public class AddApplianceUseCase {

    private final HomeRepository homeRepository;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final AssetRegistrationPublisher assetRegistrationPublisher;
    private final ApplianceFactory applianceFactory;
    private final ClockProvider clockProvider;

    public AddApplianceUseCase(HomeRepository homeRepository, ApplianceLiveStatePort applianceLiveStatePort,
                                AssetRegistrationPublisher assetRegistrationPublisher,
                                ApplianceFactory applianceFactory, ClockProvider clockProvider) {
        this.homeRepository = homeRepository;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.assetRegistrationPublisher = assetRegistrationPublisher;
        this.applianceFactory = applianceFactory;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public Appliance execute(AddApplianceCommand command) {
        Home home = homeRepository.findById(command.homeId())
                .orElseThrow(() -> new HomeNotFoundException(command.homeId()));

        Appliance appliance = applianceFactory.build(home.id(), command.name(), command.type(),
                command.safePowerLimitWatt(), command.simulationMinWatt(), command.simulationMaxWatt(),
                command.catalogItemId());
        home.addAppliance(appliance);
        homeRepository.save(home);

        Instant now = clockProvider.now();
        ApplianceCatalogView catalogView = applianceFactory.resolveCatalogView(appliance);
        applianceLiveStatePort.initialize(ApplianceLiveState.zero(home.id(), appliance.id(), appliance.name(),
                appliance.type(), appliance.safePowerLimitWatt(), now, catalogView.catalogCode(),
                catalogView.catalogDisplayName(), catalogView.catalogIconKey()));

        // Republishes the full home (existing appliances included) rather than a single-appliance
        // event — the sensors' registration consumer treats upsert/ensureScheduled as idempotent
        // by design (see RegistrationEventConsumer), so this is safe and avoids a second event shape.
        assetRegistrationPublisher.publish(home);

        return appliance;
    }
}
