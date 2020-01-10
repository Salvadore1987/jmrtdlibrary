package uz.asbt.passport.jmrtdlibrary.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {

    protected String serialNumber;
    protected String productInformation;
    protected String nnaVersion;
    protected String version;

}
