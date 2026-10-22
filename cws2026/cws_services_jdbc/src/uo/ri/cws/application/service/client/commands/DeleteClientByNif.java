package uo.ri.cws.application.service.client.commands;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.cash.CashGateway;
import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.charge.ChargeGateway;
import uo.ri.cws.application.persistence.card.CardGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.recommendation.RecommendationGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class DeleteClientByNif implements Command<Void> {

    private final String nif;
    private final ClientGateway clients = Factories.persistence.forClient();
    private final RecommendationGateway recommendations =
            Factories.persistence.forRecommendation();
    private final ChargeGateway charges = Factories.persistence.forCharge();
    private final CashGateway cash = Factories.persistence.forCash();
    private final CardGateway cards = Factories.persistence.forCard();
    private final VoucherGateway vouchers = Factories.persistence.forVoucher();
    private final PaymentMeanGateway paymentMeans =
            Factories.persistence.forPaymentMean();

    public DeleteClientByNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        this.nif = nif;
    }

    @Override
    public Void execute() throws BusinessException {
        var client = clients.findByNif(nif);
        BusinessChecks.exists(client, "No existe el cliente");
        String clientId = client.get().id;
        if (clients.hasVehicles(clientId)) {
            throw new BusinessException(
                    "No se puede eliminar un cliente con vehículos");
        }
        if (clients.hasPaidInvoices(clientId)) {
            throw new BusinessException(
                    "No se puede eliminar un cliente con facturas pagadas");
        }
        recommendations.removeForClient(clientId);
        charges.removeForClient(clientId);
        cash.removeForClient(clientId);
        cards.removeForClient(clientId);
        vouchers.removeForClient(clientId);
        paymentMeans.removeForClient(clientId);
        clients.remove(clientId);
        return null;
    }
}
