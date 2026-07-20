package com.vegawatt.core.billing.domain;

import java.util.Optional;
import java.util.UUID;

public interface BillingAccountRepository {

    BillingAccount save(BillingAccount billingAccount);

    Optional<BillingAccount> findByHomeId(UUID homeId);
}
