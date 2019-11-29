package uz.asbt.passport.jmrtdlibrary.model.rest;

import lombok.Data;

@Data
public class MRZRequest {

    private String passportNumber;
    private String dateOfBirth;
    private String dateOfExpiry;

}
