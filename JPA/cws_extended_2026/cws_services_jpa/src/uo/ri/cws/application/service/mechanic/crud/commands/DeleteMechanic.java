package uo.ri.cws.application.service.mechanic.crud.commands;

import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.query.MechanicQueries;
import uo.ri.cws.application.repository.MechanicRepository;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Mechanic;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class DeleteMechanic implements Command<Void> {

	private String mechanicId;
	private MechanicRepository repo = Factories.repository.forMechanic();
	private MechanicQueries queries = Factories.query.forMechanic();

	public DeleteMechanic(String mechanicId) {
		ArgumentChecks.isNotBlank(mechanicId, "The mechanic id is null or empty");
		this.mechanicId = mechanicId;
	}

	@Override
	public Void execute() throws BusinessException {
		Optional<Mechanic> om = repo.findById(mechanicId);
		BusinessChecks.exists(om, "The mechanic does not exist");
		Mechanic m = om.get();

		BusinessChecks.isTrue(m.getAssigned().isEmpty(), "The mechanic has work orders assigned");
		BusinessChecks.isFalse(queries.hasInterventions(mechanicId), "The mechanic has interventions");

		repo.remove(m);
		return null;
	}

}
