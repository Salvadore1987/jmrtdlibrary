package uz.asbt.passport.jmrtdlibrary;

import uz.asbt.passport.jmrtdlibrary.model.DeviceInfo;
import uz.asbt.passport.jmrtdlibrary.model.PassportData;
import uz.asbt.passport.jmrtdlibrary.service.Scanner;
import uz.asbt.passport.jmrtdlibrary.service.impl.ScannerImpl;

public class Main {

    public static void main(String[] args) {
        try {
            Scanner scanner = new ScannerImpl();
            DeviceInfo info = scanner.info("COM26");
            System.out.println("SN: " + info.getSerialNumber());
            System.out.println("NNA Version: " + info.getNnaVersion());
            System.out.println("Product info: " + info.getProductInformation());
            System.out.println("Version: " + info.getVersion());
            PassportData passportData = scanner.scan("COM26");
            System.out.println(passportData.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
