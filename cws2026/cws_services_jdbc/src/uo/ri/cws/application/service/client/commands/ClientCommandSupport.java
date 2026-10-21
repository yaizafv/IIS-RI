package uo.ri.cws.application.service.client.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.PersistenceException;

final class ClientCommandSupport {

    private ClientCommandSupport() {
    }

    static ClientDto add(ClientDto client, String recommenderNif)
            throws BusinessException {
        validateClient(client);
        if (recommenderNif != null) {
            ArgumentChecks.isNotBlank(recommenderNif,
                    "El NIF del recomendador no puede estar vacío");
        }

        Connection connection = Jdbc.getCurrentConnection();
        if (exists(connection, "TCLIENTS_FIND_BY_NIF", client.nif)) {
            throw new BusinessException("Ya existe un cliente con ese NIF");
        }

        String recommenderId = null;
        if (recommenderNif != null) {
            if (client.nif.equals(recommenderNif)) {
                throw new BusinessException(
                        "Un cliente no puede recomendarse a sí mismo");
            }
            recommenderId = findIdByNif(connection, recommenderNif);
            if (recommenderId == null) {
                throw new BusinessException(
                        "No existe el cliente recomendador");
            }
        }

        ClientDto added = copy(client);
        added.id = UUID.randomUUID().toString();
        added.version = 1;
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence("TCLIENTS_ADD"))) {
            bindClient(pst, added, now);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        String paymentMeanId = UUID.randomUUID().toString();
        execute(connection, "TPAYMENTMEANS_ADD_CASH", paymentMeanId,
                now, now, 1L, "ENABLED", added.id);
        execute(connection, "TCASHES_ADD", paymentMeanId);

        if (recommenderId != null) {
            execute(connection, "TRECOMMENDATIONS_ADD",
                    UUID.randomUUID().toString(), now, now, "ENABLED",
                    false, 1L, recommenderId, added.id);
        }
        return added;
    }

    static void deleteByNif(String nif) throws BusinessException {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        Connection connection = Jdbc.getCurrentConnection();
        String clientId = findIdByNif(connection, nif);
        if (clientId == null) {
            throw new BusinessException("No existe el cliente");
        }
        if (exists(connection, "TVEHICLES_EXISTS_BY_CLIENT", clientId)) {
            throw new BusinessException(
                    "No se puede eliminar un cliente con vehículos");
        }
        if (exists(connection, "TCLIENTS_HAS_PAID_INVOICES", clientId)) {
            throw new BusinessException(
                    "No se puede eliminar un cliente con facturas pagadas");
        }

        execute(connection, "TRECOMMENDATIONS_DELETE_FOR_CLIENT",
                clientId, clientId);
        execute(connection, "TCHARGES_DELETE_FOR_CLIENT", clientId);
        execute(connection, "TCASHES_DELETE_FOR_CLIENT", clientId);
        execute(connection, "TCREDITCARDS_DELETE_FOR_CLIENT", clientId);
        execute(connection, "TVOUCHERS_DELETE_FOR_CLIENT", clientId);
        execute(connection, "TPAYMENTMEANS_DELETE_FOR_CLIENT", clientId);
        execute(connection, "TCLIENTS_DELETE_BY_ID", clientId);
    }

    static void update(ClientDto client) throws BusinessException {
        validateClientForUpdate(client);
        Connection connection = Jdbc.getCurrentConnection();
        if (!exists(connection, "TCLIENTS_EXISTS_BY_ID", client.id)) {
            throw new BusinessException("No existe el cliente");
        }
        if (exists(connection, "TCLIENTS_FIND_OTHER_BY_NIF",
                client.nif, client.id)) {
            throw new BusinessException(
                    "Ya existe otro cliente con ese NIF");
        }

        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence("TCLIENTS_UPDATE"))) {
            pst.setString(1, client.name);
            pst.setString(2, client.surname);
            pst.setString(3, client.nif);
            pst.setString(4, client.phone);
            pst.setString(5, client.email);
            pst.setString(6, client.addressStreet);
            pst.setString(7, client.addressCity);
            pst.setString(8, client.addressCountry);
            pst.setString(9, client.addressZipcode);
            pst.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            pst.setString(11, client.id);
            pst.setLong(12, client.version);
            if (pst.executeUpdate() != 1) {
                throw new BusinessException(
                        "El cliente ha sido modificado por otra operación");
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    static List<ClientDto> findAll() {
        List<ClientDto> clients = new ArrayList<>();
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
                .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_ALL"));
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                clients.add(toDto(rs));
            }
            return clients;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    static Optional<ClientDto> findByNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
                .prepareStatement(
                        Queries.getSQLSentence("TCLIENTS_FIND_BY_NIF"))) {
            pst.setString(1, nif);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? Optional.of(toDto(rs))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    static List<ClientDto> findRecommendedBy(String recommenderNif) {
        ArgumentChecks.isNotBlank(recommenderNif,
                "El NIF del recomendador no puede estar vacío");
        List<ClientDto> clients = new ArrayList<>();
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
                .prepareStatement(Queries.getSQLSentence(
                        "TCLIENTS_FIND_RECOMMENDED_BY_NIF"))) {
            pst.setString(1, recommenderNif);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    clients.add(toDto(rs));
                }
            }
            return clients;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static void validateClient(ClientDto dto) {
        ArgumentChecks.isNotNull(dto,
                "Los datos del cliente no pueden ser nulos");
        ArgumentChecks.isNotBlank(dto.nif,
                "El NIF no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.name,
                "El nombre no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.surname,
                "Los apellidos no pueden estar vacíos");
        ArgumentChecks.isNotBlank(dto.addressStreet,
                "La calle no puede estar vacía");
        ArgumentChecks.isNotBlank(dto.addressCity,
                "La ciudad no puede estar vacía");
        ArgumentChecks.isNotBlank(dto.addressCountry,
                "El país no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.addressZipcode,
                "El código postal no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.phone,
                "El teléfono no puede estar vacío");
        ArgumentChecks.isNotBlank(dto.email,
                "El correo electrónico no puede estar vacío");
    }

    private static void validateClientForUpdate(ClientDto dto) {
        validateClient(dto);
        ArgumentChecks.isNotBlank(dto.id,
                "El identificador del cliente no puede estar vacío");
    }

    private static ClientDto copy(ClientDto source) {
        ClientDto dto = new ClientDto();
        dto.id = source.id;
        dto.version = source.version;
        dto.nif = source.nif;
        dto.name = source.name;
        dto.surname = source.surname;
        dto.addressStreet = source.addressStreet;
        dto.addressCity = source.addressCity;
        dto.addressCountry = source.addressCountry;
        dto.addressZipcode = source.addressZipcode;
        dto.phone = source.phone;
        dto.email = source.email;
        return dto;
    }

    private static void bindClient(PreparedStatement pst, ClientDto dto,
            Timestamp now) throws SQLException {
        pst.setString(1, dto.id);
        pst.setLong(2, dto.version);
        pst.setTimestamp(3, now);
        pst.setTimestamp(4, now);
        pst.setString(5, "ENABLED");
        pst.setString(6, dto.name);
        pst.setString(7, dto.surname);
        pst.setString(8, dto.nif);
        pst.setString(9, dto.phone);
        pst.setString(10, dto.email);
        pst.setString(11, dto.addressStreet);
        pst.setString(12, dto.addressCity);
        pst.setString(13, dto.addressCountry);
        pst.setString(14, dto.addressZipcode);
    }

    private static ClientDto toDto(ResultSet rs) throws SQLException {
        ClientDto dto = new ClientDto();
        dto.id = rs.getString("id");
        dto.version = rs.getLong("version");
        dto.nif = rs.getString("nif");
        dto.name = rs.getString("name");
        dto.surname = rs.getString("surname");
        dto.addressStreet = rs.getString("street");
        dto.addressCity = rs.getString("city");
        dto.addressCountry = rs.getString("country");
        dto.addressZipcode = rs.getString("postalCode");
        dto.phone = rs.getString("phone");
        dto.email = rs.getString("email");
        return dto;
    }

    private static String findIdByNif(Connection connection, String nif) {
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence("TCLIENTS_FIND_BY_NIF"))) {
            pst.setString(1, nif);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getString("id") : null;
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static boolean exists(Connection connection, String key,
            Object... values) {
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence(key))) {
            for (int i = 0; i < values.length; i++) {
                pst.setObject(i + 1, values[i]);
            }
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static void execute(Connection connection, String key,
            Object... values) {
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence(key))) {
            for (int i = 0; i < values.length; i++) {
                pst.setObject(i + 1, values[i]);
            }
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
