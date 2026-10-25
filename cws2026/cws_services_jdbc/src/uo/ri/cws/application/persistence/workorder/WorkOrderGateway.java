package uo.ri.cws.application.persistence.workorder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface WorkOrderGateway extends Gateway<WorkOrderGateway.WorkOrderRecord> {

    List<WorkOrderRecord> findByIds(List<String> workOrderIds);

    List<WorkOrderRecord> findByVehicleId(String id);

    List<WorkOrderRecord> findByMechanicId(String id);

    List<WorkOrderRecord> findByState(String state);

    public static class WorkOrderRecord extends Gateway.BaseRecord {

        public String id;
        public long version;

        public String vehicleId;
        public String description;
        public LocalDateTime date;
        public BigDecimal amount;
        public String state;

        public String mechanicId;
        public String invoiceId;
    }

    List<WorkOrderRecord> findNotInvoicedWorkOrdersByClientNif(String nif);

    List<WorkOrderRecord> findByClientNif(String nif);

    void setInvoiced(String workOrderId, String invoiceId);

    List<WorkOrderRecord> findEligibleForVoucher() throws PersistenceException;

    void markUsedForVoucher(String id) throws PersistenceException;
}
