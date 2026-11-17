package uo.ri.cws.application.service.invoice.create.commands;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.InvoiceRepository;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Invoice;
import uo.ri.cws.domain.PaymentMean;
import uo.ri.cws.domain.types.Money;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class SettleInvoice implements Command<Void> {

	private String invoiceId;
	private Map<String, BigDecimal> charges;
	private InvoiceRepository invoiceRepo = Factories.repository.forInvoice();
	private PaymentMeanRepository pmRepo = Factories.repository.forPaymentMean();

	public SettleInvoice(String invoiceId, Map<String, BigDecimal> charges) {
		ArgumentChecks.isNotBlank(invoiceId, "The invoice id cannot be null or blank");
		ArgumentChecks.isNotNull(charges, "The charges cannot be null");
		ArgumentChecks.isFalse(charges.isEmpty(), "The charges cannot be empty");
		charges.forEach((id, amount) -> {
			ArgumentChecks.isNotNull(id, "The payment mean id cannot be null");
			ArgumentChecks.isNotNull(amount, "The charge amount cannot be null");
		});
		this.invoiceId = invoiceId;
		this.charges = charges;
	}

	@Override
	public Void execute() throws BusinessException {
		Optional<Invoice> oi = invoiceRepo.findById(invoiceId);
		BusinessChecks.exists(oi, "The invoice does not exist");
		Invoice invoice = oi.get();
		BusinessChecks.isFalse(invoice.isSettled(), "The invoice is already paid");

		Map<PaymentMean, Money> requested = loadPaymentMeans();
		BusinessChecks.isTrue(invoice.canBeSettledWith(requested),
				"The charges do not sum the invoice total or some payment mean cannot be charged");

		invoice.settle(requested);
		return null;
	}

	private Map<PaymentMean, Money> loadPaymentMeans() throws BusinessException {
		List<PaymentMean> paymentMeans = pmRepo.findByIds(charges.keySet());
		BusinessChecks.isTrue(paymentMeans.size() == charges.size(), "Some payment mean does not exist");

		Map<PaymentMean, Money> result = new HashMap<>();
		paymentMeans.forEach(pm -> result.put(pm, Money.of(charges.get(pm.getId()))));
		return result;
	}

}
