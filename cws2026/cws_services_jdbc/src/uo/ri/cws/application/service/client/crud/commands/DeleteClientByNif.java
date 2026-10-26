package uo.ri.cws.application.service.client.crud.commands;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.card.CardGateway;
import uo.ri.cws.application.persistence.cash.CashGateway;
import uo.ri.cws.application.persistence.charge.ChargeGateway;
import uo.ri.cws.application.persistence.charge.ChargeGateway.ChargeRecord;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.client.ClientGateway.ClientRecord;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.cws.application.persistence.recommendation.RecommendationGateway;
import uo.ri.cws.application.persistence.recommendation.RecommendationGateway.RecommendationRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.vehicle.VehicleGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class DeleteClientByNif implements Command<Void> {

    private static final String PAID = "PAID";
    private static final String CASH = "Cash";
    private static final String CREDIT_CARD = "CreditCard";
    private static final String VOUCHER = "Voucher";

    private final String nif;
    private final ClientGateway clients = Factories.persistence.forClient();
    private final VehicleGateway vehicles = Factories.persistence.forVehicle();
    private final RecommendationGateway recommendations = Factories.persistence.forRecommendation();
    private final PaymentMeanGateway paymentMeans = Factories.persistence.forPaymentMean();
    private final ChargeGateway charges = Factories.persistence.forCharge();
    private final InvoiceGateway invoices = Factories.persistence.forInvoice();
    private final CashGateway cash = Factories.persistence.forCash();
    private final CardGateway cards = Factories.persistence.forCard();
    private final VoucherGateway vouchers = Factories.persistence.forVoucher();

    public DeleteClientByNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        this.nif = nif;
    }

    @Override
    public Void execute() throws BusinessException {
        var found = clients.findByNif(nif);
        BusinessChecks.exists(found, "No existe el cliente");
        ClientRecord client = found.get();

        BusinessChecks.isTrue(vehicles.findByClientId(client.id).isEmpty(),
            "No se puede eliminar un cliente con vehículos");

        List<PaymentMeanRecord> means = paymentMeans.findByClientId(client.id);
        for (PaymentMeanRecord mean : means) {
            checkNoPaidInvoices(mean);
        }

        for (RecommendationRecord recommendation : recommendations.findByClientId(client.id)) {
            recommendations.remove(recommendation.id);
        }
        for (PaymentMeanRecord mean : means) {
            removeSpecialization(mean);
            paymentMeans.remove(mean.id);
        }
        clients.remove(client.id);
        return null;
    }

    private void checkNoPaidInvoices(PaymentMeanRecord mean) throws BusinessException {
        for (ChargeRecord charge : charges.findByPaymentMeanId(mean.id)) {
            var invoice = invoices.findById(charge.invoiceId);
            if (invoice.isPresent()) {
                InvoiceRecord record = invoice.get();
                BusinessChecks.isTrue(!PAID.equals(record.state),
                    "No se puede eliminar un cliente con facturas pagadas");
            }
        }
    }

    private void removeSpecialization(PaymentMeanRecord mean) {
        if (CASH.equals(mean.dtype)) {
            cash.remove(mean.id);
        } else if (CREDIT_CARD.equals(mean.dtype)) {
            cards.remove(mean.id);
        } else if (VOUCHER.equals(mean.dtype)) {
            vouchers.remove(mean.id);
        }
    }
}
