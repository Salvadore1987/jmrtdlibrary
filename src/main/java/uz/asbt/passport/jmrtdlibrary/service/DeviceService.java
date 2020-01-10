package uz.asbt.passport.jmrtdlibrary.service;

import jssc.SerialPortList;
import uz.asbt.passport.jmrtdlibrary.exception.JMRTDException;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceService {

    /**
     * Метод возвращает список подключенных портов на устройстве
     * @return - список имен портов
     * @throws JMRTDException в случае ошибки
     */
    public static List<String> comPortList() throws JMRTDException {
        return Arrays.asList(SerialPortList.getPortNames());
    }

    /**
     * Метод возвращает список терминалов подключенных к устройству
     * @return список имен
     * @throws CardException в случае ошибки
     */
    public static List<String> terminalList() throws CardException {
        final List<String> terminalList = new ArrayList<>();
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();
        terminals.stream().forEach(i -> terminalList.add(i.getName()));
        return terminalList;
    }

}
