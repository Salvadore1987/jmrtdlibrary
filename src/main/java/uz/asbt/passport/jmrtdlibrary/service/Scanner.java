package uz.asbt.passport.jmrtdlibrary.service;

import uz.asbt.passport.jmrtdlibrary.exception.JMRTDException;
import uz.asbt.passport.jmrtdlibrary.model.DeviceInfo;
import uz.asbt.passport.jmrtdlibrary.model.PassportData;
import uz.asbt.passport.jmrtdlibrary.model.rest.MRZRequest;

import java.util.concurrent.ExecutionException;

public interface Scanner extends AutoCloseable {

    PassportData scan() throws JMRTDException, ExecutionException, InterruptedException;
    PassportData scan(MRZRequest request, String terminalName) throws JMRTDException;
    PassportData scan(String mrzString) throws JMRTDException;
    DeviceInfo info() throws JMRTDException;
    String deviceSerialNumber() throws JMRTDException;
    String deviceNNAVersion() throws JMRTDException;
    String deviceProductInformation() throws JMRTDException;
    String deviceVersion() throws JMRTDException;
    String mrzString() throws JMRTDException;

}
