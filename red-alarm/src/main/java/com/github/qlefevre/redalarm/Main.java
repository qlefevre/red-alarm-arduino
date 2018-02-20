/*
 * Copyright 2018  Quentin Lefèvre
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.qlefevre.redalarm;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import org.apache.commons.cli.CommandLine;

/**
 * Main program red alarm
 *
 * @author qlefevre
 */
public class Main extends AbstractCommonMain {

    private static final String COMMAND_ON = "on";

    private static final String COMMAND_OFF = "off";

    public static void main(String[] pArgs) throws Exception {
        CommandLine vCommandLine = getCommandLine(pArgs);

        String vUser = vCommandLine.getOptionValue("u");
        String vPassword = vCommandLine.getOptionValue("p");
        String vUrl = vCommandLine.getOptionValue("s");

        checkURL(vUrl, vUser, vPassword);

        String vFirstPort = null;
        if (!vCommandLine.hasOption('c')) {
            List<CommPortIdentifier> vPorts = Collections.list(CommPortIdentifier.getPortIdentifiers());
            for (CommPortIdentifier vPort : vPorts) {
                if (vPort.getPortType() == CommPortIdentifier.PORT_SERIAL && (vPort.getName().contains("ttyUSB") || vPort.getName().contains("COM"))) {
                    System.out.println(vPort.getName() + " - " + vPort.getPortType());
                    if (vFirstPort == null) {
                        vFirstPort = vPort.getName();
                    }
                }
            }
        } else {
            vFirstPort = vCommandLine.getOptionValue("c");
        }

        System.out.println("Port sélectionné : " + vFirstPort);
        CommPortIdentifier vPortId = CommPortIdentifier.getPortIdentifier(vFirstPort);
        SerialPort vSerialPort = (SerialPort) vPortId.open("Demo application", 5000);

        int vBaudRate = 9600; // 9600

        // Set serial port to 9600bps-8N1..my favourite
        vSerialPort.setSerialPortParams(
                vBaudRate,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        vSerialPort.setFlowControlMode(
                SerialPort.FLOWCONTROL_NONE);

        final OutputStream vOutStream = vSerialPort.getOutputStream();

        System.out.println("Program start");
        testSequence(vOutStream);

        boolean vAlert = false;
        while (true) {
            if (checkURL(vUrl, vUser, vPassword)) {
                System.out.println("Alert START");
                sendCommand(vOutStream, COMMAND_ON);
                vAlert = true;
            } else if (vAlert) {
                System.out.println("Alert STOP");
                sendCommand(vOutStream, COMMAND_OFF);
                vAlert = false;
            }
            Thread.sleep(30000);
        }
    }

    private static void testSequence(OutputStream pOutStream) throws InterruptedException, IOException {
        System.out.println("Test start");
        Thread.sleep(2000);
        sendCommand(pOutStream, COMMAND_ON);
        Thread.sleep(4000);
        sendCommand(pOutStream, COMMAND_OFF);
        Thread.sleep(4000);
        sendCommand(pOutStream, COMMAND_ON);
        Thread.sleep(4000);
        sendCommand(pOutStream, COMMAND_OFF);
        System.out.println("Test stop");
    }

    private static void sendCommand(OutputStream pOutStream, String pCommand) throws IOException {
        System.out.println("Program " + pCommand);
        pOutStream.write(pCommand.getBytes());
    }

    private static void arduinoOutput(final InputStream pVInStream) {
        new Thread() {
            @Override
            public void run() {
                try {
                    byte vBuffer[] = new byte[1024];
                    int vSize;
                    while (true) {
                        Thread.sleep(500);
                        String vOutputArduino = "";
                        do {
                            vSize = pVInStream.read(vBuffer);
                            if (vSize > 0) {
                                vOutputArduino += new String(vBuffer, 0, vSize);
                            }
                            Thread.sleep(400);
                        } while (vSize > 0);

                        System.out.println("Arduino " + vOutputArduino);
                    }
                } catch (Exception vEx) {
                    System.err.println(vEx);
                }
            }
        }.start();
    }

}
