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

package metro.http;

import metro.Account;
import metro.MetroException;
import metro.db.DbIterator;
import metro.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccount extends APIServlet.APIRequestHandler {

    static final GetAccount instance = new GetAccount();

    private GetAccount() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "includeLessors", "includeAssets", "includeEffectiveBalance");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws MetroException {

        Account account = ParameterParser.getAccount(req);
        boolean includeLessors = "true".equalsIgnoreCase(req.getParameter("includeLessors"));
        boolean includeAssets = "true".equalsIgnoreCase(req.getParameter("includeAssets"));
        boolean includeEffectiveBalance = "true".equalsIgnoreCase(req.getParameter("includeEffectiveBalance"));

        JSONObject response = JSONData.accountBalance(account, includeEffectiveBalance);
        JSONData.putAccount(response, "account", account.getId(), account.getId2());

        byte[] publicKey = Account.getPublicKey(account.getId());
        if (publicKey != null) {
            response.put("publicKey", Convert.toHexString(publicKey));
        }
        Account.AccountInfo accountInfo = account.getAccountInfo();
        if (accountInfo != null) {
            response.put("name", Convert.nullToEmpty(accountInfo.getName()));
            response.put("description", Convert.nullToEmpty(accountInfo.getDescription()));
        }
        Account.AccountLease accountLease = account.getAccountLease();
        if (accountLease != null) {
            //FIXME #220 optimize
            Account.FullId leaseFullId = Account.getAccount(accountLease.getCurrentLesseeId()).getFullId();
            JSONData.putAccount(response, "currentLessee", leaseFullId);
            response.put("currentLeasingHeightFrom", accountLease.getCurrentLeasingHeightFrom());
            response.put("currentLeasingHeightTo", accountLease.getCurrentLeasingHeightTo());
            if (accountLease.getNextLesseeId() != 0) {
                //FIXME #220 optimize
                Account.FullId nextLeaseFullId = Account.getAccount(accountLease.getNextLesseeId()).getFullId();
                JSONData.putAccount(response, "nextLessee", nextLeaseFullId);
                response.put("nextLeasingHeightFrom", accountLease.getNextLeasingHeightFrom());
                response.put("nextLeasingHeightTo", accountLease.getNextLeasingHeightTo());
            }
        }

        if (!account.getControls().isEmpty()) {
            JSONArray accountControlsJson = new JSONArray();
            account.getControls().forEach(accountControl -> accountControlsJson.add(accountControl.toString()));
            response.put("accountControls", accountControlsJson);
        }

        if (includeLessors) {
            try (DbIterator<Account> lessors = account.getLessors()) {
                if (lessors.hasNext()) {
                    JSONArray lessorIds = new JSONArray();
                    JSONArray lessorIdsRS = new JSONArray();
                    JSONArray lessorInfo = new JSONArray();
                    while (lessors.hasNext()) {
                        Account lessor = lessors.next();
                        lessorIds.add(Long.toUnsignedString(lessor.getId()));
                        lessorIdsRS.add(Convert.rsAccount(lessor.getId(), lessor.getId2()));
                        lessorInfo.add(JSONData.lessor(lessor, includeEffectiveBalance));
                    }
                    response.put("lessors", lessorIds);
                    response.put("lessorsRS", lessorIdsRS);
                    response.put("lessorsInfo", lessorInfo);
                }
            }
        }

        if (includeAssets) {
            try (DbIterator<Account.AccountAsset> accountAssets = account.getAssets(0, -1)) {
                JSONArray assetBalances = new JSONArray();
                JSONArray unconfirmedAssetBalances = new JSONArray();
                while (accountAssets.hasNext()) {
                    Account.AccountAsset accountAsset = accountAssets.next();
                    JSONObject assetBalance = new JSONObject();
                    assetBalance.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
                    assetBalance.put("balanceQNT", String.valueOf(accountAsset.getQuantityQNT()));
                    assetBalances.add(assetBalance);
                    JSONObject unconfirmedAssetBalance = new JSONObject();
                    unconfirmedAssetBalance.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
                    unconfirmedAssetBalance.put("unconfirmedBalanceQNT", String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
                    unconfirmedAssetBalances.add(unconfirmedAssetBalance);
                }
                if (assetBalances.size() > 0) {
                    response.put("assetBalances", assetBalances);
                }
                if (unconfirmedAssetBalances.size() > 0) {
                    response.put("unconfirmedAssetBalances", unconfirmedAssetBalances);
                }
            }
        }

        return response;

    }

}
