package uo.ri.cws.application.ui.manager.mechanic.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.jdbc.Jdbc;
import uo.ri.util.menu.Action;

public class DeleteMechanicAction implements Action {

    private static final String TMECHANICS_DELETE = "DELETE FROM TMECHANICS "
            + "WHERE ID = ?";

    @Override
    public void execute() throws BusinessException {

        String idMechanic = Console.readString("Type mechanic id ");

        // Process
        try (Connection c = Jdbc.createThreadConnection();) {
            try (PreparedStatement pst = c
                    .prepareStatement(TMECHANICS_DELETE)) {
                pst.setString(1, idMechanic);
                pst.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Console.println("Mechanic deleted");
    }

}