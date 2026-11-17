package uo.ri.cws.application.service.client.crud.commands;

import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.domain.types.Address;
import uo.ri.cws.domain.types.Email;
import uo.ri.cws.domain.types.FullName;
import uo.ri.cws.domain.types.Nif;
import uo.ri.cws.domain.types.PhoneNumber;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessChecks;
import uo.ri.util.exception.BusinessException;

/**
 * Checks shared by the commands that receive a full ClientDto
 */
final class ClientDtoChecks {

	private ClientDtoChecks() {
	}

	/**
	 * Programming errors: the dto or any of its fields are missing
	 */
	static void checkFields(ClientDto dto) {
		ArgumentChecks.isNotNull(dto, "The client dto cannot be null");
		ArgumentChecks.isNotBlank(dto.nif, "The client nif cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.name, "The client name cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.surname, "The client surname cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.addressStreet, "The street cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.addressCity, "The city cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.addressCountry, "The country cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.addressZipcode, "The zip code cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.phone, "The phone cannot be null or blank");
		ArgumentChecks.isNotBlank(dto.email, "The email cannot be null or blank");
	}

	/**
	 * User errors: the data typed by the user does not have a valid format
	 */
	static void checkFormats(ClientDto dto) throws BusinessException {
		BusinessChecks.isTrue(Nif.isValid(dto.nif), "The nif is not valid");
		BusinessChecks.isTrue(FullName.isValid(dto.name, dto.surname), "The name and surname are required");
		BusinessChecks.isTrue(Email.isValid(dto.email), "The email is not valid");
		BusinessChecks.isTrue(PhoneNumber.isValid(dto.phone), "The phone is not valid");
		BusinessChecks.isTrue(
				Address.isValid(dto.addressStreet, dto.addressCity, dto.addressCountry, dto.addressZipcode),
				"The address is not complete");
	}

}
