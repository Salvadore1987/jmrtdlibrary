# JMRTD Library

A Java library for scanning and reading Machine Readable Zone (MRZ) data from passports and identity documents using JMRTD technology.

## Overview

The JMRTD Library is developed by ASBT (uz.asbt.passport) and provides functionality to:
- Scan passports via serial port connection
- Parse Machine Readable Zone (MRZ) data
- Extract passport information including personal details, document numbers, and dates
- Handle device communication and information retrieval

## Features

- **Passport Scanning**: Read passport data through serial port communication
- **MRZ Parsing**: Parse Machine Readable Zone data from scanned documents
- **Device Management**: Retrieve device information including serial number, version, and product details
- **Data Extraction**: Extract structured passport data including:
  - Document code and issuing state
  - Personal information (name, surname, nationality)
  - Document number and dates (birth, expiry)
  - Gender and optional data fields
  - Photo data (when available)

## Requirements

- Java 11 or higher
- Serial port connection to scanning device
- JMRTD-compatible scanning hardware

## Dependencies

- **JMRTD**: Core JMRTD library (v0.7.12)
- **SCUBA**: Smart card utilities (v0.0.18)
- **Bouncy Castle**: Cryptographic provider (v1.46)
- **JAI ImageIO**: JPEG2000 image support (v1.3.0)
- **JSSC**: Java Simple Serial Connector (v2.8.0)
- **Lombok**: Code generation (v1.18.32)
- **SLF4J**: Logging framework (v1.7.25)
- **Google Guava**: Utility libraries (v25.1-jre)

## Usage

### Basic Usage

```java
import uz.asbt.passport.jmrtdlibrary.service.Scanner;
import uz.asbt.passport.jmrtdlibrary.service.impl.ScannerImpl;
import uz.asbt.passport.jmrtdlibrary.model.PassportData;
import uz.asbt.passport.jmrtdlibrary.model.DeviceInfo;

// Initialize scanner with COM port
Scanner scanner = new ScannerImpl("COM3");

// Get device information
DeviceInfo info = scanner.info();
System.out.println("Device Serial: " + info.getSerialNumber());
System.out.println("Version: " + info.getVersion());

// Scan passport
PassportData passportData = scanner.scan();
System.out.println("Name: " + passportData.getName());
System.out.println("Document Number: " + passportData.getDocumentNumber());
```

### Command Line Usage

```bash
java -jar jmrtd-library.jar COM3
```

### Scanner Interface Methods

- `scan()`: Scan passport using device
- `scan(MRZRequest request, String terminalName)`: Scan with specific request parameters
- `scan(String mrzString)`: Parse MRZ from string
- `info()`: Get device information
- `deviceSerialNumber()`: Get device serial number
- `mrzString()`: Get raw MRZ string

## Building

```bash
mvn clean compile
mvn package
```

## API Models

### PassportData
Contains extracted passport information including personal details, document data, and MRZ lines.

### MRZData
Simplified model containing passport number, birth date, and expiry date.

### DeviceInfo
Device-specific information including serial number, version, and product details.

## Error Handling

The library uses `JMRTDException` for JMRTD-specific errors and standard Java exceptions for general error cases.

## Developer

Developed by Sagitov Eldar  
GitHub: https://github.com/Salvadore1987

## Version

1.0.0