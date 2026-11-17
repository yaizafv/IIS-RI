package uo.ri.cws.application.service.paymentmean.crud.commands;

import java.math.BigDecimal;
import java.util.Optional;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.ClientRepository;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Associations;
import uo.ri.cws.domain.Client;
import uo.ri.cws.domain.Voucher;
import uo.ri.cws.domain.types.Money;
import uo.ri.cws.domain.types.VoucherCode;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

public class AddVoucher implements Command<VoucherDto> {

	private VoucherDto dto;
	private ClientRepository clientRepo = Factories.repository.forClient();
	private PaymentMeanRepository pmRepo = Factories.repository.forPaymentMean();

	public AddVoucher(VoucherDto dto) {
		ArgumentChecks.isNotNull(dto, "The voucher dto cannot be null");
		ArgumentChecks.isNotBlank(dto.clientId, "The client id cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.code, "The voucher code cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.description, "The voucher description cannot be null or blank");
		ArgumentChecks.isNotNull(dto.availableAmount, "The available amount cannot be null");
		ArgumentChecks.isTrue(dto.availableAmount.compareTo(BigDecimal.ZERO) >= 0,
				"The available amount cannot be negative");
		this.dto = dto;
	}

	@Override
	public VoucherDto execute() throws BusinessException {
		Optional<Client> oc = clientRepo.findById(dto.clientId);
		BusinessChecks.exists(oc, "The client does not exist");
		VoucherCode code = new VoucherCode(dto.code);
		BusinessChecks.doesNotExist(pmRepo.findVoucherByCode(code), "There is already a voucher with the same code");

		Voucher voucher = new Voucher(code, dto.description, Money.of(dto.availableAmount));
		Associations.Holds.link(oc.get(), voucher);
		pmRepo.add(voucher);

		dto.id = voucher.getId();
		dto.version = voucher.getVersion();
		return dto;
	}

}
