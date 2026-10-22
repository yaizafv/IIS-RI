package uo.ri.cws.application.service.paymentmean;

import java.util.List;

import uo.ri.cws.application.persistence.util.command.CommandExecutor;
import uo.ri.cws.application.service.paymentmean.commands.FindVouchersByClientNif;
import uo.ri.cws.application.service.paymentmean.commands.GenerateVouchers;
import uo.ri.cws.application.service.paymentmean.commands.GetVoucherSummary;
import uo.ri.util.exception.BusinessException;

public class VoucherGenerationServiceImpl implements VoucherGenerationService {

    private final CommandExecutor executor = new CommandExecutor();

    @Override
    public int[] generateVouchers() throws BusinessException {
        return executor.execute(new GenerateVouchers());
    }

    @Override
    public List<VoucherDto> findVouchersByClientNif(String nif) throws BusinessException {
        return executor.execute(new FindVouchersByClientNif(nif));
    }

    @Override
    public List<VoucherSummaryDto> getVoucherSummary() throws BusinessException {
        return executor.execute(new GetVoucherSummary());
    }
}
