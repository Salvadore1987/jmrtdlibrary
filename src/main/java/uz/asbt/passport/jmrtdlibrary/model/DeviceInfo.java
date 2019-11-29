package uz.asbt.passport.jmrtdlibrary.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {

    private String serialNumber;
    private String productInformation;
    private String nnaVersion;
    private String version;

}
