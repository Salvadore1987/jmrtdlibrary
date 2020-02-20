package uz.asbt.passport.jmrtdlibrary.code;

public enum Error {
    CARD_DATA_ERROR(1100),
    COM_PORT_ERROR(1101),
    COM_PORT_READ_DATA_ERROR(1103),
    CARD_ERROR(1104),
    OBJECT_PARSE_ERROR(1105),
    INFORMATION_STREAM_ERROR(1106),
    COM_PORT_WRITE_ERROR(1107),
    MRZ_READ_ERROR(1108),
    MRZ_DATA_EMPTY(1109),
    MRZ_READ_TIMEOUT(1110),
    COM_PORT_OPEN_ERROR(1111),
    TERMINAL_NOT_FOUND(1112),
    CARD_NOT_PRESENT(1113)
    ;

    private int code;

    Error(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
