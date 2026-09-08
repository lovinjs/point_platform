package com.core.coreboot.platform.customer.controller;

import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import com.core.coreboot.platform.customer.service.CustomerTransactionQueryService;
import com.core.coreboot.platform.point.model.CustomerPointBalanceView;
import com.core.coreboot.platform.point.model.CustomerPointLedgerView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerTransactionControllerTest {
    @Mock
    private CustomerTransactionQueryService queryService;
    @InjectMocks
    private CustomerTransactionController controller;

    @Test
    void shouldDeriveCustomerIdForBalanceAndLedgerQueries() {
        CustomerPrincipal principal = new CustomerPrincipal(7L, 0, CustomerStatus.ACTIVE);
        CustomerPointBalanceView balance = new CustomerPointBalanceView(970L, null);
        PageResult<CustomerPointLedgerView> page = new PageResult<>(
                1L,
                20L,
                0L,
                0L,
                false,
                List.of()
        );
        when(queryService.getPointBalance(7L)).thenReturn(balance);
        when(queryService.getPointLedger(7L, 1, 20)).thenReturn(page);

        var balanceResponse = controller.balance(principal);
        var ledgerResponse = controller.pointLedger(1, 20, principal);

        verify(queryService).getPointBalance(7L);
        verify(queryService).getPointLedger(7L, 1, 20);
        assertEquals(balance, balanceResponse.getData());
        assertEquals(page, ledgerResponse.getData());
    }
}
