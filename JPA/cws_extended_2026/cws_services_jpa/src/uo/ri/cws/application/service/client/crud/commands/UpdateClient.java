package uo.ri.cws.application.service.client.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.ClientRepository;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.client.crud.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Client;
import uo.ri.cws.domain.types.Email;
import uo.ri.cws.domain.types.Nif;
import uo.ri.cws.domain.types.PhoneNumber;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class UpdateClient implements Command<Void> {

	private ClientDto dto;
	private ClientRepository repo = Factories.repository.forClient();

	public UpdateClient(ClientDto dto) {
		ArgumentChecks.isNotNull(dto, "The client dto cannot be null");
		ArgumentChecks.isNotBlank(dto.id, "The client id cannot be null or blank");
		this.dto = dto;
	}

	@Override
	public Void execute() throws BusinessException {
		ClientDtoChecks.checkFormats(dto);
		Optional<Client> oc = repo.findById(dto.id);
		BusinessChecks.exists(oc, "The client does not exist");
		Client client = oc.get();
		BusinessChecks.hasVersion(client.getVersion(), dto.version, "The client has been updated by another user");

		Nif nif = new Nif(dto.nif);
		if (!client.getNif().equals(nif)) {
			BusinessChecks.doesNotExist(repo.findByNif(nif), "There is already a client with the same nif");
			client.changeNif(nif);
		}
		client.update(DtoAssembler.toFullName(dto), new Email(dto.email), new PhoneNumber(dto.phone),
				DtoAssembler.toAddress(dto));
		client.updatedNow();
		return null;
	}

}
