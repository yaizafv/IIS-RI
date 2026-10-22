package uo.ri.cws.application.service;

import uo.ri.cws.application.service.client.ClientCrudService;
import uo.ri.cws.application.service.client.ClientCrudServiceImpl;
import uo.ri.cws.application.service.client.ClientHistoryService;
import uo.ri.cws.application.service.invoice.InvoicingService;
import uo.ri.cws.application.service.invoice.create.InvoicingServiceImpl;
import uo.ri.cws.application.service.mechanic.MechanicCrudService;
import uo.ri.cws.application.service.mechanic.crud.MechanicCrudServiceImpl;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudServiceImpl;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationServiceImpl;
import uo.ri.cws.application.service.spare.SparePartCrudService;
import uo.ri.cws.application.service.vehicle.VehicleCrudService;
import uo.ri.cws.application.service.vehicletype.VehicleTypeCrudService;
import uo.ri.cws.application.service.workorder.CloseWorkOrderService;
import uo.ri.cws.application.service.workorder.ViewAssignedWorkOrdersService;
import uo.ri.cws.application.service.workorder.WorkOrderCrudService;
import uo.ri.util.exception.NotYetImplementedException;

public class ServiceFactoryImpl implements ServiceFactory {

    @Override
    public VehicleCrudService forVehicleCrudService() {
        throw new NotYetImplementedException();
    }

    @Override
    public ClientCrudService forClientCrudService() {
        return new ClientCrudServiceImpl();
    }

    @Override
    public ClientHistoryService forClientHistoryService() {
        throw new NotYetImplementedException();
    }

    @Override
    public WorkOrderCrudService forWorkOrderCrudService() {
        throw new NotYetImplementedException();
    }

    @Override
    public CloseWorkOrderService forClosingWorkOrder() {
        throw new NotYetImplementedException();
    }

    @Override
    public ViewAssignedWorkOrdersService forViewAssignedWorkOrdersService() {
        throw new NotYetImplementedException();
    }

    @Override
    public MechanicCrudService forMechanicCrudService() {
        return new MechanicCrudServiceImpl();
    }

    @Override
    public VehicleTypeCrudService forVehicleTypeCrudService() {
        throw new NotYetImplementedException();
    }

    @Override
    public SparePartCrudService forSparePartCrudService() {
        throw new NotYetImplementedException();
    }

    @Override
    public InvoicingService forCreateInvoiceService() {
        return new InvoicingServiceImpl();
    }

    @Override
    public VoucherGenerationService forVoucherService() {
        return new VoucherGenerationServiceImpl();
    }

    @Override
    public PaymentMeanCrudService forPaymentMeanCrudService() {
        return new PaymentMeanCrudServiceImpl();
    }

}
