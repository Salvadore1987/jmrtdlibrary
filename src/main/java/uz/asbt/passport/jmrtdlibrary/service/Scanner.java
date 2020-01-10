package uz.asbt.passport.jmrtdlibrary.service;

import uz.asbt.passport.jmrtdlibrary.exception.JMRTDException;
import uz.asbt.passport.jmrtdlibrary.model.DeviceInfo;
import uz.asbt.passport.jmrtdlibrary.model.PassportData;
import uz.asbt.passport.jmrtdlibrary.model.rest.MRZRequest;

import java.util.concurrent.ExecutionException;

public interface Scanner {

    PassportData scan(String comPort) throws JMRTDException, ExecutionException, InterruptedException;
    PassportData scan(MRZRequest request, String terminalName, String comPort) throws JMRTDException;
    DeviceInfo info(String comPort) throws JMRTDException;
    String deviceSerialNumber(String comPort) throws JMRTDException;
    String deviceNNAVersion(String comPort) throws JMRTDException;
    String deviceProductInformation(String comPort) throws JMRTDException;
    String deviceVersion(String comPort) throws JMRTDException;
    String mrzString(String comPort) throws JMRTDException;

}
