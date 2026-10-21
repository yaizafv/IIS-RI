package uo.ri.cws.application.persistence;

import uo.ri.cws.application.persistence.intervention.InterventionGateway;
import uo.ri.cws.application.persistence.intervention.impl.InterventionGatewayImpl;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway;
import uo.ri.cws.application.persistence.mechanic.impl.MechanicGatewayImpl;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.impl.WorkOrderGatewayImpl;

public class PersistenceFactoryImpl implements PersistenceFactory {

    @Override
    public MechanicGateway forMechanic() {
        return new MechanicGatewayImpl();
    }

    @Override
    public WorkOrderGateway forWorkOrder() {
        return new WorkOrderGatewayImpl();
    }

    @Override
    public InvoiceGateway forInvoice() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InterventionGateway forIntervention() {
        return new InterventionGatewayImpl();
    }

}
