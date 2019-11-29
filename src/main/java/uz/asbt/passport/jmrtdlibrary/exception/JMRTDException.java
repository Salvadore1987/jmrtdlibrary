package uz.asbt.passport.jmrtdlibrary.exception;

/**
 * @author Sagitov Eldar Technologic ASBT©
 * @version 1.0.0
 * @see java.lang.Exception
 */
public class JMRTDException extends Exception {

    private int code;

    public JMRTDException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
