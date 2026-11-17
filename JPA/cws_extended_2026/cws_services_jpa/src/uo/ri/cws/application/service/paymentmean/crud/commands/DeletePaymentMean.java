package uo.ri.cws.application.service.paymentmean.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.query.PaymentMeanQueries;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Associations;
import uo.ri.cws.domain.PaymentMean;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class DeletePaymentMean implements Command<Void> {

	private String id;
	private PaymentMeanRepository repo = Factories.repository.forPaymentMean();
	private PaymentMeanQueries queries = Factories.query.forPaymentMean();

	public DeletePaymentMean(String id) {
		ArgumentChecks.isNotBlank(id, "The payment mean id cannot be null or blank");
		this.id = id;
	}

	@Override
	public Void execute() throws BusinessException {
		Optional<PaymentMean> opm = repo.findById(id);
		BusinessChecks.exists(opm, "The payment mean does not exist");
		PaymentMean paymentMean = opm.get();
		BusinessChecks.isTrue(paymentMean.isRemovable(), "Cash payment means cannot be removed");
		BusinessChecks.isFalse(queries.hasCharges(id), "The payment mean has charges");

		Associations.Holds.unlink(paymentMean);
		repo.remove(paymentMean);
		return null;
	}

}
