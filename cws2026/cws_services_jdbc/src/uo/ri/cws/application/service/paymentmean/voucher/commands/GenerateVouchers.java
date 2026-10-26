package uo.ri.cws.application.service.paymentmean.voucher.commands;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.vehicle.VehicleGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;
import uo.ri.cws.application.service.paymentmean.voucher.VoucherDtoAssembler;

public class GenerateVouchers implements Command<int[]> {

    private static final String PAID = "PAID";
    private static final int WORK_ORDERS_PER_VOUCHER = 3;

    private final WorkOrderGateway workOrders = Factories.persistence.forWorkOrder();
    private final VehicleGateway vehicles = Factories.persistence.forVehicle();
    private final VoucherGateway vouchers = Factories.persistence.forVoucher();
    private final PaymentMeanGateway paymentMeans = Factories.persistence.forPaymentMean();

    @Override
    public int[] execute() {
        int generated = 0;
        Map<String, List<WorkOrderRecord>> byClientId = groupByClientId(
            workOrders.findNotUsedForVoucherByInvoiceState(PAID));

        for (Map.Entry<String, List<WorkOrderRecord>> entry : byClientId.entrySet()) {
            List<WorkOrderRecord> clientOrders = entry.getValue();
            for (int i = 0; i + WORK_ORDERS_PER_VOUCHER <= clientOrders.size();
                i += WORK_ORDERS_PER_VOUCHER) {
                for (int j = i; j < i + WORK_ORDERS_PER_VOUCHER; j++) {
                    WorkOrderRecord workOrder = clientOrders.get(j);
                    workOrder.usedForVoucher = true;
                    workOrder.updatedAt = LocalDateTime.now();
                    workOrders.update(workOrder);
                }
                createVoucher(entry.getKey());
                generated++;
            }
        }
        return new int[] { generated, 0, 0 };
    }

    private Map<String, List<WorkOrderRecord>> groupByClientId(List<WorkOrderRecord> records) {
        Map<String, List<WorkOrderRecord>> result = new LinkedHashMap<>();
        for (WorkOrderRecord record : records) {
            String clientId = vehicles.findById(record.vehicleId).get().clientId;
            if (!result.containsKey(clientId)) {
                result.put(clientId, new ArrayList<WorkOrderRecord>());
            }
            result.get(clientId).add(record);
        }
        return result;
    }

    private void createVoucher(String clientId) {
        PaymentMeanRecord parent = VoucherDtoAssembler.toPaymentMeanRecord(clientId);
        paymentMeans.add(parent);
        vouchers.add(VoucherDtoAssembler.toRecord(parent));
    }
}
