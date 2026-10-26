package uo.ri.cws.application.persistence.workorder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface WorkOrderGateway extends Gateway<WorkOrderGateway.WorkOrderRecord> {

    List<WorkOrderRecord> findNotUsedForVoucherByInvoiceState(String invoiceState) throws PersistenceException;

    List<WorkOrderRecord> findByMechanicId(String mechanicId) throws PersistenceException;


    List<WorkOrderRecord> findNotInvoicedWorkOrdersByClientNif(String nif) throws PersistenceException;

    public static class WorkOrderRecord extends Gateway.BaseRecord {
        public String vehicleId;
        public String description;
        public LocalDateTime date;
        public BigDecimal amount;
        public String state;
        public boolean usedForVoucher;
        public String mechanicId;
        public String invoiceId;
    }
}
