package uz.asbt.passport.jmrtdlibrary.service.impl;

import jssc.*;
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
import uz.asbt.passport.jmrtdlibrary.util.PortReader;

import javax.imageio.ImageIO;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

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

    /**
     * Данный метод считывает данные из паспорта, формирует объект @see PassportData и возвращает его.
     * Работает только с ридерами "Elyctis" или совместимыми с ним.
     * Автоматически считывает MRZ через OCR сканнер.
     * @param terminalName имя устройства с которового производится чтение.
     * @return @see PassportData
     * @throws JMRTDException в случае ошибки.
     */
    @Override
    public PassportData scan(String terminalName, long timeout) throws JMRTDException {
        String mrz = readMrz(timeout);
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
    public PassportData scan(MRZRequest request, String terminalName, long timeout) throws JMRTDException {
        return passportData(null,null, request);
    }

    @Override
    public DeviceInfo info(long timeout) throws JMRTDException {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setSerialNumber(readDeviceSN(timeout));
        deviceInfo.setNnaVersion(readDeviceNNAVersion(timeout));
        deviceInfo.setProductInformation(readDeviceProductInformation(timeout));
        deviceInfo.setVersion(readDeviceVersion(timeout));
        return deviceInfo;
    }

    @Override
    public String deviceSerialNumber(long timeout) throws JMRTDException {
        return readDeviceSN(timeout);
    }

    @Override
    public String deviceNNAVersion(long timeout) throws JMRTDException {
        return readDeviceNNAVersion(timeout);
    }

    @Override
    public String deviceProductInformation(long timeout) throws JMRTDException {
        return readDeviceProductInformation(timeout);
    }

    @Override
    public String deviceVersion(long timeout) throws JMRTDException {
        return readDeviceVersion(timeout);
    }

    @Override
    public String mrzString(long timeout) throws JMRTDException {
        return readMrz(timeout);
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
            CardService cardService = CardService.getInstance(getTerminal(terminalName));
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
                String line1 = mrzArr[0].replaceAll("\\p{C}", "");
                passportData.setMrzLine1(line1.substring(2, line1.length()));
                passportData.setMrzLine2(mrzArr[1].replaceAll("\\p{C}", ""));
                passportData.setMrzLine3(mrzArr[2].replaceAll("\\p{C}", ""));
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

    private CardTerminal getTerminal(String terminalName) throws JMRTDException, CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();
        CardTerminal terminal;

        if (null == terminalName) {
            terminal = terminals.get(0);
        } else {
            terminal = terminals.stream()
                    .filter(t -> t.getName().equals(terminalName))
                    .findFirst()
                    .orElse(null);
        }

        if (null == terminal)
            throw new JMRTDException(-1, "Terminal not found");
        return terminal;
    }

    private BACKey getBAC(MRZData mrzData) {
        return new BACKey(
                mrzData.getPassport(),
                mrzData.getDateOfBirth(),
                mrzData.getExpiredDate()
        );
    }

    /**
     * Метод считывает MRZ-зону с помошью OCR-сканера, если устройство поддерживает такую возможность.
     * @return строку MRZ
     * @throws SerialPortException - в случае возникновения ошибки.
     */
    private String readMrz(long timeout) throws JMRTDException {
        byte[] inquire = {0x49, 0, 0};
        return new PortReader().read(inquire, timeout);
    }

    private String readDeviceSN(long timeout) throws JMRTDException {
        byte[] inquire = {0x52, 0, 0};
        return new PortReader().read(inquire, timeout);
    }

    private String readDeviceVersion(long timeout) throws JMRTDException {
        byte[] inquire = {0x56, 0, 0};
        return new PortReader().read(inquire, timeout);
    }

    private String readDeviceNNAVersion(long timeout) throws JMRTDException {
        byte[] inquire = {0x57, 0, 0};
        return new PortReader().read(inquire, timeout);
    }

    private String readDeviceProductInformation(long timeout) throws JMRTDException {
        byte[] inquire = {0x54, 0, 0};
        return new PortReader().read(inquire, timeout);
    }

}
