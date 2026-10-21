package uo.ri.cws.application.service.paymentmean.commands;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;

public class GenerateVouchers implements Command<int[]> {

    @Override
    public int[] execute() {
        VoucherGateway vouchers = Factories.persistence.forVoucher();
        return new int[] {
                generateWorkOrderVouchers(vouchers),
                generateRecommendationVouchers(vouchers),
                generateInvoiceVouchers(vouchers)
        };
    }

    private static int generateWorkOrderVouchers(VoucherGateway vouchers) {
        return generateGrouped(vouchers.findEligibleWorkOrders(), vouchers,
                new BigDecimal("20.00"), "Por tres órdenes de trabajo",
                vouchers::markWorkOrderUsed);
    }

    private static int generateRecommendationVouchers(VoucherGateway vouchers) {
        return generateGrouped(vouchers.findEligibleRecommendations(), vouchers,
                new BigDecimal("25.00"), "Por recomendación",
                vouchers::markRecommendationUsed);
    }

    private static int generateGrouped(List<VoucherGateway.EligibleRecord> eligible,
            VoucherGateway vouchers, BigDecimal amount, String description,
            IdMarker marker) {
        int generated = 0;
        for (Map.Entry<String, List<String>> entry : group(eligible).entrySet()) {
            List<String> ids = entry.getValue();
            for (int offset = 0; offset + 2 < ids.size(); offset += 3) {
                for (String id : ids.subList(offset, offset + 3)) {
                    marker.mark(id);
                }
                vouchers.addGenerated(entry.getKey(), amount, description);
                generated++;
            }
        }
        return generated;
    }

    private static int generateInvoiceVouchers(VoucherGateway vouchers) {
        int generated = 0;
        for (VoucherGateway.EligibleRecord invoice :
                vouchers.findEligibleInvoices()) {
            vouchers.markInvoiceUsed(invoice.itemId());
            vouchers.addGenerated(invoice.clientId(), new BigDecimal("30.00"),
                    "Por factura de más de 500");
            generated++;
        }
        return generated;
    }

    private static Map<String, List<String>> group(
            List<VoucherGateway.EligibleRecord> records) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (VoucherGateway.EligibleRecord record : records) {
            result.computeIfAbsent(record.clientId(),
                    ignored -> new ArrayList<>()).add(record.itemId());
        }
        return result;
    }

    @FunctionalInterface
    private interface IdMarker {
        void mark(String id);
    }
}
