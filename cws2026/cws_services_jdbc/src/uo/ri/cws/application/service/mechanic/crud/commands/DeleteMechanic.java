package uo.ri.cws.application.service.mechanic.crud.commands;

import java.util.List;
import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.intervention.InterventionGateway;
import uo.ri.cws.application.persistence.intervention.InterventionGateway.InterventionRecord;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway;
import uo.ri.cws.application.persistence.mechanic.MechanicGateway.MechanicRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class DeleteMechanic implements Command<Void> {

    private final String mechanicId;
    private MechanicGateway mg = Factories.persistence.forMechanic();
    private InterventionGateway ig = Factories.persistence.forIntervention();
    private WorkOrderGateway wog = Factories.persistence.forWorkOrder();

    public DeleteMechanic(String mechanicId) {
        ArgumentChecks.isNotNull(mechanicId, "Id cannot be null");
        ArgumentChecks.isNotBlank(mechanicId, "Invalid id");
        this.mechanicId = mechanicId;
    }

    @Override
    public Void execute() throws BusinessException {
        checkMechanicExists();
        checkCanBeDeleted();
        deleteMechanic();
        return null;
    }

    private void deleteMechanic() {
        mg.remove(mechanicId);
    }

    private void checkMechanicExists() throws BusinessException {
        Optional<MechanicRecord> omr = mg.findById(mechanicId);
        BusinessChecks.exists(omr, "The mechanic doesnt exists");
    }

    private void checkCanBeDeleted() throws BusinessException {
        List<InterventionRecord> interventions = ig
            .findByMechanicId(mechanicId);
        Optional<InterventionRecord> optionalIntervention = Optional.empty();
        if (!interventions.isEmpty()) {
            optionalIntervention = Optional.of(interventions.get(0));
        }
        BusinessChecks.doesNotExist(optionalIntervention,
            "It has an intervention associated");

        List<WorkOrderRecord> workOrders = wog.findByMechanicId(mechanicId);
        Optional<WorkOrderRecord> optionalWorkOrder = Optional.empty();
        if (!workOrders.isEmpty()) {
            optionalWorkOrder = Optional.of(workOrders.get(0));
        }
        BusinessChecks.doesNotExist(optionalWorkOrder,
            "It has a work order associated");
    }
}