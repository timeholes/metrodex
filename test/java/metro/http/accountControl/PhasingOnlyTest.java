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

package metro.http.accountControl;

import metro.BlockchainTest;
import metro.Metro;
import metro.PhasingParams;
import metro.VoteWeighting.MinBalanceModel;
import metro.VoteWeighting.VotingModel;
import metro.http.APICall;
import metro.http.APICall.Builder;
import metro.util.Convert;
import metro.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static metro.Constants.ONE_MTR;

public class PhasingOnlyTest extends BlockchainTest {
    @Test
    public void testSetAndGet() throws Exception {
        
        assertNoPhasingOnlyControl();
        
        setPhasingOnlyControl(VotingModel.ACCOUNT, null, 1L, null, null, new String[] {BOB.getFullIdAsString()},
                10 * ONE_MTR, 5, 1440);
        
        assertPhasingOnly(new PhasingParams(VotingModel.ACCOUNT.getCode(), 0L, 1L, 0L, (byte)0, Collections.singletonList(BOB.getFullId())),
                10 * ONE_MTR, 5, 1440);
    }
    
    @Test
    public void testAccountVoting() throws Exception {
        //all transactions must be approved either by BOB or CHUCK
        setPhasingOnlyControl(VotingModel.ACCOUNT, null, 1L, null, null, new String[] {BOB.getFullIdAsString(), CHUCK.getFullIdAsString()}, 0, 0, 0);
        
        Builder builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getFullId())
            .param("amountMQT", ONE_MTR)
            .param("feeMQT", 2 * ONE_MTR);
        
        //no phasing - block
        ACTestUtils.assertTransactionBlocked(builder);
        
      //correct phasing
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 1L, null, null, new String[] {BOB.getFullIdAsString(), CHUCK.getFullIdAsString()});
        ACTestUtils.assertTransactionSuccess(builder);
        
      //subset of the voters should also be blocked
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 1L, null, null, new String[] {BOB.getFullIdAsString()});
        ACTestUtils.assertTransactionBlocked(builder);
        
      //incorrect quorum - even if more restrictive, should also be blocked
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 2L, null, null, new String[] {BOB.getFullIdAsString(), CHUCK.getFullIdAsString()});
        ACTestUtils.assertTransactionBlocked(builder);
        
        //remove the phasing control
        builder = new ACTestUtils.Builder("setPhasingOnlyControl", ALICE.getSecretPhrase());
        
        setControlPhasingParams(builder, VotingModel.NONE, null, null, null, null, null, 0, 0, 0);
        
        setTransactionPhasingParams(builder, 3, VotingModel.ACCOUNT, null, 1L, null, null, new String[] {BOB.getFullIdAsString(), CHUCK.getFullIdAsString()});
        
        JSONObject removePhasingOnlyJSON = ACTestUtils.assertTransactionSuccess(builder);
        generateBlock();
        
        assertPhasingOnly(new PhasingParams(VotingModel.ACCOUNT.getCode(), 0L, 1L, 0L, (byte)0, Arrays.asList(BOB.getFullId(), CHUCK.getFullId())), 0, 0, 0);
        
        String fullHash = (String) removePhasingOnlyJSON.get("fullHash");
        
        //approve the remove
        builder = new ACTestUtils.Builder("approveTransaction", BOB.getSecretPhrase())
                .param("transactionFullHash", fullHash);
        ACTestUtils.assertTransactionSuccess(builder);
        
        generateBlock();

        assertNoPhasingOnlyControl();
    }

    @Test
    public void testExtraRestrictions() throws Exception {
        //all transactions must be approved either by BOB or CHUCK, total fees 5 MTR, min duration 4, max duration 100
        setPhasingOnlyControl(VotingModel.ACCOUNT, null, 1L, null, null, new String[] {BOB.getFullIdAsString(), CHUCK.getFullIdAsString()},
                5 * ONE_MTR, 4, 100);

        Builder builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
                .recipient(BOB.getFullId())
                .param("amountMQT", ONE_MTR)
                .feeMQT(7 * ONE_MTR);
        // fee too high
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 1L, null, null, new String[] {BOB.getFullIdAsString(), CHUCK.getFullIdAsString()});
        ACTestUtils.assertTransactionBlocked(builder);

        // fee at the limit
        builder.feeMQT(5 * ONE_MTR);
        JSONObject response = ACTestUtils.assertTransactionSuccess(builder);
        String fullHash = (String)response.get("fullHash");

        generateBlock();

        // not yet approved, another transaction at max fee should fail
        ACTestUtils.assertTransactionBlocked(builder);

        //approve
        Builder approveBuilder = new ACTestUtils.Builder("approveTransaction", BOB.getSecretPhrase())
                .param("transactionFullHash", fullHash);
        ACTestUtils.assertTransactionSuccess(approveBuilder);
        generateBlock();

        //now can submit next transaction
        response = ACTestUtils.assertTransactionSuccess(builder);
        fullHash = (String)response.get("fullHash");
        generateBlock();

        //approve
        approveBuilder.param("transactionFullHash", fullHash);
        ACTestUtils.assertTransactionSuccess(approveBuilder);
        generateBlock();

        //too long or too short periods should fail
        builder.param("phasingFinishHeight", Metro.getBlockchain().getHeight() + 200);
        ACTestUtils.assertTransactionBlocked(builder);
        builder.param("phasingFinishHeight", Metro.getBlockchain().getHeight() + 3);
        ACTestUtils.assertTransactionBlocked(builder);
        builder.param("phasingFinishHeight", Metro.getBlockchain().getHeight() + 4);
        ACTestUtils.assertTransactionSuccess(builder);

    }

    @Test
    public void testRejectingPendingTransaction() throws Exception {

        long AMOUNT = 1000 * ONE_MTR;

        Builder builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getFullId())
            .param("amountMQT", AMOUNT)
            .param("feeMQT", 10 * ONE_MTR);
    
        setTransactionPhasingParams(builder, 4, VotingModel.ACCOUNT, null, 1L, null, null, new String[] {BOB.getFullIdAsString(), CHUCK.getFullIdAsString()});
        JSONObject sendMoneyJSON = ACTestUtils.assertTransactionSuccess(builder);
        generateBlock();
        
        builder = new ACTestUtils.Builder("setPhasingOnlyControl", ALICE.getSecretPhrase());

        setControlPhasingParams(builder, VotingModel.ACCOUNT, null, 1L, null, null, new String[] {DAVE.getFullIdAsString()}, 0, 0, 0);
        ACTestUtils.assertTransactionSuccess(builder);
        
        generateBlock();
        
        long balanceBeforeTransactionRejection = ACTestUtils.getAccountBalance(ALICE.getFullIdAsString(), "balanceMQT");
        
        String fullHash = (String) sendMoneyJSON.get("fullHash");
        
        //approve the pending transaction - it should not confirm sendMoney
        builder = new ACTestUtils.Builder("approveTransaction", BOB.getSecretPhrase())
                .param("transactionFullHash", fullHash);
        ACTestUtils.assertTransactionSuccess(builder);
        
        generateBlock();

        Assert.assertEquals((balanceBeforeTransactionRejection  - AMOUNT) / ONE_MTR,
                ACTestUtils.getAccountBalance(ALICE.getFullIdAsString(), "unconfirmedBalanceMQT") / ONE_MTR);

        //the sendMoney finish height
        generateBlock();

        //Assert the unconfirmed balance is recovered
        Assert.assertEquals(balanceBeforeTransactionRejection / ONE_MTR,
                ACTestUtils.getAccountBalance(ALICE.getFullIdAsString(), "unconfirmedBalanceMQT") / ONE_MTR);
    }
    
    @Test
    public void testBalanceVoting() {
        setPhasingOnlyControl(VotingModel.MQT, null, 100 * ONE_MTR, null, null, null, 0, 0, 0);
        
        Builder builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getFullId())
            .param("amountMQT", ONE_MTR);
        
        //no phasing - block
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.MQT, null, 100 * ONE_MTR, null, null, new String[] {DAVE.getFullIdAsString()});
        builder.param("feeMQT", 21 * ONE_MTR); //21 is minimum
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.ACCOUNT, null, 1L, null, null, new String[] {BOB.getFullIdAsString(), CHUCK.getFullIdAsString()});
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.MQT, null, 100 * ONE_MTR + 1, null, null, null);
        ACTestUtils.assertTransactionBlocked(builder);
        
        builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getFullId())
            .param("amountMQT", ONE_MTR);
        
        setTransactionPhasingParams(builder, 20, VotingModel.MQT, null, 100 * ONE_MTR, null, null, null);
        builder.param("feeMQT", 21 * ONE_MTR); //21 is minimum
        ACTestUtils.assertTransactionSuccess(builder);
    }
    
    @Test
    public void testAssetVoting() {
        Builder builder = new ACTestUtils.AssetBuilder(ALICE.getSecretPhrase(), "TestAsset");
        builder.feeMQT(1000 * ONE_MTR);
        String assetId = (String) ACTestUtils.assertTransactionSuccess(builder).get("transaction");
        generateBlock();
        
        builder = new ACTestUtils.AssetBuilder(ALICE.getSecretPhrase(), "TestAsset2");
        builder.feeMQT(1000 * ONE_MTR);
        String asset2Id = (String) ACTestUtils.assertTransactionSuccess(builder).get("transaction");
        generateBlock();
        
        setPhasingOnlyControl(VotingModel.ASSET, assetId, 100L, null, null, null, 0, 0, 0);
        
        builder = new ACTestUtils.Builder("sendMoney", ALICE.getSecretPhrase())
            .recipient(BOB.getFullId())
            .param("amountMQT", ONE_MTR)
            .param("feeMQT", ONE_MTR);
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.ASSET, asset2Id, 100L, null, null, null);
        builder.param("feeMQT", 21 * ONE_MTR); //21 is minimum
        ACTestUtils.assertTransactionBlocked(builder);
        
        setTransactionPhasingParams(builder, 20, VotingModel.ASSET, assetId, 100L, null, null, null);
        ACTestUtils.assertTransactionSuccess(builder);
    }

    private void assertNoPhasingOnlyControl() {
        Builder builder = new APICall.Builder("getPhasingOnlyControl")
            .param("account", ALICE.getFullId().toString());
        
        JSONObject response = builder.build().invoke();
        Assert.assertTrue(response.isEmpty());
    }
    
    private void assertPhasingOnly(PhasingParams expected, long maxFees, int minDuration, int maxDuration) {
        Builder builder = new APICall.Builder("getPhasingOnlyControl")
            .param("account", ALICE.getFullId().toString());

        JSONObject response = builder.build().invoke();
        Logger.logMessage("getPhasingOnlyControl response: " + response.toJSONString());
        Assert.assertEquals(expected.getVoteWeighting().getVotingModel().getCode(), ((Long) response.get("votingModel")).byteValue());
        Assert.assertEquals(expected.getQuorum(), Convert.parseLong(response.get("quorum")));
        Assert.assertEquals(expected.getWhitelist().size(), ((JSONArray) response.get("whitelist")).size());
        Assert.assertEquals(expected.getVoteWeighting().getHoldingId(), Convert.parseUnsignedLong((String)response.get("holding")));
        Assert.assertEquals(expected.getVoteWeighting().getMinBalance(), Convert.parseLong(response.get("minBalance")));
        Assert.assertEquals(expected.getVoteWeighting().getMinBalanceModel().getCode(), ((Long) response.get("minBalanceModel")).byteValue());
        Assert.assertEquals(maxFees, Convert.parseLong(response.get("maxFees")));
        Assert.assertEquals(minDuration, ((Long)response.get("minDuration")).shortValue());
        Assert.assertEquals(maxDuration, ((Long)response.get("maxDuration")).shortValue());
    }
    
    private void setPhasingOnlyControl(VotingModel votingModel, String holdingId, Long quorum,
                                       Long minBalance, MinBalanceModel minBalanceModel, String[] whitelist,
                                       long maxFees, int minDuration, int maxDuration) {
        
        Builder builder = new ACTestUtils.Builder("setPhasingOnlyControl", ALICE.getSecretPhrase());

        setControlPhasingParams(builder, votingModel, holdingId, quorum,
                minBalance, minBalanceModel, whitelist, maxFees, minDuration, maxDuration);

        builder.feeMQT(ONE_MTR);
        APICall apiCall = builder.build();
        JSONObject response = apiCall.invoke();
        Logger.logMessage("setPhasingOnlyControl response: " + response.toJSONString());
        
        String result = (String) response.get("transaction");
        Assert.assertNotNull(result);
        
        generateBlock();
    }

    private void setControlPhasingParams(Builder builder,
                                         VotingModel votingModel, String holdingId, Long quorum,
                                         Long minBalance, MinBalanceModel minBalanceModel, String[] whitelist,
                                         long maxFees, int minDuration, int maxDuration) {
        if (votingModel != null) {
            builder.param("controlVotingModel", votingModel.getCode());
        }
        
        if (holdingId != null) {
            builder.param("controlHolding", holdingId);
        }
        
        if (quorum != null) {
            builder.param("controlQuorum", quorum);
        }
        
        if (minBalance != null) {
            builder.param("controlMinBalance", minBalance);
        }
        
        if (minBalanceModel != null) {
            builder.param("controlMinBalanceModel", minBalanceModel.getCode());
        }
        
        if (whitelist != null) {
            builder.param("controlWhitelisted", whitelist);
        }

        if (maxFees > 0) {
            builder.param("controlMaxFees", maxFees);
        }

        if (minDuration > 0) {
            builder.param("controlMinDuration", minDuration);
        }

        if (maxDuration > 0) {
            builder.param("controlMaxDuration", maxDuration);
        }

    }
    
    private void setTransactionPhasingParams(Builder builder, int finishAfter, VotingModel votingModel, String holdingId, Long quorum,
            Long minBalance, MinBalanceModel minBalanceModel, String[] whitelist) {
        
        builder.param("phased", "true");
        
        builder.param("phasingVotingModel", votingModel.getCode());
        
        builder.param("phasingFinishHeight", Metro.getBlockchain().getHeight() + finishAfter);
        
        if (holdingId != null) {
            builder.param("phasingHolding", holdingId);
        }
        
        if (quorum != null) {
            builder.param("phasingQuorum", quorum);
        }
        
        if (minBalance != null) {
            builder.param("phasingMinBalance", minBalance);
        }
        
        if (minBalanceModel != null) {
            builder.param("phasingMinBalanceModel", minBalanceModel.getCode());
        }
        
        if (whitelist != null) {
            builder.param("phasingWhitelisted", whitelist);
        }
    }
}
