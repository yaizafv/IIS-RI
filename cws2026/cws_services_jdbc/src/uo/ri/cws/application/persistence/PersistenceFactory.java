package uo.ri.cws.application.persistence;

import uo.ri.cws.application.persistence.intervention.InterventionGateway;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.persistence.charge.ChargeGateway;
import uo.ri.cws.application.persistence.card.CardGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;

public interface PersistenceFactory {
		 
		MechanicGateway forMechanic();
		WorkOrderGateway forWorkOrder();
		InvoiceGateway forInvoice();
		InterventionGateway forIntervention();
		PaymentMeanGateway forPaymentMean();
		VoucherGateway forVoucher();
		ChargeGateway forCharge();
		CardGateway forCard();
		
//		SparePartGateway forSparePart();
//		SubstitutionGateway forSubstitutionsGateway();
//		VehicleGateway forVehicle();
//		VehicleTypeGateway forVehicleType();

}
