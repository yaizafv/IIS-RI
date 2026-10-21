package uo.ri.cws.application.service.paymentmean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uo.ri.cws.application.persistence.PersistenceFactory;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CardRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.VoucherRecord;
import uo.ri.cws.application.persistence.util.command.CommandExecutor;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CardDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CashDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.PaymentMeanDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class PaymentMeanCrudServiceImpl implements PaymentMeanCrudService {

    private final CommandExecutor executor = new CommandExecutor();
    private final PersistenceFactory persistence = new uo.ri.cws.application.persistence.PersistenceFactoryImpl();

    @Override
    public CardDto addCard(CardDto card) throws BusinessException {
        ArgumentChecks.isNotNull(card, "La tarjeta no puede ser nula");
        ArgumentChecks.isNotBlank(card.cardNumber, "El número de tarjeta no puede estar vacío");
        ArgumentChecks.isNotNull(card.cardExpiration, "La fecha de caducidad no puede ser nula");
        ArgumentChecks.isNotBlank(card.cardType, "El tipo de tarjeta no puede estar vacío");
        ArgumentChecks.isNotBlank(card.clientId, "El identificador del cliente no puede estar vacío");
        return executor.execute(() -> {
            PaymentMeanGateway gateway = persistence.forPaymentMean();
            if (!gateway.existsClient(card.clientId)) {
                throw new BusinessException("No existe el cliente");
            }
            if (gateway.existsCardNumber(card.cardNumber)) {
                throw new BusinessException("Ya existe una tarjeta con ese número");
            }
            if (card.cardExpiration.isBefore(YearMonth.from(LocalDate.now()))) {
                throw new BusinessException("La tarjeta está caducada");
            }
            CardRecord record = new CardRecord();
            record.id = UUID.randomUUID().toString();
            record.dtype = "CreditCard";
            record.clientId = card.clientId;
            record.accumulated = BigDecimal.ZERO;
            record.number = card.cardNumber;
            record.validThru = card.cardExpiration;
            record.type = card.cardType;
            gateway.addCard(record);
            card.id = record.id;
            card.version = record.version;
            card.accumulatedAmount = BigDecimal.ZERO;
            return card;
        });
    }

    @Override
    public VoucherDto addVoucher(VoucherDto voucher) throws BusinessException {
        ArgumentChecks.isNotNull(voucher, "El vale no puede ser nulo");
        ArgumentChecks.isNotBlank(voucher.code, "El código del vale no puede estar vacío");
        ArgumentChecks.isNotBlank(voucher.description, "La descripción no puede estar vacía");
        ArgumentChecks.isNotBlank(voucher.clientId, "El identificador del cliente no puede estar vacío");
        ArgumentChecks.isNotNull(voucher.availableAmount, "El importe disponible no puede ser nulo");
        ArgumentChecks.isTrue(voucher.availableAmount.signum() >= 0,
                "El importe disponible no puede ser negativo");
        return executor.execute(() -> {
            PaymentMeanGateway gateway = persistence.forPaymentMean();
            if (!gateway.existsClient(voucher.clientId)) {
                throw new BusinessException("No existe el cliente");
            }
            if (gateway.existsVoucherCode(voucher.code)) {
                throw new BusinessException("Ya existe un vale con ese código");
            }
            VoucherRecord record = new VoucherRecord();
            record.id = UUID.randomUUID().toString();
            record.dtype = "Voucher";
            record.clientId = voucher.clientId;
            record.code = voucher.code;
            record.description = voucher.description;
            record.available = voucher.availableAmount;
            record.accumulated = voucher.accumulatedAmount == null
                    ? BigDecimal.ZERO : voucher.accumulatedAmount;
            gateway.addVoucher(record);
            voucher.id = record.id;
            voucher.version = record.version;
            voucher.accumulatedAmount = record.accumulated;
            return voucher;
        });
    }

    @Override
    public void deletePaymentMean(String id) throws BusinessException {
        ArgumentChecks.isNotBlank(id, "El identificador del medio de pago no puede estar vacío");
        executor.execute(() -> {
            PaymentMeanGateway gateway = persistence.forPaymentMean();
            Optional<PaymentMeanRecord> found = gateway.findById(id);
            if (found.isEmpty()) {
                throw new BusinessException("No existe el medio de pago");
            }
            PaymentMeanRecord record = found.get();
            if ("Cash".equals(record.dtype)) {
                throw new BusinessException("No se puede eliminar el medio de pago en efectivo");
            }
            if (gateway.hasCharges(id)) {
                throw new BusinessException("El medio de pago tiene cargos asociados");
            }
            gateway.removeSubtype(record);
            gateway.remove(id);
            return null;
        });
    }

    @Override
    public Optional<PaymentMeanDto> findById(String id) throws BusinessException {
        ArgumentChecks.isNotBlank(id, "El identificador del medio de pago no puede estar vacío");
        return executor.execute(() -> toDto(persistence.forPaymentMean().findById(id)));
    }

    @Override
    public List<PaymentMeanDto> findPaymentMeansByClientNif(String nif)
            throws BusinessException {
        ArgumentChecks.isNotBlank(nif, "El NIF del cliente no puede estar vacío");
        return executor.execute(() -> {
            List<PaymentMeanDto> result = new ArrayList<>();
            for (PaymentMeanRecord record : persistence.forPaymentMean()
                    .findByClientNif(nif)) {
                result.add(toDto(record));
            }
            return result;
        });
    }

    @Override
    public Optional<VoucherDto> findVoucherByCode(String code) throws BusinessException {
        ArgumentChecks.isNotBlank(code, "El código del vale no puede estar vacío");
        return executor.execute(() -> toVoucher(
                persistence.forPaymentMean().findByVoucherCode(code)));
    }

    @Override
    public Optional<CardDto> findCreditCardByNumber(String cardNumber)
            throws BusinessException {
        ArgumentChecks.isNotBlank(cardNumber, "El número de tarjeta no puede estar vacío");
        return executor.execute(() -> toCard(
                persistence.forPaymentMean().findByCardNumber(cardNumber)));
    }

    private static Optional<PaymentMeanDto> toDto(Optional<PaymentMeanRecord> record) {
        return record.map(PaymentMeanCrudServiceImpl::toDto);
    }

    private static PaymentMeanDto toDto(PaymentMeanRecord record) {
        PaymentMeanDto dto;
        if (record instanceof CardRecord card) {
            CardDto result = new CardDto();
            result.cardNumber = card.number;
            result.cardExpiration = card.validThru;
            result.cardType = card.type;
            dto = result;
        } else if (record instanceof VoucherRecord voucher) {
            VoucherDto result = new VoucherDto();
            result.code = voucher.code;
            result.description = voucher.description;
            result.availableAmount = voucher.available;
            dto = result;
        } else {
            dto = new CashDto();
        }
        dto.id = record.id;
        dto.version = record.version;
        dto.clientId = record.clientId;
        dto.accumulatedAmount = record.accumulated;
        return dto;
    }

    private static Optional<VoucherDto> toVoucher(Optional<PaymentMeanRecord> record) {
        return record.filter(VoucherRecord.class::isInstance)
                .map(PaymentMeanCrudServiceImpl::toDto)
                .map(VoucherDto.class::cast);
    }

    private static Optional<CardDto> toCard(Optional<PaymentMeanRecord> record) {
        return record.filter(CardRecord.class::isInstance)
                .map(PaymentMeanCrudServiceImpl::toDto)
                .map(CardDto.class::cast);
    }
}
