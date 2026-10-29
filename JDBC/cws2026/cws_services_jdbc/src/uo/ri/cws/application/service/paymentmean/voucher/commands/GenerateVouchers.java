package uo.ri.cws.application.service.paymentmean.voucher.commands;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        String currentClientId = null;
        List<WorkOrderRecord> batch = new ArrayList<>();

        for (WorkOrderRecord workOrder : workOrders.findNotUsedForVoucherByInvoiceState(PAID)) {
            String clientId = vehicles.findById(workOrder.vehicleId).get().clientId;
            if (!clientId.equals(currentClientId)) {
                currentClientId = clientId;
                batch.clear();
            }
            batch.add(workOrder);
            if (batch.size() == WORK_ORDERS_PER_VOUCHER) {
                markAsUsed(batch);
                createVoucher(clientId);
                generated++;
                batch.clear();
            }
        }
        return new int[] { generated, 0, 0 };
    }

    private void markAsUsed(List<WorkOrderRecord> batch) {
        for (WorkOrderRecord workOrder : batch) {
            workOrder.usedForVoucher = true;
            workOrder.updatedAt = LocalDateTime.now();
            workOrders.update(workOrder);
        }
    }

    private void createVoucher(String clientId) {
        PaymentMeanRecord parent = VoucherDtoAssembler.toPaymentMeanRecord(clientId);
        paymentMeans.add(parent);
        vouchers.add(VoucherDtoAssembler.toRecord(parent));
    }
}
