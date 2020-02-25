package uz.asbt.passport.jmrtdlibrary.model;

import lombok.Data;
import org.jmrtd.lds.icao.MRZInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class PassportData {

    private final static Logger logger = LoggerFactory.getLogger(PassportData.class);

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

        public static PassportData build(String mrz) throws ParseException {
            PassportData data = new PassportData();
            try {
                //AB36522180UZB9302246M26040773240293050003324
                //AA12742406UZB930914223041934140993027101328
                String[] lines = mrz.split("\n");
                if (lines.length >= 2) {
                    logger.info("Lines count: {}", lines.length);
                    logger.info("First line: {}", lines[0]);
                    logger.info("Second line: {}", lines[1]);
                    logger.info("Third line: {}", lines[2]);
                    String[] parsed = lines[0].split("<");
                    String documentType = parsed[0];
                    logger.info("Document type: {}", documentType);
                    String issuingState = parsed[1].substring(0, 3);
                    logger.info("Issuing state: {}", issuingState);
                    String lastName = parsed[1].substring(3);
                    logger.info("Last name: {}", lastName);
                    String firstName = parsed[3];
                    logger.info("Name: {}", firstName);
                    //String mrzLine2 = parsed[parsed.length -1].replace("\r\n", "");
                    String mrzLine2 = lines[1];
                    logger.info("MRZ line 2: {}", mrzLine2);
                    String documentNumber = mrzLine2.substring(0, 9).replaceAll("<", "");
                    logger.info("Document number: {}", documentNumber);
                    String nationality = mrzLine2.substring(10, 13).replaceAll("<", "");
                    logger.info("Nationality: {}", nationality);
                    String dateOfBirth = mrzLine2.substring(13, 19).replaceAll("<", "");
                    logger.info("Date of birth: {}", dateOfBirth);
                    String gender = mrzLine2.substring(20, 21).replaceAll("<", "");
                    logger.info("Gender: {}", gender);
                    String dateOfExpiry = mrzLine2.substring(21, 27).replaceAll("<", "");
                    logger.info("Date of expiry: {}", dateOfExpiry);
                    String optionalData = mrzLine2.substring(28, 42).replaceAll("<", "");
                    logger.info("Optional data: {}", optionalData);
                    DateFormat df = new SimpleDateFormat("yyMMdd");
                    data.setDocumentCode(documentType);
                    data.setIssuingState(issuingState);
                    data.setSurname(lastName);
                    data.setName(firstName);
                    data.setNationality(nationality);
                    data.setDocumentNumber(documentNumber);
                    data.setDateOfBirth(df.parse(dateOfBirth));
                    data.setGender(gender.equals("M") ? "MALE" : "FEMALE");
                    data.setDateOfExpiry(df.parse(dateOfExpiry));
                    data.setOptionalData1(optionalData);
                    data.setOptionalData2("");
                    data.setMrzLine1(lines[0].replaceAll("\r", ""));
                    data.setMrzLine2(lines[1].replaceAll("\r", ""));
                }
                if (lines.length == 3) {
                    try {
                        data.setMrzLine3(lines[2].replaceAll("\r", ""));
                    } catch (Exception ex) {
                        data.setMrzLine3("");
                    }
                }
            } catch (Exception ex) {
                System.out.println("Passport data ex: " + data.toString());
                logger.info("Passport data ex: {}", data.toString());
                System.out.println("Passport data ex: " + data.toString());
                throw new ParseException("Can't parse MRZ", 1);
            }
            System.out.println("Passport data return: " + data.toString());
            logger.info("Passport data return: {}", data.toString());
            System.out.println("Passport data return: " + data.toString());
            logger.info("Passport data return: {}", data.toString());
            return data;
        }
    }

}
