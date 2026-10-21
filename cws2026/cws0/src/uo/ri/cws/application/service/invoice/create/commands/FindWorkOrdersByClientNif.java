package uo.ri.cws.application.service.invoice.create.commands;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.PersistenceException;

public class FindWorkOrdersByClientNif implements Command<List<InvoicingWorkOrderDto>> {

    private final String nif;

    public FindWorkOrdersByClientNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        this.nif = nif;
    }

    @Override
    public List<InvoicingWorkOrderDto> execute() throws BusinessException {
        List<InvoicingWorkOrderDto> workOrders = new ArrayList<>();
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
                .prepareStatement(Queries.getSQLSentence("TWORKORDERS_FIND_BY_CLIENT_NIF"))) {
            pst.setString(1, nif);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    InvoicingWorkOrderDto dto = new InvoicingWorkOrderDto();
                    dto.id = rs.getString("id");
                    if (rs.getTimestamp("date") != null) {
                        dto.date = rs.getTimestamp("date").toLocalDateTime();
                    }
                    dto.description = rs.getString("description");
                    dto.state = rs.getString("state");
                    dto.amount = rs.getBigDecimal("total_amount");
                    workOrders.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return workOrders;
    }
}
