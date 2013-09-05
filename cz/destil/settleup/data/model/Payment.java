package cz.destil.settleup.data.model;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import cz.destil.settleup.util.BigDecimals;
import cz.destil.settleup.util.Utils;

public class Payment implements Serializable, Comparable<Payment> {
    public List<BigDecimal> amounts;
    public List<Long> whoPaid;
    public List<Long> forWho;
    public List<Double> weights;
    public String purpose;
    public long datetime;
    public long id;
    public long groupId;
    public boolean transfer;
    public String onlineId;
    // for converting to single currency
    public List<BigDecimal> convertedAmounts;
    public String convertedCurrency;
    public boolean converted = false;
    private String currency;

    public Payment(String amounts, String forWho, String purpose, String whoPaid, String weights, long datetime,
                   String currency, boolean transfer, long groupId, String onlineId) {
        this.amounts = Utils.splitToBigDecimals(amounts);
        this.whoPaid = Utils.splitToLongs(whoPaid);
        this.forWho = Utils.splitToLongs(forWho);
        this.purpose = purpose;
        this.weights = Utils.splitToDoubles(weights);
        // Needed otherwise it will crash on payments from demo group and
        // elsewhere
        normalizeWeights();
        this.currency = currency;
        this.datetime = datetime;
        this.transfer = transfer;
        this.groupId = groupId;
        this.onlineId = onlineId;
    }

    public BigDecimal getAmount() {
        return BigDecimals.sum(amounts);
    }

    public void setAmount(BigDecimal amount) {
        this.amounts = new ArrayList<BigDecimal>();
        amounts.add(amount);
    }

    public BigDecimal getConvertedAmount() {
        return BigDecimals.sum(convertedAmounts);
    }

    /**
     * Aligns weights with for_who, kind of a hack
     */
    public void normalizeWeights() {
        if (weights == null || (forWho != null && weights.size() != forWho.size())) {
            List<Double> weightsList = new ArrayList<Double>();
            if (forWho != null) {
                for (@SuppressWarnings("unused")
                long _ : forWho) {
                    weightsList.add((double) 1);
                }
            }
            weights = weightsList;
        }
    }

    public String getCurrency() {
        if (TextUtils.isEmpty(currency)) {
            return Payments.getLastCurrency(groupId);
        } else {
            return currency;
        }
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "a=" + this.amounts + "who=" + this.whoPaid + "for=" + this.forWho + "pur=" + this.purpose + "wei="
                + this.weights + "curr=" + this.currency;
    }

    @Override
    public int compareTo(Payment another) {
        if (this.equals(another))
            return 0;
        if (datetime > another.datetime)
            return -1;
        else
            return 1;
    }
}
