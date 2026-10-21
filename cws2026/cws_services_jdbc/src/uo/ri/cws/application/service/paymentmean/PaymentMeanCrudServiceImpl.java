package uo.ri.cws.application.service.paymentmean;

import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.util.command.CommandExecutor;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CardDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.PaymentMeanDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;
import uo.ri.cws.application.service.paymentmean.commands.AddCard;
import uo.ri.cws.application.service.paymentmean.commands.AddVoucher;
import uo.ri.cws.application.service.paymentmean.commands.DeletePaymentMean;
import uo.ri.cws.application.service.paymentmean.commands.FindCardByNumber;
import uo.ri.cws.application.service.paymentmean.commands.FindPaymentMeanById;
import uo.ri.cws.application.service.paymentmean.commands.FindPaymentMeansByClientNif;
import uo.ri.cws.application.service.paymentmean.commands.FindVoucherByCode;
import uo.ri.util.exception.BusinessException;

public class PaymentMeanCrudServiceImpl implements PaymentMeanCrudService {

    private final CommandExecutor executor = new CommandExecutor();

    @Override
    public CardDto addCard(CardDto card) throws BusinessException {
        return executor.execute(new AddCard(card));
    }

    @Override
    public VoucherDto addVoucher(VoucherDto voucher) throws BusinessException {
        return executor.execute(new AddVoucher(voucher));
    }

    @Override
    public void deletePaymentMean(String id) throws BusinessException {
        executor.execute(new DeletePaymentMean(id));
    }

    @Override
    public Optional<PaymentMeanDto> findById(String id) throws BusinessException {
        return executor.execute(new FindPaymentMeanById(id));
    }

    @Override
    public List<PaymentMeanDto> findPaymentMeansByClientNif(String nif)
            throws BusinessException {
        return executor.execute(new FindPaymentMeansByClientNif(nif));
    }

    @Override
    public Optional<VoucherDto> findVoucherByCode(String code)
            throws BusinessException {
        return executor.execute(new FindVoucherByCode(code));
    }

    @Override
    public Optional<CardDto> findCreditCardByNumber(String cardNumber)
            throws BusinessException {
        return executor.execute(new FindCardByNumber(cardNumber));
    }
}
