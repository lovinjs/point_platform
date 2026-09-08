package com.core.coreboot.platform.settlement.service;

import com.core.coreboot.platform.common.enums.StoreSettlementStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.settlement.model.SettlementConfirmCommand;
import com.core.coreboot.platform.settlement.model.SettlementGenerateCommand;
import com.core.coreboot.platform.settlement.model.SettlementGenerationResult;
import com.core.coreboot.platform.settlement.model.SettlementPaymentCommand;
import com.core.coreboot.platform.settlement.model.StoreSettlementDetailView;
import com.core.coreboot.platform.settlement.model.StoreSettlementSummaryView;

public interface SettlementService {
    SettlementGenerationResult generate(SettlementGenerateCommand command);

    PageResult<StoreSettlementSummaryView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            String periodCode,
            StoreSettlementStatus status
    );

    StoreSettlementDetailView detail(Long operatorId, String settlementNo);

    StoreSettlementDetailView confirm(SettlementConfirmCommand command);

    StoreSettlementDetailView markPaid(SettlementPaymentCommand command);
}
