package uo.ri.cws.application.service.invoice.create.commands;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.charge.ChargeGateway;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CardRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.VoucherRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.PersistenceException;

public class SettleInvoice implements Command<Void> {

    private final InvoiceGateway invoiceGateway = Factories.persistence.forInvoice();
    private final PaymentMeanGateway paymentMeanGateway = Factories.persistence.forPaymentMean();
    private final ChargeGateway chargeGateway = Factories.persistence.forCharge();
    private final String invoiceId;
    private final Map<String, BigDecimal> charges;

    public SettleInvoice(String invoiceId, Map<String, BigDecimal> charges) {
        ArgumentChecks.isNotBlank(invoiceId, "El identificador de factura no puede estar vacío");
        ArgumentChecks.isNotNull(charges, "Los cargos no pueden ser nulos");
        ArgumentChecks.isTrue(!charges.isEmpty(), "Debe indicarse al menos un cargo");
        for (Map.Entry<String, BigDecimal> charge : charges.entrySet()) {
            ArgumentChecks.isNotBlank(charge.getKey(),
                    "El identificador del medio de pago no puede estar vacío");
            ArgumentChecks.isNotNull(charge.getValue(), "El importe del cargo no puede ser nulo");
            ArgumentChecks.isTrue(charge.getValue().signum() >= 0,
                    "El importe del cargo no puede ser negativo");
        }
        this.invoiceId = invoiceId;
        this.charges = new LinkedHashMap<>();
        charges.forEach((id, amount) ->
                this.charges.put(id, amount.setScale(2, RoundingMode.HALF_EVEN)));
    }

    @Override
    public Void execute() throws BusinessException {
        InvoiceRecord invoice = invoiceGateway.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("No existe la factura"));
        if ("PAID".equals(invoice.state)) {
            throw new BusinessException("La factura ya está pagada");
        }

        BigDecimal total = charges.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(invoice.total.setScale(2, RoundingMode.HALF_EVEN)) != 0) {
            throw new BusinessException("El importe de los cargos no coincide con la factura");
        }

        for (Map.Entry<String, BigDecimal> charge : charges.entrySet()) {
            settlePaymentMean(charge.getKey(), charge.getValue());
            chargeGateway.add(invoiceId, charge.getKey(), charge.getValue());
        }
        invoiceGateway.setPaid(invoiceId);
        return null;
    }

    private void settlePaymentMean(String id, BigDecimal amount)
            throws BusinessException {
        PaymentMeanGateway.PaymentMeanRecord record = paymentMeanGateway.findById(id)
                .orElseThrow(() -> new BusinessException("No existe el medio de pago"));
        if (record instanceof CardRecord card) {
            if (card.validThru != null
                    && card.validThru.isBefore(YearMonth.from(LocalDate.now()))) {
                throw new BusinessException("La tarjeta está caducada");
            }
        } else if (record instanceof VoucherRecord voucher) {
            if (voucher.available.compareTo(amount) < 0) {
                throw new BusinessException("El vale no tiene saldo suficiente");
            }
            paymentMeanGateway.decreaseVoucherAvailable(id, amount);
        }
        paymentMeanGateway.addAccumulated(id, amount);
    }
}
