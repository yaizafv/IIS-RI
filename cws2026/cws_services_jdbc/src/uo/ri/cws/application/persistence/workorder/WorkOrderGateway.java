package uo.ri.cws.application.persistence.workorder;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface WorkOrderGateway extends Gateway<WorkOrderGateway.WorkOrderRecord> {

    /**
     * @param workorder Ids, list of workorders to retrieve
     * @return list of workorder dto whose id is included in the parameter,
     *         probably empty
     * @throws PersistenceException
     */
    List<WorkOrderRecord> findByIds(List<String> workOrderIds);

    /**
     * @return a list of all work orders for an specific vehicle id
     * @throws PersistenceException
     */
    List<WorkOrderRecord> findByVehicleId(String id);

    /**
     * @param a mechanic id
     * @return a list of all work orders assigned to this specific mechanic
     * @throws PersistenceException
     */
    List<WorkOrderRecord> findByMechanicId(String id);

    /**
     * @param the state
     * @return a list of all work orders in the specific state
     * 
     */
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
}
