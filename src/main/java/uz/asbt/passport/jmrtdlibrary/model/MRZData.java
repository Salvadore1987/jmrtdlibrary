package uz.asbt.passport.jmrtdlibrary.model;

import lombok.Data;
import uz.asbt.passport.jmrtdlibrary.model.rest.MRZRequest;

import java.text.ParseException;

@Data
public class MRZData {

    private String passport;
    private String dateOfBirth;
    private String expiredDate;

    private MRZData() {}

    private MRZData(String passport, String dateOfBirth, String expiredDate) {
        this.passport = passport;
        this.dateOfBirth = dateOfBirth;
        this.expiredDate = expiredDate;
    }

    public static class MRZBuilder {
        public static MRZData build(String mrz) throws ParseException {
            MRZData info = new MRZData();
            String[] mrzArr = mrz.split("\n");
            info.setPassport(mrzArr[1].substring(0, 9));
            String birthDate = mrzArr[1].substring(13, 19);
            info.setDateOfBirth(birthDate);
            String expiry = mrzArr[1].substring(21, 27);
            info.setExpiredDate(expiry);
            return info;
        }

        public static MRZData build(MRZRequest request) throws ParseException {
            MRZData info = new MRZData();
            String[] birthDateArr = request.getDateOfBirth().split("\\.");
            String birthDate = birthDateArr[2].substring(2, 4).concat(birthDateArr[1]).concat(birthDateArr[0]);
            String[] expiryDateArr = request.getDateOfExpiry().split("\\.");
            String expiryDate = expiryDateArr[2].substring(2, 4).concat(expiryDateArr[1]).concat(expiryDateArr[0]);
            info.setPassport(request.getPassportNumber());
            info.setDateOfBirth(birthDate);
            info.setExpiredDate(expiryDate);
            return info;
        }
    }

}
