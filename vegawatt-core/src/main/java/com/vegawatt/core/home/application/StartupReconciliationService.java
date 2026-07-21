package com.vegawatt.core.home.application;

import com.vegawatt.core.billing.application.EvaluateHomeBillingUseCase;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.AssetRegistrationPublisher;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.common.time.ClockProvider;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs once on application startup. If Ignite came up cold (a fresh container, or one that lost
 * its data), rebuilds each home's and appliance's live state from PostgreSQL's permanent ledger.
 * Then, regardless of whether Ignite was cold, re-publishes every home's asset-registration event —
 * this is what lets the telemetry-sensors simulator recover after its own restart (it only keeps
 * registered homes in memory): the sensor's registration consumer is idempotent
 * (HomeRegistry.upsert + a computeIfAbsent-guarded scheduler), so replaying is always safe.
 */
@Component
public class StartupReconciliationService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupReconciliationService.class);

    private final HomeRepository homeRepository;
    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final EvaluateHomeBillingUseCase evaluateHomeBillingUseCase;
    private final AssetRegistrationPublisher assetRegistrationPublisher;
    private final ClockProvider clockProvider;

    public StartupReconciliationService(HomeRepository homeRepository, HomeLiveStatePort homeLiveStatePort,
                                         ApplianceLiveStatePort applianceLiveStatePort,
                                         EvaluateHomeBillingUseCase evaluateHomeBillingUseCase,
                                         AssetRegistrationPublisher assetRegistrationPublisher,
                                         ClockProvider clockProvider) {
        this.homeRepository = homeRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.evaluateHomeBillingUseCase = evaluateHomeBillingUseCase;
        this.assetRegistrationPublisher = assetRegistrationPublisher;
        this.clockProvider = clockProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        Instant now = clockProvider.now();
        int reconciledHomes = 0;

        for (Home home : homeRepository.findAll()) {
            try {
                reconcileHome(home, now);
                reconciledHomes++;
            } catch (RuntimeException e) {
                log.error("Failed to reconcile home {} on startup", home.id(), e);
            }
        }

        log.info("Startup reconciliation complete: {} home(s) processed", reconciledHomes);
    }

    private void reconcileHome(Home home, Instant now) {
        if (homeLiveStatePort.get(home.id()).isEmpty()) {
            HomeLiveState recovered = evaluateHomeBillingUseCase.recoverFromLedger(home, now);
            homeLiveStatePort.initialize(recovered);
            log.info("Recovered cold Ignite home state for {} from the PostgreSQL ledger", home.id());
        }

        for (Appliance appliance : home.appliances()) {
            if (applianceLiveStatePort.get(home.id(), appliance.id()).isEmpty()) {
                applianceLiveStatePort.initialize(ApplianceLiveState.zero(home.id(), appliance.id(),
                        appliance.name(), appliance.type(), appliance.safePowerLimitWatt(), now));
            }
        }

        assetRegistrationPublisher.publish(home);
    }
}
