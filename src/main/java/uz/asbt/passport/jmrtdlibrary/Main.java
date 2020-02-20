package uz.asbt.passport.jmrtdlibrary;

import jssc.SerialPortList;
import uz.asbt.passport.jmrtdlibrary.model.DeviceInfo;
import uz.asbt.passport.jmrtdlibrary.model.PassportData;
import uz.asbt.passport.jmrtdlibrary.service.Scanner;
import uz.asbt.passport.jmrtdlibrary.service.impl.ScannerImpl;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        try {
            Arrays.stream(SerialPortList.getPortNames()).forEach(System.out::println);
            if (args.length != 1) {
                throw new IllegalArgumentException("Please send COM port");
            }
            System.out.println(args[0]);
            Scanner scanner = new ScannerImpl(args[0]);
            DeviceInfo info = scanner.info();
            System.out.println("SN: " + info.getSerialNumber());
            System.out.println("NNA Version: " + info.getNnaVersion());
            System.out.println("Product info: " + info.getProductInformation());
            System.out.println("Version: " + info.getVersion());
            PassportData passportData = scanner.scan(args[0]);
            System.out.println(passportData.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
