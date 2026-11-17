package uo.ri.cws.application.service.client.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.ClientRepository;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.repository.RecommendationRepository;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.client.crud.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Cash;
import uo.ri.cws.domain.Client;
import uo.ri.cws.domain.Recommendation;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class AddClient implements Command<ClientDto> {

	private ClientDto dto;
	private String recommenderNif;
	private ClientRepository clientRepo = Factories.repository.forClient();
	private PaymentMeanRepository pmRepo = Factories.repository.forPaymentMean();
	private RecommendationRepository recommendationRepo = Factories.repository.forRecommendation();

	public AddClient(ClientDto dto, String recommenderNif) {
		ClientDtoChecks.checkFields(dto);
		if (recommenderNif != null) {
			ArgumentChecks.isNotBlank(recommenderNif, "The recommender nif cannot be empty or blank");
		}
		this.dto = dto;
		this.recommenderNif = recommenderNif;
	}

	@Override
	public ClientDto execute() throws BusinessException {
		ClientDtoChecks.checkFormats(dto);
		Nif nif = new Nif(dto.nif);
		BusinessChecks.doesNotExist(clientRepo.findByNif(nif), "There is already a client with the same nif");

		Client client = DtoAssembler.toClient(dto);
		clientRepo.add(client);
		pmRepo.add(new Cash(client));
		if (recommenderNif != null) {
			addRecommendation(client);
		}

		dto.id = client.getId();
		dto.version = client.getVersion();
		return dto;
	}

	private void addRecommendation(Client recommended) throws BusinessException {
		BusinessChecks.isTrue(Nif.isValid(recommenderNif), "The recommender client does not exist");
		Nif nif = new Nif(recommenderNif);
		BusinessChecks.isFalse(nif.equals(recommended.getNif()), "A client cannot recommend itself");
		Optional<Client> recommender = clientRepo.findByNif(nif);
		BusinessChecks.exists(recommender, "The recommender client does not exist");

		recommendationRepo.add(new Recommendation(recommender.get(), recommended));
	}

}
