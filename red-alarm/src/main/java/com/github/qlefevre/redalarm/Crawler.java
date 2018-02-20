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

import static com.github.qlefevre.redalarm.AbstractCommonMain.getCommandLine;
import org.apache.commons.cli.CommandLine;

/**
 *
 * @author dsdsystem
 */
public class Crawler extends AbstractCommonMain {

    public static void main(String[] pArgs) throws Exception {
        CommandLine vCommandLine = getCommandLine(pArgs);

        String vUser = vCommandLine.getOptionValue("u");
        String vPassword = vCommandLine.getOptionValue("p");
        String vUrl = vCommandLine.getOptionValue("s");

        System.out.println("Update servers");
        checkURL(vUrl, vUser, vPassword);
    }
}
