package uz.asbt.passport.jmrtdlibrary.service.impl;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import lombok.extern.slf4j.Slf4j;
import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;
import org.jmrtd.BACKey;
import org.jmrtd.PassportService;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;
import uz.asbt.passport.jmrtdlibrary.code.Error;
import uz.asbt.passport.jmrtdlibrary.exception.JMRTDException;
import uz.asbt.passport.jmrtdlibrary.model.DeviceInfo;
import uz.asbt.passport.jmrtdlibrary.model.MRZData;
import uz.asbt.passport.jmrtdlibrary.model.PassportData;
import uz.asbt.passport.jmrtdlibrary.model.rest.MRZRequest;
import uz.asbt.passport.jmrtdlibrary.service.Scanner;

import javax.imageio.ImageIO;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Класс обертка, предназначен для работы со считывателями.
 * @author Sagitov Eldar
 * @version 1.0.0
 * @see SerialPort
 * @see SerialPortException
 * @see SerialPortTimeoutException
 * @see CardService
 * @see PassportService
 * @see CardTerminal
 */
@Slf4j
public class ScannerImpl implements Scanner {

    private SerialPort serialPort;
    private String comPort;

    public ScannerImpl(String comPort) {
        this.comPort = comPort;
        this.connect();
    }

    /**
     * Метод для открытия соедеинения с указанным COM-портом
     * @return true - если соединение успешно открыто
     */
    private boolean connect() {
        try {
            serialPort = new SerialPort(this.comPort);
            if (serialPort.isOpened())
                return true;
            if (serialPort.openPort()) {
                log.info("Port opened");
                serialPort.setParams(
                        SerialPort.BAUDRATE_9600,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
                        SerialPort.FLOWCONTROL_RTSCTS_OUT);
                return true;
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return false;
    }

    /**
     * Метод закрывает соединение с указанным портом
     * @return true - в случае успешного закрытия
     */
    private boolean disconnect() {
        try {
            return serialPort.closePort();
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return false;
    }

    /**
     * Данный метод считывает данные из паспорта, формирует объект @see PassportData и возвращает его.
     * Работает только с ридерами "Elyctis" или совместимыми с ним.
     * Автоматически считывает MRZ через OCR сканнер.
     * @return @see PassportData
     * @throws JMRTDException в случае ошибки.
     */
    @Override
    public PassportData scan() throws JMRTDException, ExecutionException, InterruptedException {
        String mrz = readMrz();
        System.out.println("MRZ: " + mrz);
        return passportData(mrz, null, null);
    }

    /**
     * Данный метод считывает данные из паспорта, формирует объект @see PassportData и возвращает его.
     * Работает с любыми устройствами считывания бесконтактной карты, для этого необходимо передавать объект
     * MRZ в метод.
     * @param request Объект с данными для прохождения BAC-аутентификации.
     * @param terminalName имя устройства с которового производится чтение.
     * @return @see PassportData
     * @throws JMRTDException в случае возникновения ошибки.
     */
    @Override
    public PassportData scan(MRZRequest request, String terminalName) throws JMRTDException {
        return passportData(null,null, request);
    }

    @Override
    public DeviceInfo info() throws JMRTDException {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setSerialNumber(readDeviceSN());
        deviceInfo.setNnaVersion(readDeviceNNAVersion());
        deviceInfo.setProductInformation(readDeviceProductInformation());
        deviceInfo.setVersion(readDeviceVersion());
        return deviceInfo;
    }

    @Override
    public String deviceSerialNumber() throws JMRTDException {
        return readDeviceSN();
    }

    @Override
    public String deviceNNAVersion() throws JMRTDException {
        return readDeviceNNAVersion();
    }

    @Override
    public String deviceProductInformation() throws JMRTDException {
        return readDeviceProductInformation();
    }

    @Override
    public String deviceVersion() throws JMRTDException {
        return readDeviceVersion();
    }

    @Override
    public String mrzString() throws JMRTDException {
        return readMrz();
    }

    @Override
    public PassportData scan(String mrzString) throws JMRTDException {
        PassportData passportData;
        PassportService passportService = null;
        try {
            log.info("MRZ string is {}", mrzString);
            CardTerminal terminal = getTerminal(null);
            if (!terminal.isCardPresent())
                throw new JMRTDException(Error.CARD_ERROR.getCode(), "Card not present");
            CardService cardService = CardService.getInstance(terminal);
            passportService = new PassportService(
                    cardService,
                    PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                    PassportService.DEFAULT_MAX_BLOCKSIZE,
                    true,
                    false
            );
            passportService.open();
            // Нужно чтобы при вызове метода doBAC не выбрасывало Exception
            passportService.sendSelectApplet(false);
            if (null == mrzString) {
                throw new JMRTDException(Error.MRZ_DATA_EMPTY.getCode(), "MRZ data is empty");
            }

            MRZData mrzData = MRZData.MRZBuilder.build(mrzString);
            passportService.doBAC(getBAC(mrzData));

            DG1File dg1 = getDg1(passportService);
            DG2File dg2 = getDg2(passportService);

            passportData = PassportData.PassportBuilder.build(dg1.getMRZInfo());
            String[] mrzArr = mrzString.split("\n");
            if (mrzArr.length == 2) {
                String line1 = mrzArr[0].replaceAll("\\p{C}", "");
                passportData.setMrzLine1(line1);
                passportData.setMrzLine2(mrzArr[1].replaceAll("\\p{C}", ""));
            } else if (mrzArr.length == 3) {
                String line1 = mrzArr[0].replaceAll("\\p{C}", "");
                passportData.setMrzLine1(line1);
                passportData.setMrzLine2(mrzArr[1].replaceAll("\\p{C}", ""));
                passportData.setMrzLine3(mrzArr[2].replaceAll("\\p{C}", ""));
            } else {
                throw new JMRTDException(Error.MRZ_DATA_EMPTY.getCode(), "MRZ data is empty");
            }
            passportData.setPhoto(getPhoto(dg2));

        } catch (CardException ex) {
            log.error(ex.getMessage());
            throw new JMRTDException(Error.CARD_ERROR.getCode(), "Can't connect to card");
        } catch (CardServiceException ex) {
            log.error(ex.getMessage());
            throw new JMRTDException(Error.CARD_DATA_ERROR.getCode(), "Can't getting data from passport");
        } catch (ParseException ex) {
            log.error(ex.getMessage());
            throw new JMRTDException(Error.OBJECT_PARSE_ERROR.getCode(), "Can't parse object");
        } catch (IOException ex) {
            log.error(ex.getMessage());
            throw new JMRTDException(Error.INFORMATION_STREAM_ERROR.getCode(), "Can't get input stream from card");
        } finally {
            log.info("PassportService close");
            if (null != passportService)
                passportService.close();
        }
        return passportData;
    }

    /**
     * Подключается к устройству, считывает данные с паспорта, формирует объект PassportData
     * и возвращает его.
     * @param mrz - строка MRZ
     * @param terminalName - название устройства считывания
     * @param request - объект с данными по MRZ, если устройство не считывает его автоматически.
     * @return - заполненный объект PassportData
     * @throws JMRTDException - в случае ошибки.
     */
    private PassportData passportData(
            String mrz,
            String terminalName,
            MRZRequest request) throws JMRTDException {
        PassportData passportData;
        PassportService passportService = null;
        try {
            log.info("MRZ string is {}", mrz);
            CardTerminal cardTerminal = getTerminal(terminalName);
            if (!cardTerminal.isCardPresent()) {
                throw new JMRTDException(Error.CARD_NOT_PRESENT.getCode(), "Card not present");
            }
            CardService cardService = CardService.getInstance(cardTerminal);
            passportService = new PassportService(
                    cardService,
                    PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                    PassportService.DEFAULT_MAX_BLOCKSIZE,
                    true,
                    false
            );
            passportService.open();
            // Нужно чтобы при вызове метода doBAC не выбрасывало Exception
            passportService.sendSelectApplet(false);
            MRZData mrzData;
            if (null != mrz) {
                mrzData = MRZData.MRZBuilder.build(mrz);
            } else {
                mrzData = MRZData.MRZBuilder.build(request);
            }

            passportService.doBAC(getBAC(mrzData));

            DG1File dg1 = getDg1(passportService);
            DG2File dg2 = getDg2(passportService);

            passportData = PassportData.PassportBuilder.build(dg1.getMRZInfo());
            if (mrz != null) {
                String[] mrzArr = mrz.split("\n");
                if (mrzArr.length == 2) {
                    String line1 = mrzArr[0].replaceAll("\\p{C}", "");
                    passportData.setMrzLine1(line1);
                    passportData.setMrzLine2(mrzArr[1].replaceAll("\\p{C}", ""));
                } else if (mrzArr.length == 3) {
                    String line1 = mrzArr[0].replaceAll("\\p{C}", "");
                    passportData.setMrzLine1(line1);
                    passportData.setMrzLine2(mrzArr[1].replaceAll("\\p{C}", ""));
                    passportData.setMrzLine3(mrzArr[2].replaceAll("\\p{C}", ""));
                } else {
                    throw new JMRTDException(Error.MRZ_DATA_EMPTY.getCode(), "MRZ data is empty");
                }
            }
            passportData.setPhoto(getPhoto(dg2));

        } catch (CardException ex) {
            log.error(ex.getMessage());
            throw new JMRTDException(Error.CARD_ERROR.getCode(), "Can't connect to card");
        } catch (CardServiceException ex) {
            log.error(ex.getMessage());
            throw new JMRTDException(Error.CARD_DATA_ERROR.getCode(), "Can't getting data from passport");
        } catch (ParseException ex) {
            log.error(ex.getMessage());
            throw new JMRTDException(Error.OBJECT_PARSE_ERROR.getCode(), "Can't parse object");
        } catch (IOException ex) {
            log.error(ex.getMessage());
            throw new JMRTDException(Error.INFORMATION_STREAM_ERROR.getCode(), "Can't get input stream from card");
        } finally {
            log.info("PassportService close");
            if (null != passportService)
                passportService.close();
        }
        return passportData;
    }

    /**
     * Метод возвращает изображение фото в байтовом представлении.
     * @param dg2 - Объект класса @see DG2File
     * @return фото
     * @throws IOException в случае если не удалось считать изображение.
     */
    private byte[] getPhoto(DG2File dg2) throws IOException {
        List<FaceInfo> infos = dg2.getFaceInfos();
        List<FaceImageInfo> imageInfos = infos.get(0).getFaceImageInfos();
        FaceImageInfo faceImageInfo = imageInfos.get(0);
        InputStream inputStream = faceImageInfo.getImageInputStream();
        BufferedImage image = ImageIO.read(inputStream);

        byte[] imageInByte;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            baos.flush();
            imageInByte = baos.toByteArray();
        }
        return imageInByte;
    }

    private DG1File getDg1(PassportService passportService) throws CardServiceException, IOException {
        InputStream is = passportService.getInputStream(PassportService.EF_DG1);
        return  (DG1File) LDSFileUtil.getLDSFile(PassportService.EF_DG1, is);
    }

    private DG2File getDg2(PassportService passportService) throws CardServiceException, IOException {
        InputStream is = passportService.getInputStream(PassportService.EF_DG2);
        return (DG2File) LDSFileUtil.getLDSFile(PassportService.EF_DG2, is);
    }

    private CardTerminal getTerminal(String terminalName) throws JMRTDException {
        CardTerminal terminal;
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            log.info("Default factory is present");
            List<CardTerminal> terminals = factory.terminals().list();
            log.info("Getting list of terminals");
            terminals.forEach(i -> log.info(i.getName()));

            if (terminalName == null) {
                if (terminals.size() == 0) {
                    log.info("Terminal size is 0");
                    throw new JMRTDException(Error.TERMINAL_NOT_FOUND.getCode(), "Terminal not found");
                } else if (terminals.size() == 1) {
                    log.info("Terminal size is 1");
                    terminal = terminals.get(0);
                } else {
                    log.info("Terminal ELYCTIS");
                    final String elyctisTerminal = "ELYCTIS CL";
                    terminal = terminals.stream()
                            .filter(t -> t.getName().contains(elyctisTerminal))
                            .findFirst()
                            .orElse(null);
                    if (terminal == null)
                        throw new JMRTDException(Error.TERMINAL_NOT_FOUND.getCode(), "Terminal not found");
                }
            } else {
                log.info("Terminal Other: {}", terminalName);
                terminal = terminals.stream()
                        .filter(t -> t.getName().contains(terminalName))
                        .findFirst()
                        .orElse(null);
                if (terminal == null)
                    throw new JMRTDException(Error.TERMINAL_NOT_FOUND.getCode(), "Terminal not found");
            }
        } catch (CardException ex) {
            throw new JMRTDException(Error.TERMINAL_NOT_FOUND.getCode(), "Terminal not found");
        }
        return terminal;
    }

    private BACKey getBAC(MRZData mrzData) {
        return new BACKey(
                mrzData.getPassport(),
                mrzData.getDateOfBirth(),
                mrzData.getExpiredDate()
        );
    }

    private synchronized String readDataFromScanner(byte[] inquire) throws JMRTDException {
        StringBuilder builder = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        char cmd;
        try {
            if (this.serialPort.isOpened()) {
                this.serialPort.closePort();
            }
            boolean isConnected = connect();
            if (isConnected) {
                System.out.println("Begin write data into port");
                if (serialPort.writeBytes(inquire)) {
                    int length = 0;

                    int i = 0;
                    // Wait for a response from the scanner
                    for (; i < 10000; i += 10) {
                        Thread.sleep(10);
                        if ((length = serialPort.getInputBufferBytesCount()) > 2) {
                            break;
                        }
                    }

                    byte[] data = serialPort.readBytes(length, 100000);
                    if (data.length == 0)
                        throw new JMRTDException(Error.COM_PORT_READ_DATA_ERROR.getCode(), "Can't read data from port");
                    cmd = (char) data[0];

                    for (int j = 3; j < data.length; j++) {
                        baos.write(data[j]);
                    }
                    byte[] response = baos.toByteArray();
                    System.out.println(cmd);
                    if (response.length == 0)
                        return builder.toString();

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
                } else {
                    throw new JMRTDException(Error.COM_PORT_WRITE_ERROR.getCode(), "Can't write data into COM-Port");
                }
            } else {
                throw new JMRTDException(Error.COM_PORT_OPEN_ERROR.getCode(), "Can't open COM-port");
            }
        } catch (SerialPortException ex) {
            throw new JMRTDException(Error.MRZ_READ_ERROR.getCode(), "Can't read MRZ");
        } catch (SerialPortTimeoutException ex) {
            throw new JMRTDException(Error.MRZ_READ_TIMEOUT.getCode(), "Timeout when read MRZ data");
        } catch (InterruptedException ex) {
            throw new JMRTDException(Error.MRZ_READ_TIMEOUT.getCode(), "Timeout when read MRZ data");
        } finally {
            disconnect();
        }
        log.info("CMD Command is: {}", cmd);
        if (cmd == 'I')
            return builder.toString().replace("\r", "\r\n");
        return builder.toString().replaceAll("\\p{C}", "");
    }

    /**
     * Метод считывает MRZ-зону с помошью OCR-сканера, если устройство поддерживает такую возможность.
     * @return строку MRZ
     * @throws SerialPortException - в случае возникновения ошибки.
     */
    private String readMrz() throws JMRTDException {
        byte[] inquire = {0x49, 0, 0};
        return readDataFromScanner(inquire);
    }

    private String readDeviceSN() throws JMRTDException {
        byte[] inquire = {0x52, 0, 0};
        return readDataFromScanner(inquire);
    }

    private String readDeviceVersion() throws JMRTDException {
        byte[] inquire = {0x56, 0, 0};
        return readDataFromScanner(inquire);
    }

    private String readDeviceNNAVersion() throws JMRTDException {
        byte[] inquire = {0x57, 0, 0};
        return readDataFromScanner(inquire);
    }

    private String readDeviceProductInformation() throws JMRTDException {
        byte[] inquire = {0x54, 0, 0};
        return readDataFromScanner(inquire);
    }

    @Override
    public void close() throws Exception {
        if (this.serialPort.isOpened()) {
            this.serialPort.purgePort(2);
            this.serialPort.purgePort(8);
            this.serialPort.purgePort(1);
            this.serialPort.purgePort(4);
            this.serialPort.closePort();
        }
    }

}
