package uo.ri.cws.application.service.client.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.ClientRepository;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.client.crud.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindClientByNif implements Command<Optional<ClientDto>> {

	private String nif;
	private ClientRepository repo = Factories.repository.forClient();

	public FindClientByNif(String nif) {
		ArgumentChecks.isNotBlank(nif, "The client nif cannot be null or blank");
		this.nif = nif;
	}

	/**
	 * A nif with a wrong format cannot belong to any client
	 */
	@Override
	public Optional<ClientDto> execute() throws BusinessException {
		if (!Nif.isValid(nif)) {
			return Optional.empty();
		}
		return repo.findByNif(new Nif(nif)).map(DtoAssembler::toDto);
	}

}
