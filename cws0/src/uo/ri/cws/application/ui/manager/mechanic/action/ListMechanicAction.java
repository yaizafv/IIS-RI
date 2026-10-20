package uo.ri.cws.application.ui.manager.mechanic.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.jdbc.Jdbc;
import uo.ri.util.menu.Action;

public class ListMechanicAction implements Action {

    private static final String TMECHANICS_FINDBYNIF = 
            "SELECT ID, NAME, SURNAME, nif, VERSION FROM TMECHANICS "
                    + "WHERE NIF = ?";

    @Override
    public void execute() throws BusinessException {

        // Get info
        String nif = Console.readString("nif");

        Console.println("\nMechanic information \n");

        try (Connection c = Jdbc.createThreadConnection()) {
            try (PreparedStatement pst = c
                    .prepareStatement(TMECHANICS_FINDBYNIF)) {
                pst.setString(1, nif);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        Console.printf("\t%s %s %s %s %d\n",
                                rs.getString(1),
                                rs.getString(2),
                                rs.getString(3),
                                rs.getString(4),
                                rs.getLong(5));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}