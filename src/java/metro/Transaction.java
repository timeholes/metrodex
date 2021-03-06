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

package metro;

import metro.util.Filter;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONObject;

import java.util.List;

public interface Transaction {

    interface Builder {

        Builder recipientFullId(Account.FullId recipientFullId);

        Builder referencedTransactionFullHash(String referencedTransactionFullHash);

        Builder appendix(Appendix.Message message);

        Builder appendix(Appendix.EncryptedMessage encryptedMessage);

        Builder appendix(Appendix.EncryptToSelfMessage encryptToSelfMessage);

        Builder appendix(Appendix.PublicKeyAnnouncement publicKeyAnnouncement);

        Builder appendix(Appendix.PrunablePlainMessage prunablePlainMessage);

        Builder appendix(Appendix.PrunableEncryptedMessage prunableEncryptedMessage);

        Builder appendix(Appendix.Phasing phasing);

        Builder timestamp(long timestamp);

        Builder ecBlockHeight(int height);

        Builder ecBlockId(long blockId);

        Transaction build() throws MetroException.NotValidException;

        Transaction build(String secretPhrase) throws MetroException.NotValidException;

    }

    long getId();

    String getStringId();

    long getSenderId();

    int getSenderId2();

    byte[] getSenderPublicKey();

    Account.FullId getSenderFullId();

    long getRecipientId();

    int getRecipientId2();

    Account.FullId getRecipientFullId();

    int getHeight();

    long getBlockId();

    Block getBlock();

    short getIndex();

    long getTimestamp();

    long getBlockTimestamp();

    short getDeadline();

    long getExpiration();

    long getAmountMQT();

    long getFeeMQT();

    String getReferencedTransactionFullHash();

    byte[] getSignature();

    String getFullHash();

    TransactionType getType();

    Attachment getAttachment();

    boolean verifySignature();

    void validate() throws MetroException.ValidationException;

    byte[] getBytes();

    byte[] getUnsignedBytes();

    JSONObject getJSONObject();

    JSONObject getPrunableAttachmentJSON();

    byte getVersion();

    int getFullSize();

    Appendix.Message getMessage();

    Appendix.EncryptedMessage getEncryptedMessage();

    Appendix.EncryptToSelfMessage getEncryptToSelfMessage();

    Appendix.Phasing getPhasing();

    Appendix.PrunablePlainMessage getPrunablePlainMessage();

    Appendix.PrunableEncryptedMessage getPrunableEncryptedMessage();

    List<? extends Appendix> getAppendages();

    List<? extends Appendix> getAppendages(boolean includeExpiredPrunable);

    List<? extends Appendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable);

    int getECBlockHeight();

    long getECBlockId();

    long[] getBackFees();
}
