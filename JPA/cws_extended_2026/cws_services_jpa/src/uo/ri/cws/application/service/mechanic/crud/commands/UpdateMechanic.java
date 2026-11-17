package uo.ri.cws.application.service.mechanic.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.MechanicRepository;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Mechanic;
import uo.ri.cws.domain.types.FullName;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class UpdateMechanic implements Command<Void> {

	private MechanicDto dto;
	private MechanicRepository repo = Factories.repository.forMechanic();

	public UpdateMechanic(MechanicDto dto) {
		ArgumentChecks.isNotNull(dto, "The mechanic dto is null");
		ArgumentChecks.isNotBlank(dto.id, "The mechanic id is null or empty");
		ArgumentChecks.isNotBlank(dto.nif, "The mechanic nif is null or empty");
		ArgumentChecks.isNotBlank(dto.name, "The mechanic name is null or empty");
		ArgumentChecks.isNotBlank(dto.surname, "The mechanic surname is null or empty");
		this.dto = dto;
	}

	@Override
	public Void execute() throws BusinessException {
		Optional<Mechanic> om = repo.findById(dto.id);
		BusinessChecks.exists(om, "The mechanic does not exist");
		Mechanic m = om.get();
		BusinessChecks.hasVersion(m.getVersion(), dto.version, "The mechanic has been updated by another user");

		BusinessChecks.isTrue(Nif.isValid(dto.nif), "The nif is not valid");
		Nif nif = new Nif(dto.nif);
		if (!m.getNif().equals(nif)) {
			BusinessChecks.doesNotExist(repo.findByNif(nif), "There is already a mechanic with the same nif");
			m.setNif(nif);
		}
		m.setFullName(new FullName(dto.name, dto.surname));
		m.updatedNow();
		return null;
	}

}
