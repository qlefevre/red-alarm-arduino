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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author dsdsystem
 */
public class Main {

    private static final String COMMAND_ON = "on";

    private static final String COMMAND_OFF = "off";

    public static void main(String[] pArgs) throws Exception {

        String vUser = "redalert";
        String vPassword = "g2euxprwz2da667a";
        String vUrl = "http://lesaascestsensas.altairsystem.fr";

        checkURL(vUrl, vUser,vPassword);

        List<CommPortIdentifier> vPorts = Collections.list(CommPortIdentifier.getPortIdentifiers());
        String vFirstPort = null;
        for (CommPortIdentifier vPort : vPorts) {
            if (vPort.getPortType() == CommPortIdentifier.PORT_SERIAL && (vPort.getName().contains("ttyUSB") || vPort.getName().contains("COM4"))) {
                System.out.println(vPort.getName() + " - " + vPort.getPortType());
                if (vFirstPort == null) {
                    vFirstPort = vPort.getName();
                }
            }
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
            if (checkURL(vUrl, vUser,vPassword)) {
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

    public static boolean checkURL(String pUrl, String pUser, String pPassword) {
        boolean vResult = false;
        try {
            String vCookie = getCookie(pUrl, pUser, pPassword);
            String vPage = getPage(pUrl, vCookie);
            vResult = vPage.contains("offline");
        } catch (Exception vEx) {
            vEx.printStackTrace();
        }
        System.out.println("Résultat de l'appel : "+vResult);
        return vResult;
    }

    private static String getPage(String pUrl, String pCookie) {
        String rPage = null;
        try {
            URL vUrl = new URL(pUrl);
            HttpURLConnection vCon = (HttpURLConnection) vUrl.openConnection();
            if (pCookie != null) {
                vCon.setRequestProperty("Cookie", pCookie);
            }
            vCon.setRequestProperty("Accept-Encoding", "gzip");
            InputStream vIs1;
            try (InputStream vIs0 = vCon.getInputStream()) {
                vIs1 = vIs0;
                if ("gzip".equals(vCon.getContentEncoding())) {
                    vIs1 = new GZIPInputStream(vIs0);
                }

                rPage = IOUtils.toString(vIs1, StandardCharsets.UTF_8.name());
            }
        } catch (Exception vEx) {
            vEx.printStackTrace();
        }
        return rPage;
    }

    private static String getCookie(String pUrl, String pUser, String pPassword) {
        String rCookie = null;
        try {
            URL vUrl = new URL(pUrl);
            String vLoginPage;
            HttpURLConnection vConCsrf = (HttpURLConnection) vUrl.openConnection();
            try (InputStream vIs0 = vConCsrf.getInputStream()) {
                vLoginPage = IOUtils.toString(vIs0, StandardCharsets.UTF_8.name());
            }
            String vCsrf = StringUtils.substringBetween(vLoginPage, "<input type=\"hidden\" name=\"csrf\" value=\"", "\" />");
            String vPost = "csrf=" + vCsrf + "&user_name=" + pUser + "&user_password=" + pPassword + "&action=login&user_rememberme=1";
            rCookie = vConCsrf.getHeaderFields().get("Set-Cookie").stream().map(vProperty -> StringUtils.substringBefore(vProperty, ";")).collect(Collectors.joining("; "));

            byte[] vPostData       = vPost.getBytes( StandardCharsets.UTF_8 );
            int    vPostDataLength = vPostData.length;

            HttpURLConnection vCon = (HttpURLConnection) vUrl.openConnection();
            vCon.setDoOutput(true);
            vCon.setInstanceFollowRedirects( false );
            vCon.setRequestMethod("POST");
            vCon.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            vCon.setRequestProperty("Cookie", rCookie);
            vCon.setRequestProperty("Origin", pUrl);
            vCon.setRequestProperty("Referer", pUrl);
            vCon.setRequestProperty("Content-Length", Integer.toString( vPostDataLength ));
            
            vCon.setUseCaches( false );
            try( DataOutputStream wr = new DataOutputStream( vCon.getOutputStream())) {
                wr.write( vPostData );
            }

            try (InputStream vIs0 = vCon.getInputStream()) {
                vLoginPage = IOUtils.toString(vIs0, StandardCharsets.UTF_8.name());
                System.out.println(vLoginPage);
            }

            rCookie = vCon.getHeaderFields().get("Set-Cookie").stream().map(vProperty -> StringUtils.substringBefore(vProperty, ";")).collect(Collectors.joining("; "));
        } catch (Exception vEx) {
            vEx.printStackTrace();
        }
        return rCookie;
    }

}
