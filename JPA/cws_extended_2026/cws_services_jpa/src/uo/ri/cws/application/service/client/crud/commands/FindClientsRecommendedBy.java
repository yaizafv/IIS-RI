package uo.ri.cws.application.service.client.crud.commands;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.ClientRepository;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.client.crud.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindClientsRecommendedBy implements Command<List<ClientDto>> {

	private String recommenderNif;
	private ClientRepository repo = Factories.repository.forClient();

	public FindClientsRecommendedBy(String recommenderNif) {
		ArgumentChecks.isNotBlank(recommenderNif, "The recommender nif cannot be null or blank");
		this.recommenderNif = recommenderNif;
	}

	/**
	 * A nif with a wrong format cannot belong to any client
	 */
	@Override
	public List<ClientDto> execute() throws BusinessException {
		if (!Nif.isValid(recommenderNif)) {
			return List.of();
		}
		return DtoAssembler.toDtoList(repo.findRecommendedBy(new Nif(recommenderNif)));
	}

}
