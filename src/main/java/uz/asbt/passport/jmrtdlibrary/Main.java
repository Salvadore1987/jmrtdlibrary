package uz.asbt.passport.jmrtdlibrary;

import uz.asbt.passport.jmrtdlibrary.model.PassportData;
import uz.asbt.passport.jmrtdlibrary.service.Scanner;
import uz.asbt.passport.jmrtdlibrary.service.impl.ScannerImpl;

public class Main {

    public static void main(String[] args) {
        try {
            Scanner scanner = new ScannerImpl();
            PassportData passportData = scanner.scan(null, 1000);
            System.out.println(passportData.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
