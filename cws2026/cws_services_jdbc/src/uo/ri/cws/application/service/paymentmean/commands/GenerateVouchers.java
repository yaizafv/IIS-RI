package uo.ri.cws.application.service.paymentmean.commands;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.EligibleRecord;

public class GenerateVouchers implements Command<int[]> {

    @Override
    public int[] execute() {
        VoucherGateway vouchers = Factories.persistence.forVoucher();
        int workOrderVouchers = generateWorkOrderVouchers(vouchers);
        int recommendationVouchers = generateRecommendationVouchers(vouchers);
        int invoiceVouchers = generateInvoiceVouchers(vouchers);
        return new int[] {
                workOrderVouchers, recommendationVouchers, invoiceVouchers
        };
    }

    private int generateWorkOrderVouchers(VoucherGateway vouchers) {
        List<EligibleRecord> eligible = vouchers.findEligibleWorkOrders();
        Map<String, List<String>> byClient = groupByClient(eligible);
        int generated = 0;
        for (Map.Entry<String, List<String>> entry : byClient.entrySet()) {
            List<String> ids = entry.getValue();
            for (int i = 0; i + 2 < ids.size(); i += 3) {
                vouchers.markWorkOrderUsed(ids.get(i));
                vouchers.markWorkOrderUsed(ids.get(i + 1));
                vouchers.markWorkOrderUsed(ids.get(i + 2));
                vouchers.addGenerated(entry.getKey(), new BigDecimal("20.00"),
                        "Por tres órdenes de trabajo");
                generated++;
            }
        }
        return generated;
    }

    private int generateRecommendationVouchers(VoucherGateway vouchers) {
        List<EligibleRecord> eligible = vouchers.findEligibleRecommendations();
        Map<String, List<String>> byClient = groupByClient(eligible);
        int generated = 0;
        for (Map.Entry<String, List<String>> entry : byClient.entrySet()) {
            List<String> ids = entry.getValue();
            for (int i = 0; i + 2 < ids.size(); i += 3) {
                vouchers.markRecommendationUsed(ids.get(i));
                vouchers.markRecommendationUsed(ids.get(i + 1));
                vouchers.markRecommendationUsed(ids.get(i + 2));
                vouchers.addGenerated(entry.getKey(), new BigDecimal("25.00"),
                        "Por recomendación");
                generated++;
            }
        }
        return generated;
    }

    private int generateInvoiceVouchers(VoucherGateway vouchers) {
        int generated = 0;
        List<EligibleRecord> invoices = vouchers.findEligibleInvoices();
        for (EligibleRecord invoice : invoices) {
            vouchers.markInvoiceUsed(invoice.itemId());
            vouchers.addGenerated(invoice.clientId(), new BigDecimal("30.00"),
                    "Por factura de más de 500");
            generated++;
        }
        return generated;
    }

    private Map<String, List<String>> groupByClient(List<EligibleRecord> records) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (EligibleRecord record : records) {
            List<String> ids = result.get(record.clientId());
            if (ids == null) {
                ids = new ArrayList<>();
                result.put(record.clientId(), ids);
            }
            ids.add(record.itemId());
        }
        return result;
    }
}
