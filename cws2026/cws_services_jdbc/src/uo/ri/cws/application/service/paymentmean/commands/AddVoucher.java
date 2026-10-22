package uo.ri.cws.application.service.paymentmean.commands;

import java.math.BigDecimal;
import java.util.UUID;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class AddVoucher implements Command<VoucherDto> {

    private final VoucherDto voucher;

    public AddVoucher(VoucherDto voucher) {
        ArgumentChecks.isNotNull(voucher, "El vale no puede ser nulo");
        ArgumentChecks.isNotBlank(voucher.code, "El código del vale no puede estar vacío");
        ArgumentChecks.isNotBlank(voucher.description, "La descripción no puede estar vacía");
        ArgumentChecks.isNotBlank(voucher.clientId, "El identificador del cliente no puede estar vacío");
        ArgumentChecks.isNotNull(voucher.availableAmount, "El importe disponible no puede ser nulo");
        ArgumentChecks.isTrue(voucher.availableAmount.signum() >= 0, "El importe disponible no puede ser negativo");
        this.voucher = voucher;
    }

    @Override
    public VoucherDto execute() throws BusinessException {
        PaymentMeanGateway paymentMeans = Factories.persistence.forPaymentMean();
        if (!paymentMeans.existsClient(voucher.clientId)) {
            throw new BusinessException("No existe el cliente");
        }
        if (paymentMeans.existsVoucherCode(voucher.code)) {
            throw new BusinessException("Ya existe un vale con ese código");
        }
        BigDecimal accumulated = voucher.accumulatedAmount == null ? BigDecimal.ZERO : voucher.accumulatedAmount;
        VoucherGateway.VoucherRecord record = new VoucherGateway.VoucherRecord();
        record.id = UUID.randomUUID().toString();
        record.clientId = voucher.clientId;
        record.code = voucher.code;
        record.description = voucher.description;
        record.available = voucher.availableAmount;
        record.accumulated = accumulated;
        PaymentMeanGateway.PaymentMeanRecord parent = new PaymentMeanGateway.PaymentMeanRecord();
        parent.id = record.id;
        parent.dtype = "Voucher";
        parent.clientId = voucher.clientId;
        parent.accumulated = accumulated;
        paymentMeans.add(parent);
        Factories.persistence.forVoucher().add(record);
        voucher.id = record.id;
        voucher.version = record.version;
        voucher.accumulatedAmount = accumulated;
        return voucher;
    }
}
