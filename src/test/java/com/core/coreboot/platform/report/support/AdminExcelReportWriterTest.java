package com.core.coreboot.platform.report.support;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.FundReceiver;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.RechargeChannel;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeRefundStatus;
import com.core.coreboot.platform.common.enums.RefundMethod;
import com.core.coreboot.platform.common.enums.SettlementStatus;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.finance.model.AdminFinancialReconciliationView;
import com.core.coreboot.platform.finance.model.AdminFinancialStoreView;
import com.core.coreboot.platform.finance.model.AdminFinancialSummaryView;
import com.core.coreboot.platform.order.model.AdminConsumptionOrderView;
import com.core.coreboot.platform.order.model.AdminRechargeOrderView;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminExcelReportWriterTest {
    private final AdminExcelReportWriter writer = new AdminExcelReportWriter();

    @Test
    void shouldCreateReadableFinancialWorkbookWithTypedAmounts() throws Exception {
        AdminFinancialSummaryView summary = new AdminFinancialSummaryView(
                100_000, 1_000, 20_000, 200, 80_000,
                30_000, 300, 1_500, 28_500, 9_500, 9_500, 9_500, 8_000
        );
        AdminFinancialStoreView store = new AdminFinancialStoreView(
                2L, "STORE-2", "测试门店", StoreStatus.ACTIVE,
                100_000, 1_000, 20_000, 200, 80_000,
                30_000, 300, 1_500, 28_500, 9_500, 9_500, 9_500, 8_000
        );
        AdminFinancialReconciliationView report = new AdminFinancialReconciliationView(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 10),
                LocalDateTime.of(2026, 9, 10, 11, 30),
                summary,
                List.of(store)
        );

        byte[] bytes = writer.writeFinancialReconciliation(report, "测试门店（STORE-2）");

        assertTrue(bytes.length > 1_000);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(1, workbook.getNumberOfSheets());
            var sheet = workbook.getSheet("财务对账");
            assertNotNull(sheet);
            assertEquals("平台财务对账报表", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC, sheet.getRow(6).getCell(0).getCellType());
            assertEquals(1_000.0, sheet.getRow(6).getCell(0).getNumericCellValue());
            assertEquals("STORE-2", sheet.getRow(10).getCell(0).getStringCellValue());
            assertEquals(285.0, sheet.getRow(10).getCell(11).getNumericCellValue());
            assertNotNull(sheet.getPaneInformation());
        }
    }

    @Test
    void shouldCreateReadableOrderWorkbooksWithTypedDatesMoneyAndPercentages() throws Exception {
        LocalDateTime createdTime = LocalDateTime.of(2026, 9, 9, 9, 30);
        AdminRechargeOrderView recharge = new AdminRechargeOrderView(
                "RCH-1", 7L, "13800138000", "测试用户", 2L, "STORE-2", "测试门店",
                100, 10_000, RechargeChannel.OFFLINE, PaymentMethod.BANK_TRANSFER,
                FundReceiver.PLATFORM, "BANK-1", RechargeOrderStatus.REFUNDED, 9L, "店长甲",
                "线下充值", createdTime, createdTime, createdTime, "RF-1", RefundMethod.BANK_TRANSFER,
                "BANK-RF-1", RechargeRefundStatus.COMPLETED, "客户申请退款", createdTime.plusHours(1)
        );
        AdminConsumptionOrderView consumption = new AdminConsumptionOrderView(
                "CSM-1", 7L, "13800138000", "测试用户", 2L, "STORE-2", "测试门店",
                30, 3_000, 500, 150, 2_850, ConsumptionVerificationMode.CUSTOMER_PIN,
                ConsumptionOrderStatus.COMPLETED, SettlementStatus.NOT_INCLUDED, 9L, "店长甲", "消费",
                createdTime.plusMinutes(5), createdTime.plusMinutes(2), createdTime.plusMinutes(2), createdTime
        );

        byte[] rechargeBytes = writer.writeRechargeOrders(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10),
                "测试门店", "无额外筛选", List.of(recharge)
        );
        byte[] consumptionBytes = writer.writeConsumptionOrders(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10),
                "测试门店", "无额外筛选", List.of(consumption)
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(rechargeBytes))) {
            var row = workbook.getSheet("充值订单").getRow(5);
            assertEquals(CellType.NUMERIC, row.getCell(0).getCellType());
            assertEquals("13800138000", row.getCell(4).getStringCellValue());
            assertEquals(100.0, row.getCell(8).getNumericCellValue());
            assertFalse(row.getCell(8).getCellStyle().getDataFormatString().isBlank());
        }
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(consumptionBytes))) {
            var row = workbook.getSheet("消费订单").getRow(5);
            assertEquals(30.0, row.getCell(8).getNumericCellValue());
            assertEquals(0.05, row.getCell(10).getNumericCellValue(), 0.000001);
            assertEquals("0.00%", row.getCell(10).getCellStyle().getDataFormatString());
        }
    }
}
