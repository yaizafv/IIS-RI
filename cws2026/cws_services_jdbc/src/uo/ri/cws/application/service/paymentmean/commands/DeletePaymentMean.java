package uo.ri.cws.application.service.paymentmean.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class DeletePaymentMean implements Command<Void> {

    private final String id;

    public DeletePaymentMean(String id) {
        ArgumentChecks.isNotBlank(id, "El identificador del medio de pago no puede estar vacío");
        this.id = id;
    }

    @Override
    public Void execute() throws BusinessException {
        PaymentMeanGateway paymentMeans = Factories.persistence.forPaymentMean();
        Optional<PaymentMeanGateway.PaymentMeanRecord> result = paymentMeans.findById(id);
        if (result.isEmpty()) {
            throw new BusinessException("No existe el medio de pago");
        }
        PaymentMeanGateway.PaymentMeanRecord record = result.get();
        if ("Cash".equals(record.dtype)) {
            throw new BusinessException("No se puede eliminar el medio de pago en efectivo");
        }
        if (paymentMeans.hasCharges(id)) {
            throw new BusinessException("El medio de pago tiene cargos asociados");
        }
        if ("CreditCard".equals(record.dtype)) {
            Factories.persistence.forCard().remove(id);
        } else if ("Voucher".equals(record.dtype)) {
            Factories.persistence.forVoucher().remove(id);
        }
        paymentMeans.remove(id);
        return null;
    }
}
