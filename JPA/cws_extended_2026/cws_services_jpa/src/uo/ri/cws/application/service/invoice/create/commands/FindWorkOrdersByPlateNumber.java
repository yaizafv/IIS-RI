package uo.ri.cws.application.service.invoice.create.commands;

import java.util.ArrayList;
import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.VehicleRepository;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.cws.application.service.invoice.create.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.types.LicensePlate;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindWorkOrdersByPlateNumber implements Command<List<InvoicingWorkOrderDto>> {

	private LicensePlate plate;
	private VehicleRepository repo = Factories.repository.forVehicle();

	public FindWorkOrdersByPlateNumber(String plate) {
		ArgumentChecks.isNotBlank(plate, "The plate cannot be null or blank");
		this.plate = new LicensePlate(plate);
	}

	@Override
	public List<InvoicingWorkOrderDto> execute() throws BusinessException {
		return repo.findByPlate(plate)
				.map(v -> new ArrayList<>(v.getWorkOrders()))
				.map(DtoAssembler::toInvoicingWorkOrderDtoList)
				.orElse(List.of());
	}

}
