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

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Common two red alarm program and crawler
 *
 * @author qlefevre
 */
abstract class AbstractCommonMain {

    protected static boolean checkURL(String pUrl, String pUser, String pPassword) {
        boolean vResult = false;
        try {
            String vCookie = getCookie(pUrl, pUser, pPassword);
            String vPage = getPage(pUrl, vCookie);
            vResult = vPage.contains("offline");
        } catch (Exception vEx) {
            vEx.printStackTrace();
        }
        System.out.println("Résultat de l'appel : " + vResult);
        return vResult;
    }

    protected static String getPage(String pUrl, String pCookie) {
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

    protected static String getCookie(String pUrl, String pUser, String pPassword) {
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

            byte[] vPostData = vPost.getBytes(StandardCharsets.UTF_8);
            int vPostDataLength = vPostData.length;

            HttpURLConnection vCon = (HttpURLConnection) vUrl.openConnection();
            vCon.setDoOutput(true);
            vCon.setInstanceFollowRedirects(false);
            vCon.setRequestMethod("POST");
            vCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            vCon.setRequestProperty("Cookie", rCookie);
            vCon.setRequestProperty("Origin", pUrl);
            vCon.setRequestProperty("Referer", pUrl);
            vCon.setRequestProperty("Content-Length", Integer.toString(vPostDataLength));

            vCon.setUseCaches(false);
            try (DataOutputStream wr = new DataOutputStream(vCon.getOutputStream())) {
                wr.write(vPostData);
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

    /**
     * Retourne une instance de CommandLine pour récupérer les options données en paramètres
     *
     * @param pArgs les arguments donnés au programme
     *
     * @return une instance de CommandLine pour récupérer les options données en paramètres
     * @throws ParseException
     */
    protected static CommandLine getCommandLine(String[] pArgs) throws ParseException {

        // Récupère les options
        Options vOptions = new Options();
        vOptions.addOption("h", "help", false, "show help");
        vOptions.addOption("u", "user", true, "user");
        vOptions.addOption("p", "password", true, "password");
        vOptions.addOption("s", "site", true, "site to check");
        vOptions.addOption("c", "com", true, "port com");
        CommandLineParser vCmdLineParser = new DefaultParser();
        CommandLine rCommandLine = vCmdLineParser.parse(vOptions, pArgs);

        // On affiche l'aide si besoin
        if (rCommandLine.hasOption('h') || !rCommandLine.hasOption('p') || !rCommandLine.hasOption('u')) {
            HelpFormatter vformatter = new HelpFormatter();
            vformatter.printHelp(Main.class.getSimpleName(), vOptions);
        }

        return rCommandLine;
    }

}
