package uo.ri.cws.application.service.paymentmean.voucher.commands;

import java.util.List;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherDto;
import uo.ri.cws.application.service.paymentmean.voucher.DtoAssembler;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class FindVouchersByClientNif implements Command<List<VoucherDto>> {

	private String nif;
	private PaymentMeanRepository repo = Factories.repository.forPaymentMean();

	public FindVouchersByClientNif(String nif) {
		ArgumentChecks.isNotBlank(nif, "The client nif cannot be null or blank");
		this.nif = nif;
	}

	/**
	 * A nif with a wrong format cannot belong to any client
	 */
	@Override
	public List<VoucherDto> execute() throws BusinessException {
		if (!Nif.isValid(nif)) {
			return List.of();
		}
		return DtoAssembler.toDtoList(repo.findVouchersByClientNif(new Nif(nif)));
	}

}
