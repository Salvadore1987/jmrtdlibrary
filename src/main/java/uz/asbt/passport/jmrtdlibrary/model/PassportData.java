package uz.asbt.passport.jmrtdlibrary.model;

import lombok.Data;
import org.jmrtd.lds.icao.MRZInfo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class PassportData {

    private String documentCode;
    private String issuingState;
    private String surname;
    private String name;
    private String nationality;
    private String documentNumber;
    private Date dateOfBirth;
    private String gender;
    private Date dateOfExpiry;
    private String optionalData1;
    private String optionalData2;
    private String mrzLine1;
    private String mrzLine2;
    private String mrzLine3;
    private byte[] photo;

    private PassportData(){}

    public static class PassportBuilder {
        public static PassportData build(MRZInfo mrzInfo) throws ParseException {
            PassportData data = new PassportData();
            DateFormat df = new SimpleDateFormat("yyMMdd");
            data.setDocumentCode(mrzInfo.getDocumentCode());
            data.setIssuingState(mrzInfo.getIssuingState());
            data.setSurname(mrzInfo.getPrimaryIdentifier());
            data.setName(mrzInfo.getSecondaryIdentifier().replace("<", ""));
            data.setNationality(mrzInfo.getNationality());
            data.setDocumentNumber(mrzInfo.getDocumentNumber());
            data.setDateOfBirth(df.parse(mrzInfo.getDateOfBirth()));
            data.setGender(mrzInfo.getGender().name());
            data.setDateOfExpiry(df.parse(mrzInfo.getDateOfExpiry()));
            data.setOptionalData1(mrzInfo.getOptionalData1());
            data.setOptionalData2(mrzInfo.getOptionalData2());
            return data;
        }
    }

}
