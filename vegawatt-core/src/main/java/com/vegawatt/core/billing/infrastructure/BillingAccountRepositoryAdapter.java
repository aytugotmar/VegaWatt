package com.vegawatt.core.billing.infrastructure;

import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.Money;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class BillingAccountRepositoryAdapter implements BillingAccountRepository {

    private final BillingAccountJpaRepository jpaRepository;

    BillingAccountRepositoryAdapter(BillingAccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BillingAccount save(BillingAccount billingAccount) {
        BillingAccountEntity saved = jpaRepository.save(toEntity(billingAccount));
        return toDomain(saved);
    }

    @Override
    public Optional<BillingAccount> findByHomeIdAndBillingPeriod(UUID homeId, String billingPeriod) {
        return jpaRepository.findByHomeIdAndBillingPeriod(homeId, billingPeriod)
                .map(BillingAccountRepositoryAdapter::toDomain);
    }

    private static BillingAccountEntity toEntity(BillingAccount account) {
        return new BillingAccountEntity(account.id(), account.homeId(), account.billingPeriod(),
                account.accumulatedEnergyKwh(), account.accumulatedCost().amount(), account.penaltyActive(),
                account.energyQuota80Notified(), account.energyQuota100Notified(), account.budgetQuota80Notified(),
                account.budgetQuota100Notified(), account.version(), account.createdAt(), account.updatedAt());
    }

    private static BillingAccount toDomain(BillingAccountEntity entity) {
        return BillingAccount.reconstitute(entity.getId(), entity.getHomeId(), entity.getBillingPeriod(),
                entity.getAccumulatedEnergyKwh(), Money.of(entity.getAccumulatedCost()), entity.isPenaltyActive(),
                entity.isEnergyQuota80Notified(), entity.isEnergyQuota100Notified(),
                entity.isBudgetQuota80Notified(), entity.isBudgetQuota100Notified(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
