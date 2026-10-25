package uo.ri.cws.application.service.paymentmean.commands;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.VoucherRecord;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;

public class GenerateVouchers implements Command<int[]> {

    private static class EligibleWorkOrder {
        String clientId;
        String workOrderId;

        EligibleWorkOrder(String clientId, String workOrderId) {
            this.clientId = clientId;
            this.workOrderId = workOrderId;
        }
    }

    @Override
    public int[] execute() {
        WorkOrderGateway workOrders = Factories.persistence.forWorkOrder();
        VoucherGateway vouchers = Factories.persistence.forVoucher();
        PaymentMeanGateway paymentMeans = Factories.persistence.forPaymentMean();

        int generated = generateFromWorkOrders(workOrders, vouchers, paymentMeans);
        return new int[] { generated, 0, 0 };
    }

    private int generateFromWorkOrders(WorkOrderGateway workOrders, VoucherGateway vouchers,
            PaymentMeanGateway paymentMeans) {
        List<WorkOrderRecord> eligible = workOrders.findEligibleForVoucher();
        List<EligibleWorkOrder> eligibleList = convertToEligible(eligible);
        Map<String, List<String>> byClientId = groupByClientId(eligibleList);

        int generated = 0;
        for (Map.Entry<String, List<String>> entry : byClientId.entrySet()) {
            String clientId = entry.getKey();
            List<String> workOrderIds = entry.getValue();

            for (int i = 0; i + 2 < workOrderIds.size(); i += 3) {
                workOrders.markUsedForVoucher(workOrderIds.get(i));
                workOrders.markUsedForVoucher(workOrderIds.get(i + 1));
                workOrders.markUsedForVoucher(workOrderIds.get(i + 2));

                createVoucher(clientId, vouchers, paymentMeans);
                generated++;
            }
        }
        return generated;
    }

    private List<EligibleWorkOrder> convertToEligible(List<WorkOrderRecord> records) {
        List<EligibleWorkOrder> result = new ArrayList<>();
        for (WorkOrderRecord record : records) {
            result.add(new EligibleWorkOrder(record.vehicleId, record.id));
        }
        return result;
    }

    private void createVoucher(String clientId, VoucherGateway vouchers, PaymentMeanGateway paymentMeans) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        PaymentMeanRecord parentRecord = new PaymentMeanRecord();
        parentRecord.id = id;
        parentRecord.createdAt = now;
        parentRecord.updatedAt = now;
        parentRecord.version = 1;
        parentRecord.entityState = "ENABLED";
        parentRecord.accumulated = BigDecimal.ZERO;
        parentRecord.clientId = clientId;
        parentRecord.dtype = "Voucher";

        paymentMeans.add(parentRecord);

        VoucherRecord voucherRecord = new VoucherRecord();
        voucherRecord.id = id;
        voucherRecord.createdAt = now;
        voucherRecord.updatedAt = now;
        voucherRecord.version = 1;
        voucherRecord.entityState = "ENABLED";
        voucherRecord.accumulated = BigDecimal.ZERO;
        voucherRecord.clientId = clientId;
        voucherRecord.description = "Por tres órdenes de trabajo";
        voucherRecord.available = new BigDecimal("20.00");
        voucherRecord.code = "V-" + UUID.randomUUID();

        vouchers.add(voucherRecord);
    }

    private Map<String, List<String>> groupByClientId(List<EligibleWorkOrder> records) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (EligibleWorkOrder record : records) {
            result.computeIfAbsent(record.clientId, k -> new ArrayList<>()).add(record.workOrderId);
        }
        return result;
    }

}
