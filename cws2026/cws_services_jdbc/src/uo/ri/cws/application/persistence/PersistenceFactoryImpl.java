package uo.ri.cws.application.persistence;

import uo.ri.cws.application.persistence.intervention.InterventionGateway;
import uo.ri.cws.application.persistence.intervention.impl.InterventionGatewayImpl;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.invoice.impl.InvoiceGatewayImpl;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway;
import uo.ri.cws.application.persistence.mechanic.impl.MechanicGatewayImpl;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.impl.PaymentMeanGatewayImpl;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.persistence.voucher.impl.VoucherGatewayImpl;
import uo.ri.cws.application.persistence.charge.ChargeGateway;
import uo.ri.cws.application.persistence.charge.impl.ChargeGatewayImpl;
import uo.ri.cws.application.persistence.card.CardGateway;
import uo.ri.cws.application.persistence.card.impl.CardGatewayImpl;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.impl.WorkOrderGatewayImpl;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.client.impl.ClientGatewayImpl;
import uo.ri.cws.application.persistence.recommendation.RecommendationGateway;
import uo.ri.cws.application.persistence.recommendation.impl.RecommendationGatewayImpl;
import uo.ri.cws.application.persistence.cash.CashGateway;
import uo.ri.cws.application.persistence.cash.impl.CashGatewayImpl;
import uo.ri.cws.application.persistence.vehicle.VehicleGateway;
import uo.ri.cws.application.persistence.vehicle.impl.VehicleGatewayImpl;

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
        return new InvoiceGatewayImpl();
    }

    @Override
    public InterventionGateway forIntervention() {
        return new InterventionGatewayImpl();
    }

    @Override
    public PaymentMeanGateway forPaymentMean() {
        return new PaymentMeanGatewayImpl();
    }

    @Override
    public VoucherGateway forVoucher() {
        return new VoucherGatewayImpl();
    }

    @Override
    public ChargeGateway forCharge() {
        return new ChargeGatewayImpl();
    }

    @Override
    public CardGateway forCard() {
        return new CardGatewayImpl();
    }

    @Override
    public ClientGateway forClient() {
        return new ClientGatewayImpl();
    }

    @Override
    public RecommendationGateway forRecommendation() {
        return new RecommendationGatewayImpl();
    }

    @Override
    public CashGateway forCash() {
        return new CashGatewayImpl();
    }

    @Override
    public VehicleGateway forVehicle() {
        return new VehicleGatewayImpl();
    }

}
