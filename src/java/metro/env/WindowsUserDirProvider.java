/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2018 metro.software
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package metro.env;

import metro.Metro;

import java.nio.file.Paths;
import java.util.Locale;

public class WindowsUserDirProvider extends DesktopUserDirProvider {

    private static final String METRO_USER_HOME = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", Metro.APPLICATION.toUpperCase(Locale.ROOT)).toString();

    @Override
    public String getUserHomeDir() {
        return METRO_USER_HOME;
    }
}
