package uo.ri.cws.application.service.invoice.create;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import uo.ri.cws.application.persistence.util.command.CommandExecutor;
import uo.ri.cws.application.service.invoice.InvoicingService;
import uo.ri.cws.application.service.invoice.FindNotInvoicedWorkOrdersByClientNif;
import uo.ri.cws.application.service.invoice.create.commands.CreateInvoice;
import uo.ri.cws.application.service.invoice.create.commands.FindWorkOrdersByClientNif;
import uo.ri.util.exception.BusinessException;

public class InvoicingServiceImpl implements InvoicingService {

    private final CommandExecutor executor = new CommandExecutor();

    @Override
    public InvoiceDto create(List<String> workOrderIds) throws BusinessException {
        return executor.execute(new CreateInvoice(workOrderIds));
    }

    @Override
    public List<InvoicingWorkOrderDto> findWorkOrdersByClientNif(String nif) throws BusinessException {
        return executor.execute(new FindWorkOrdersByClientNif(nif));
    }

    @Override
    public List<InvoicingWorkOrderDto> findNotInvoicedWorkOrdersByClientNif(String nif) throws BusinessException {
        return executor.execute(new FindNotInvoicedWorkOrdersByClientNif(nif));
    }

    @Override
    public List<InvoicingWorkOrderDto> findWorkOrdersByPlateNumber(String plate) throws BusinessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<InvoiceDto> findInvoiceByNumber(Long number) throws BusinessException {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public List<PaymentMeanDto> findPayMeansByClientNif(String nif) throws BusinessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void settleInvoice(String invoiceId, Map<String, BigDecimal> charges) throws BusinessException {
        // TODO Auto-generated method stub
        
    }

}
