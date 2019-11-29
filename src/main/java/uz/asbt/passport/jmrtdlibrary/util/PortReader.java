package uz.asbt.passport.jmrtdlibrary.util;

import jssc.*;
import lombok.extern.slf4j.Slf4j;
import uz.asbt.passport.jmrtdlibrary.code.Error;
import uz.asbt.passport.jmrtdlibrary.exception.JMRTDException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class PortReader {

    private SerialPort serialPort;
    private ByteArrayOutputStream baos;
    private boolean isFinished;

    public String read(byte[] inquire, long timeout) throws JMRTDException {
        baos = new ByteArrayOutputStream();
        isFinished = false;
        char cmd = '\0';
        StringBuilder builder = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] b;
        try {
            openCOMPort();
            if (serialPort.writeBytes(inquire)) {
                log.info("Write success");
                long startTime = System.currentTimeMillis();
                while(System.currentTimeMillis() - startTime < timeout) {}
                try {
                    while ((b = serialPort.readBytes(1, 100)) != null) {
                        baos.write(b);
                    }
                } catch (SerialPortTimeoutException ex) {
                    log.error(ex.getMessage());
                }
                byte[] response = baos.toByteArray();
                if (response.length == 0)
                    return builder.toString();
                cmd = (char) response[0];
                switch (cmd) {
                    case 'I':
                    case 'R':
                    case 'V':
                    case 'W':
                    case 'T':
                        builder.append(new String(response));
                        break;
                    default:
                        throw new JMRTDException(Error.COM_PORT_READ_DATA_ERROR.getCode(), "Can't read data from port");
                }
            }
        } catch (SerialPortException | IOException ex) {
            log.error(ex.getMessage());
            throw new JMRTDException(Error.COM_PORT_ERROR.getCode(), "Can't connect to COM-port");
        } finally {
            try {
                if (serialPort.isOpened()) {
                    serialPort.closePort();
                }
            } catch (SerialPortException ex) {
                log.error(ex.getPortName() + " " + ex.getMessage());
            }
        }
        if (cmd == 'I')
            return builder.toString().replace("\r", "\r\n");
        else
            return builder.toString().replaceAll("\\p{C}", "");
    }

    private void openCOMPort() throws JMRTDException {
        Arrays.stream(SerialPortList.getPortNames()).forEach(log::info);
        String[] comPorts = SerialPortList.getPortNames();
        String comPort = null;
        for (int i = 0; i < comPorts.length; i++) {
            comPort = comPorts[i];
            try {
                serialPort = new SerialPort(comPort);
                if (!serialPort.isOpened()) {
                    if (serialPort.openPort()) {
                        log.info("Port opened");
                        serialPort.setParams(
                                SerialPort.BAUDRATE_9600,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);
                        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
                                SerialPort.FLOWCONTROL_RTSCTS_OUT);
                    }
                }
            } catch (SerialPortException ex) {
                log.error(ex.getPortName());
            }
        }
        if (comPort == null)
            throw new JMRTDException(Error.COM_PORT_ERROR.getCode(), "Can't find COM-port");
    }

}
