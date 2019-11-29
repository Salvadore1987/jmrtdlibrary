package uz.asbt.passport.jmrtdlibrary.service;

import uz.asbt.passport.jmrtdlibrary.exception.JMRTDException;
import uz.asbt.passport.jmrtdlibrary.model.DeviceInfo;
import uz.asbt.passport.jmrtdlibrary.model.PassportData;
import uz.asbt.passport.jmrtdlibrary.model.rest.MRZRequest;

public interface Scanner {

    PassportData scan(String terminalName, long timeout) throws JMRTDException;
    PassportData scan(MRZRequest request, String terminalName, long timeout) throws JMRTDException;
    DeviceInfo info(long timeout) throws JMRTDException;
    String deviceSerialNumber(long timeout) throws JMRTDException;
    String deviceNNAVersion(long timeout) throws JMRTDException;
    String deviceProductInformation(long timeout) throws JMRTDException;
    String deviceVersion(long timeout) throws JMRTDException;
    String mrzString(long timeout) throws JMRTDException;

}
