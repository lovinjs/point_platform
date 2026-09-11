package com.core.coreboot.platform.report.support;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.RechargeChannel;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RefundMethod;
import com.core.coreboot.platform.common.enums.SettlementStatus;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.finance.model.AdminFinancialReconciliationView;
import com.core.coreboot.platform.finance.model.AdminFinancialStoreView;
import com.core.coreboot.platform.finance.model.AdminFinancialSummaryView;
import com.core.coreboot.platform.order.model.AdminConsumptionOrderView;
import com.core.coreboot.platform.order.model.AdminRechargeOrderView;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class AdminExcelReportWriter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] writeFinancialReconciliation(
            AdminFinancialReconciliationView report,
            String scopeLabel
    ) {
        try (XSSFWorkbook workbook = createWorkbook("平台财务对账报表");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = createStyles(workbook);
            XSSFSheet sheet = createSheet(workbook, "财务对账");
            int columnCount = 16;

            writeTitle(sheet, styles, "平台财务对账报表", columnCount);
            writeMergedMeta(sheet, styles, 1,
                    "统计日期：" + DATE.format(report.startDate()) + " 至 " + DATE.format(report.endDate())
                            + "    门店范围：" + safe(scopeLabel), columnCount);
            writeMergedMeta(sheet, styles, 2,
                    "数据生成时间：" + formatDateTime(report.generatedTime())
                            + "    说明：各项金额按实际收款、退款、消费完成或结算付款日期统计。",
                    columnCount);

            writeSection(sheet, styles, 4, "平台汇总", columnCount);
            String[] summaryHeaders = {
                    "充值实收(元)", "充值积分", "退款支出(元)", "退款积分", "充值净现金(元)",
                    "消费总额(元)", "消费积分", "平台手续费(元)", "门店应付(元)",
                    "待生成结算(元)", "已纳入结算单(元)", "已完成结算(元)", "期间实际付款(元)"
            };
            writeHeaderRow(sheet, styles, 5, summaryHeaders);
            AdminFinancialSummaryView summary = report.summary();
            Row summaryRow = sheet.createRow(6);
            writeMoney(summaryRow, 0, summary.rechargeReceiptCent(), styles.money());
            writeLong(summaryRow, 1, summary.rechargePoints(), styles.integer());
            writeMoney(summaryRow, 2, summary.refundOutflowCent(), styles.money());
            writeLong(summaryRow, 3, summary.refundPoints(), styles.integer());
            writeMoney(summaryRow, 4, summary.netRechargeCashCent(), styles.money());
            writeMoney(summaryRow, 5, summary.consumptionGrossCent(), styles.money());
            writeLong(summaryRow, 6, summary.consumptionPoints(), styles.integer());
            writeMoney(summaryRow, 7, summary.platformFeeCent(), styles.money());
            writeMoney(summaryRow, 8, summary.storePayableCent(), styles.money());
            writeMoney(summaryRow, 9, summary.notIncludedPayableCent(), styles.money());
            writeMoney(summaryRow, 10, summary.includedPayableCent(), styles.money());
            writeMoney(summaryRow, 11, summary.settledPayableCent(), styles.money());
            writeMoney(summaryRow, 12, summary.settlementPaidCent(), styles.money());

            writeSection(sheet, styles, 8, "门店对账明细", columnCount);
            String[] detailHeaders = {
                    "门店编码", "门店名称", "门店状态", "充值实收(元)", "充值积分", "退款支出(元)",
                    "退款积分", "充值净现金(元)", "消费总额(元)", "消费积分", "平台手续费(元)",
                    "门店应付(元)", "待生成结算(元)", "已纳入结算单(元)", "已完成结算(元)",
                    "期间实际付款(元)"
            };
            int detailHeaderRow = 9;
            writeHeaderRow(sheet, styles, detailHeaderRow, detailHeaders);
            int rowIndex = detailHeaderRow + 1;
            for (AdminFinancialStoreView store : report.stores()) {
                Row row = sheet.createRow(rowIndex++);
                int column = 0;
                writeText(row, column++, store.storeCode(), styles.text());
                writeText(row, column++, store.storeName(), styles.text());
                writeText(row, column++, storeStatusLabel(store.storeStatus()), styles.text());
                writeMoney(row, column++, store.rechargeReceiptCent(), styles.money());
                writeLong(row, column++, store.rechargePoints(), styles.integer());
                writeMoney(row, column++, store.refundOutflowCent(), styles.money());
                writeLong(row, column++, store.refundPoints(), styles.integer());
                writeMoney(row, column++, store.netRechargeCashCent(), styles.money());
                writeMoney(row, column++, store.consumptionGrossCent(), styles.money());
                writeLong(row, column++, store.consumptionPoints(), styles.integer());
                writeMoney(row, column++, store.platformFeeCent(), styles.money());
                writeMoney(row, column++, store.storePayableCent(), styles.money());
                writeMoney(row, column++, store.notIncludedPayableCent(), styles.money());
                writeMoney(row, column++, store.includedPayableCent(), styles.money());
                writeMoney(row, column++, store.settledPayableCent(), styles.money());
                writeMoney(row, column, store.settlementPaidCent(), styles.money());
            }
            setWidths(sheet, 16, 24, 12, 15, 13, 15, 13, 16, 15, 13, 16, 15, 17, 18, 17, 17);
            finishDetailTable(sheet, detailHeaderRow, columnCount, 3);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException | RuntimeException ex) {
            return handleFailure("财务对账", ex);
        }
    }

    public byte[] writeRechargeOrders(
            LocalDate startDate,
            LocalDate endDate,
            String scopeLabel,
            String filterLabel,
            List<AdminRechargeOrderView> orders
    ) {
        try (XSSFWorkbook workbook = createWorkbook("充值订单报表");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = createStyles(workbook);
            XSSFSheet sheet = createSheet(workbook, "充值订单");
            String[] headers = {
                    "创建时间", "订单号", "订单状态", "用户ID", "用户手机号", "用户昵称", "门店编码", "充值门店",
                    "充值金额(元)", "充值积分", "充值渠道", "收款方式", "收款账户", "支付参考号", "操作员ID",
                    "操作员", "支付时间", "完成时间", "备注", "退款单号", "退款状态", "退款方式", "退款参考号",
                    "退款完成时间", "退款原因"
            };
            int columnCount = headers.length;
            writeOrderMeta(sheet, styles, "充值订单报表", startDate, endDate, scopeLabel,
                    filterLabel, orders.size(), columnCount);
            int headerRow = 4;
            writeHeaderRow(sheet, styles, headerRow, headers);
            int rowIndex = headerRow + 1;
            for (AdminRechargeOrderView order : orders) {
                Row row = sheet.createRow(rowIndex++);
                int column = 0;
                writeDateTime(row, column++, order.createTime(), styles.dateTime());
                writeText(row, column++, order.orderNo(), styles.text());
                writeText(row, column++, rechargeStatusLabel(order.orderStatus()), styles.text());
                writeLong(row, column++, order.customerId(), styles.integer());
                writeText(row, column++, order.customerPhone(), styles.text());
                writeText(row, column++, order.customerNickname(), styles.text());
                writeText(row, column++, order.storeCode(), styles.text());
                writeText(row, column++, order.storeName(), styles.text());
                writeMoney(row, column++, order.amountCent(), styles.money());
                writeLong(row, column++, order.rechargePoints(), styles.integer());
                writeText(row, column++, rechargeChannelLabel(order.channel()), styles.text());
                writeText(row, column++, paymentMethodLabel(order.paymentMethod()), styles.text());
                writeText(row, column++, order.fundReceiver() == null ? null : "平台账户", styles.text());
                writeText(row, column++, order.paymentReference(), styles.text());
                writeLong(row, column++, order.operatorId(), styles.integer());
                writeText(row, column++, order.operatorName(), styles.text());
                writeDateTime(row, column++, order.paidTime(), styles.dateTime());
                writeDateTime(row, column++, order.completedTime(), styles.dateTime());
                writeText(row, column++, order.remark(), styles.wrappedText());
                writeText(row, column++, order.refundNo(), styles.text());
                writeText(row, column++, order.refundStatus() == null ? null : "退款完成", styles.text());
                writeText(row, column++, refundMethodLabel(order.refundMethod()), styles.text());
                writeText(row, column++, order.refundReference(), styles.text());
                writeDateTime(row, column++, order.refundCompletedTime(), styles.dateTime());
                writeText(row, column, order.refundReason(), styles.wrappedText());
            }
            setWidths(sheet, 20, 30, 14, 12, 18, 18, 16, 24, 16, 13, 14, 16, 14, 28,
                    12, 16, 20, 20, 30, 28, 14, 16, 28, 20, 30);
            finishDetailTable(sheet, headerRow, columnCount, 2);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException | RuntimeException ex) {
            return handleFailure("充值订单", ex);
        }
    }

    public byte[] writeConsumptionOrders(
            LocalDate startDate,
            LocalDate endDate,
            String scopeLabel,
            String filterLabel,
            List<AdminConsumptionOrderView> orders
    ) {
        try (XSSFWorkbook workbook = createWorkbook("消费订单报表");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = createStyles(workbook);
            XSSFSheet sheet = createSheet(workbook, "消费订单");
            String[] headers = {
                    "创建时间", "订单号", "订单状态", "用户ID", "用户手机号", "用户昵称", "门店编码", "消费门店",
                     "消费金额(元)", "消费积分", "平台费率", "平台手续费(元)", "门店应付(元)", "核销方式",
                     "结算状态", "操作员ID", "操作员", "过期时间", "用户确认时间", "完成时间",
                     "冲正操作员ID", "冲正时间", "冲正原因", "备注"
            };
            int columnCount = headers.length;
            writeOrderMeta(sheet, styles, "消费订单报表", startDate, endDate, scopeLabel,
                    filterLabel, orders.size(), columnCount);
            int headerRow = 4;
            writeHeaderRow(sheet, styles, headerRow, headers);
            int rowIndex = headerRow + 1;
            for (AdminConsumptionOrderView order : orders) {
                Row row = sheet.createRow(rowIndex++);
                int column = 0;
                writeDateTime(row, column++, order.createTime(), styles.dateTime());
                writeText(row, column++, order.orderNo(), styles.text());
                writeText(row, column++, consumptionStatusLabel(order.orderStatus()), styles.text());
                writeLong(row, column++, order.customerId(), styles.integer());
                writeText(row, column++, order.customerPhone(), styles.text());
                writeText(row, column++, order.customerNickname(), styles.text());
                writeText(row, column++, order.storeCode(), styles.text());
                writeText(row, column++, order.storeName(), styles.text());
                writeMoney(row, column++, order.amountCent(), styles.money());
                writeLong(row, column++, order.consumePoints(), styles.integer());
                writePercent(row, column++, order.platformFeeRateBps(), styles.percent());
                writeMoney(row, column++, order.platformFeeCent(), styles.money());
                writeMoney(row, column++, order.storePayableCent(), styles.money());
                writeText(row, column++, order.verificationMode() == null ? null : "消费密码", styles.text());
                writeText(row, column++, settlementStatusLabel(order.settlementStatus()), styles.text());
                writeLong(row, column++, order.operatorId(), styles.integer());
                writeText(row, column++, order.operatorName(), styles.text());
                writeDateTime(row, column++, order.expiresTime(), styles.dateTime());
                 writeDateTime(row, column++, order.confirmedTime(), styles.dateTime());
                 writeDateTime(row, column++, order.completedTime(), styles.dateTime());
                 writeLong(row, column++, order.reversedBy(), styles.integer());
                 writeDateTime(row, column++, order.reversedTime(), styles.dateTime());
                 writeText(row, column++, order.reversalReason(), styles.wrappedText());
                 writeText(row, column, order.remark(), styles.wrappedText());
             }
             setWidths(sheet, 20, 30, 16, 12, 18, 18, 16, 24, 16, 13, 13, 17, 16, 14,
                     16, 12, 16, 20, 20, 20, 14, 20, 30, 30);
            finishDetailTable(sheet, headerRow, columnCount, 2);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException | RuntimeException ex) {
            return handleFailure("消费订单", ex);
        }
    }

    private XSSFWorkbook createWorkbook(String title) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        workbook.getProperties().getCoreProperties().setCreator("积分平台");
        workbook.getProperties().getCoreProperties().setTitle(title);
        return workbook;
    }

    private XSSFSheet createSheet(XSSFWorkbook workbook, String name) {
        XSSFSheet sheet = workbook.createSheet(name);
        sheet.setDisplayGridlines(false);
        sheet.setZoom(90);
        return sheet;
    }

    private void writeOrderMeta(
            XSSFSheet sheet,
            Styles styles,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            String scopeLabel,
            String filterLabel,
            int rowCount,
            int columnCount
    ) {
        writeTitle(sheet, styles, title, columnCount);
        writeMergedMeta(sheet, styles, 1,
                "订单创建日期：" + DATE.format(startDate) + " 至 " + DATE.format(endDate)
                        + "    门店范围：" + safe(scopeLabel), columnCount);
        writeMergedMeta(sheet, styles, 2,
                "筛选条件：" + safe(filterLabel) + "    导出记录数：" + rowCount + " 条"
                        + "    导出时间：" + formatDateTime(LocalDateTime.now()),
                columnCount);
    }

    private void writeTitle(XSSFSheet sheet, Styles styles, String title, int columnCount) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(30);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(styles.title());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));
    }

    private void writeMergedMeta(
            XSSFSheet sheet,
            Styles styles,
            int rowIndex,
            String value,
            int columnCount
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22);
        Cell cell = row.createCell(0);
        cell.setCellValue(value);
        cell.setCellStyle(styles.meta());
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columnCount - 1));
    }

    private void writeSection(XSSFSheet sheet, Styles styles, int rowIndex, String title, int columnCount) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(styles.section());
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columnCount - 1));
    }

    private void writeHeaderRow(XSSFSheet sheet, Styles styles, int rowIndex, String[] headers) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(30);
        for (int column = 0; column < headers.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(styles.header());
        }
    }

    private void writeText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(safe(value));
        cell.setCellStyle(style);
    }

    private void writeLong(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void writeMoney(Row row, int column, long amountCent, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(amountCent / 100.0d);
        cell.setCellStyle(style);
    }

    private void writePercent(Row row, int column, int rateBps, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(rateBps / 10_000.0d);
        cell.setCellStyle(style);
    }

    private void writeDateTime(Row row, int column, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private Styles createStyles(XSSFWorkbook workbook) {
        CellStyle title = workbook.createCellStyle();
        title.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        title.setAlignment(HorizontalAlignment.CENTER);
        title.setVerticalAlignment(VerticalAlignment.CENTER);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleFont.setFontHeightInPoints((short) 16);
        title.setFont(titleFont);

        CellStyle meta = workbook.createCellStyle();
        meta.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        meta.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        meta.setVerticalAlignment(VerticalAlignment.CENTER);
        Font metaFont = workbook.createFont();
        metaFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        meta.setFont(metaFont);

        CellStyle section = workbook.createCellStyle();
        section.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        section.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font sectionFont = workbook.createFont();
        sectionFont.setBold(true);
        sectionFont.setColor(IndexedColors.WHITE.getIndex());
        section.setFont(sectionFont);

        CellStyle header = workbook.createCellStyle();
        header.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setWrapText(true);
        header.setBorderBottom(BorderStyle.THIN);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        header.setFont(headerFont);

        CellStyle text = workbook.createCellStyle();
        text.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle wrappedText = workbook.createCellStyle();
        wrappedText.cloneStyleFrom(text);
        wrappedText.setWrapText(true);

        CellStyle integer = workbook.createCellStyle();
        integer.setVerticalAlignment(VerticalAlignment.CENTER);
        integer.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

        CellStyle money = workbook.createCellStyle();
        money.setVerticalAlignment(VerticalAlignment.CENTER);
        money.setDataFormat(workbook.createDataFormat().getFormat("¥#,##0.00;[Red]-¥#,##0.00"));

        CellStyle percent = workbook.createCellStyle();
        percent.setVerticalAlignment(VerticalAlignment.CENTER);
        percent.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

        CellStyle dateTime = workbook.createCellStyle();
        dateTime.setVerticalAlignment(VerticalAlignment.CENTER);
        dateTime.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return new Styles(title, meta, section, header, text, wrappedText, integer, money, percent, dateTime);
    }

    private void setWidths(XSSFSheet sheet, int... widths) {
        for (int column = 0; column < widths.length; column++) {
            sheet.setColumnWidth(column, Math.min(widths[column], 255) * 256);
        }
    }

    private void finishDetailTable(XSSFSheet sheet, int headerRow, int columnCount, int frozenColumns) {
        sheet.createFreezePane(frozenColumns, headerRow + 1);
        int lastRow = Math.max(headerRow, sheet.getLastRowNum());
        sheet.setAutoFilter(new CellRangeAddress(headerRow, lastRow, 0, columnCount - 1));
    }

    private byte[] handleFailure(String reportName, Exception ex) {
        if (ex instanceof CustomException customException) {
            throw customException;
        }
        log.error("生成{}Excel报表失败", reportName, ex);
        throw new CustomException(ExceptionEnum.PLATFORM_REPORT_GENERATION_FAILED);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String storeStatusLabel(StoreStatus status) {
        return status == null ? "" : switch (status) {
            case PENDING -> "待启用";
            case ACTIVE -> "营业中";
            case SUSPENDED -> "已停用";
            case CLOSED -> "已关闭";
        };
    }

    private String rechargeStatusLabel(RechargeOrderStatus status) {
        return status == null ? "" : switch (status) {
            case CREATED -> "待支付";
            case COMPLETED -> "充值成功";
            case REFUNDED -> "已退款";
            case CANCELLED -> "已取消";
        };
    }

    private String consumptionStatusLabel(ConsumptionOrderStatus status) {
        return status == null ? "" : switch (status) {
            case PENDING_CONFIRM -> "待用户确认";
            case COMPLETED -> "消费成功";
            case CANCELLED -> "已取消";
            case EXPIRED -> "已过期";
            case REVERSED -> "已冲正";
        };
    }

    private String rechargeChannelLabel(RechargeChannel channel) {
        return channel == null ? "" : switch (channel) {
            case OFFLINE -> "线下充值";
            case WECHAT_PAY -> "微信支付";
        };
    }

    private String paymentMethodLabel(PaymentMethod method) {
        return method == null ? "" : switch (method) {
            case PLATFORM_QR -> "平台收款码";
            case BANK_TRANSFER -> "银行转账";
            case OTHER -> "其他线下收款";
        };
    }

    private String refundMethodLabel(RefundMethod method) {
        return method == null ? "" : switch (method) {
            case ORIGINAL_CHANNEL -> "原路退回";
            case BANK_TRANSFER -> "银行转账";
            case OTHER -> "其他方式";
        };
    }

    private String settlementStatusLabel(SettlementStatus status) {
        return status == null ? "" : switch (status) {
            case NOT_INCLUDED -> "待结算";
            case INCLUDED -> "已纳入结算单";
            case SETTLED -> "已结算";
            case ADJUSTED -> "已调整";
        };
    }

    private record Styles(
            CellStyle title,
            CellStyle meta,
            CellStyle section,
            CellStyle header,
            CellStyle text,
            CellStyle wrappedText,
            CellStyle integer,
            CellStyle money,
            CellStyle percent,
            CellStyle dateTime
    ) {
    }
}
