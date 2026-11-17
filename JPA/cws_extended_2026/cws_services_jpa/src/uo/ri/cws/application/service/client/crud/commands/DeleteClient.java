package uo.ri.cws.application.service.client.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.query.ClientQueries;
import uo.ri.cws.application.repository.ClientRepository;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.repository.RecommendationRepository;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Associations;
import uo.ri.cws.domain.Client;
import uo.ri.cws.domain.PaymentMean;
import uo.ri.cws.domain.Recommendation;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class DeleteClient implements Command<Void> {

	private String nif;
	private ClientRepository clientRepo = Factories.repository.forClient();
	private PaymentMeanRepository pmRepo = Factories.repository.forPaymentMean();
	private RecommendationRepository recommendationRepo = Factories.repository.forRecommendation();
	private ClientQueries queries = Factories.query.forClient();

	public DeleteClient(String nif) {
		ArgumentChecks.isNotBlank(nif, "The client nif cannot be null or blank");
		this.nif = nif;
	}

	@Override
	public Void execute() throws BusinessException {
		BusinessChecks.isTrue(Nif.isValid(nif), "The client does not exist");
		Optional<Client> oc = clientRepo.findByNif(new Nif(nif));
		BusinessChecks.exists(oc, "The client does not exist");
		Client client = oc.get();
		BusinessChecks.isTrue(client.getVehicles().isEmpty(), "The client has vehicles registered");
		BusinessChecks.isFalse(queries.hasPaidInvoices(client.getId()), "The client has paid invoices");

		client.getPaymentMeans().forEach(this::removePaymentMean);
		client.getRecommendations().forEach(this::removeRecommendation);
		Optional.ofNullable(client.getRecommendation()).ifPresent(this::removeRecommendation);
		clientRepo.remove(client);
		return null;
	}

	private void removePaymentMean(PaymentMean paymentMean) {
		Associations.Holds.unlink(paymentMean);
		pmRepo.remove(paymentMean);
	}

	private void removeRecommendation(Recommendation recommendation) {
		Associations.Recommends.unlink(recommendation);
		recommendationRepo.remove(recommendation);
	}

}
