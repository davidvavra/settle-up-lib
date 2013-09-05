package cz.destil.settleup.util;

import android.content.Context;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cz.destil.settleup.R;
import cz.destil.settleup.data.Preferences;
import cz.destil.settleup.data.Preferences.PrefType;
import cz.destil.settleup.data.model.Currency;
import cz.destil.settleup.data.model.Currency.Currencies;
import cz.destil.settleup.data.model.Member;
import cz.destil.settleup.data.model.Member.Members;
import cz.destil.settleup.data.model.Payment;

/**
 * Debt calculator - calculates who should pay how much to who with optional
 * tolerance value
 *
 * @author David Vavra
 */
public class DebtCalculator {

    public static int MAX_MEMBERS_FOR_OPTIMAL = 15;

    /**
     * Main algorithm, calculates who should send how much to who It optimizes
     * basic algorithm
     *
     * @param members   List of members with their credit and debts
     * @param tolerance Money value nobody cares about
     * @return List of Hashmaps encoding transactions
     */
    public static synchronized List<Debt> calculate(List<Member> members, BigDecimal tolerance, String currency) {
        List<Debt> results = new LinkedList<Debt>();
        // reset members
        for (Member member : members) {
            member.removedFromCalculation = false;
        }
        // remove members where debts are too small (1-pairs)
        for (Member member : members) {
            if (BigDecimals.isFirstSmallerOrEqual(member.getBalanceInCalculation().abs(), tolerance)) {
                member.removedFromCalculation = true;
            }
        }
        // safety check
        if (Members.getSizeInCalculation(members) == 1) {
            return results;
        }

        // for 18 and more members alghoritm would take long time, so using just
        // basic alghoritm
        if (Members.getSizeInCalculation(members) <= MAX_MEMBERS_FOR_OPTIMAL) {

            // find n-pairs, starting at 2-pairs, deal with them using basic
            // algorithm and remove them
            int n = 2;
            while (n < Members.getSizeInCalculation(members) - 1) {
                CombinationGenerator generator = new CombinationGenerator(Members.getSizeInCalculation(members), n);
                boolean nPairFound = false;
                while (generator.hasMore()) {
                    BigDecimal sum = BigDecimal.ZERO;
                    int[] combination = generator.getNext();
                    for (int i = 0; i < combination.length; i++) {
                        sum = sum.add(Members.getMemberInCalculation(members, combination[i]).getBalanceInCalculation());
                    }
                    if (BigDecimals.isFirstSmallerOrEqual(sum.abs(), tolerance)) {
                        // found n-pair - deal with them
                        List<Member> pairedMembers = new LinkedList<Member>();
                        for (int i = 0; i < combination.length; i++) {
                            pairedMembers.add(Members.getMemberInCalculation(members, combination[i]));
                        }
                        List<Debt> values = basicDebts(pairedMembers, tolerance, currency);
                        results.addAll(values);
                        // remove all paired from calculation
                        for (Member pairedMember : pairedMembers) {
                            pairedMember.removedFromCalculation = true;
                        }
                        nPairFound = true;
                    }
                    if (nPairFound) {
                        break;
                    }
                }
                if (!nPairFound) {
                    n++;
                }
            }
        }
        // deal with what is left after removing n-pairs
        List<Debt> values = basicDebts(members, tolerance, currency);
        results.addAll(values);
        return results;
    }

    /**
     * Calculates how much each member paid and spent from the payments.
     */
    public static synchronized BigDecimal updateBalances(Context c, List<Member> members, List<Payment> payments,
                                                         String currency) {
        if (members.size() == 0) {
            return BigDecimal.ZERO;
        }
        // reset members
        for (Member member : members) {
            member.paid = BigDecimal.ZERO;
            member.spent = BigDecimal.ZERO;
            member.gave = BigDecimal.ZERO;
            member.received = BigDecimal.ZERO;
        }
        long groupId = members.get(0).groupId;
        HashMap<Long, Member> idsToMembers = new HashMap<Long, Member>();
        for (Member member : members) {
            idsToMembers.put(member.id, member);
        }
        BigDecimal total = BigDecimal.ZERO;
        // for all payments
        for (Payment payment : payments) {
            if (!payment.converted && !payment.getCurrency().equals(currency)) {
                continue;
            }
            if (payment.forWho == null || payment.whoPaid == null) {
                continue;
            }

            boolean settlement = (payment.transfer || payment.purpose.equals(c.getString(R.string.debt_settlement)) || payment.purpose
                    .equals(c.getString(R.string.paypal_settlement)));

            // update paid & gave value
            int missingMembers = 1;
            for (int i = 0; i < payment.whoPaid.size(); i++) {
                long whoPaid = payment.whoPaid.get(i);
                // hack when member is missing from payments
                if (idsToMembers.get(whoPaid) == null) {
                    Member newMember = new Member(whoPaid, c.getString(R.string.missing_member) + " " + missingMembers,
                            "", groupId, null);
                    members.add(newMember);
                    idsToMembers.put(whoPaid, newMember);
                    missingMembers++;
                }
                if (settlement) {
                    if (payment.converted) {
                        idsToMembers.get(whoPaid).gave = idsToMembers.get(whoPaid).gave.add(payment.convertedAmounts.get(i));
                    } else {
                        idsToMembers.get(whoPaid).gave = idsToMembers.get(whoPaid).gave.add(payment.amounts.get(i));
                    }
                } else {
                    if (payment.converted) {
                        idsToMembers.get(whoPaid).paid = idsToMembers.get(whoPaid).paid.add(payment.convertedAmounts.get(i));
                    } else {
                        idsToMembers.get(whoPaid).paid = idsToMembers.get(whoPaid).paid.add(payment.amounts.get(i));
                    }
                }

            }

            // update spent & received values
            BigDecimal sumOfWeights = BigDecimal.ZERO;
            for (double weight : payment.weights) {
                sumOfWeights = sumOfWeights.add(new BigDecimal(weight));
            }
            BigDecimal amountForOnePerson;
            if (payment.converted) {
                amountForOnePerson = BigDecimals.safeDivide(payment.getConvertedAmount(), sumOfWeights);
            } else {
                amountForOnePerson = BigDecimals.safeDivide(payment.getAmount(), sumOfWeights);
            }
            int i = 0;
            for (long id : payment.forWho) {
                // hack when member is missing from payments
                if (idsToMembers.get(id) == null) {
                    Member newMember = new Member(id, c.getString(R.string.missing_member) + " " + missingMembers, "",
                            groupId, null);
                    members.add(newMember);
                    idsToMembers.put(id, newMember);
                    missingMembers++;
                }
                BigDecimal weightedAmount = amountForOnePerson.multiply(new BigDecimal(payment.weights.get(i)));
                if (settlement) {
                    idsToMembers.get(id).received = idsToMembers.get(id).received.add(weightedAmount);
                } else {
                    idsToMembers.get(id).spent = idsToMembers.get(id).spent.add(weightedAmount);
                }
                i++;
            }
            // Total - all payments without debts and Paypal
            if (!settlement) {
                if (payment.converted) {
                    total = total.add(payment.getConvertedAmount());
                } else {
                    total = total.add(payment.getAmount());
                }
            }
        }

        // copy it into calculation values
        for (Member member : members) {
            member.paidInCalculation = member.paid;
            member.spentInCalculation = member.spent;
        }
        return total;
    }

    /**
     * Not-optimal debts algorithm - it calculates debts with N-1 transactions
     *
     * @param members   List of members with their credit and debts
     * @param tolerance Money value nobody cares about
     * @return List of Hashmaps encoding transactions
     */
    private static synchronized List<Debt> basicDebts(List<Member> members, BigDecimal tolerance, String currency) {
        List<Debt> debts = new LinkedList<Debt>();
        int resolvedMembers = 0;
        while (resolvedMembers < Members.getSizeInCalculation(members)) {
            // transaction is from lowes balance to highest balance
            Collections.sort(members);
            Member sender = Members.getMemberInCalculation(members, 0);
            Member recipient = Members.getLastMemberInCalculation(members);
            BigDecimal senderShouldSend = sender.getBalanceInCalculation().abs();
            BigDecimal recipientShouldReceive = recipient.getBalanceInCalculation().abs();
            BigDecimal amount;
            if (BigDecimals.isFirstLarger(senderShouldSend, recipientShouldReceive)) {
                amount = recipientShouldReceive;
            } else {
                amount = senderShouldSend;
            }
            sender.spentInCalculation = sender.spentInCalculation.subtract(amount);
            recipient.paidInCalculation = recipient.paidInCalculation.subtract(amount);
            Debt debt = new Debt(sender, recipient, amount, currency);
            debts.add(debt);
            // delete members who are settled
            senderShouldSend = sender.getBalanceInCalculation().abs();
            recipientShouldReceive = recipient.getBalanceInCalculation().abs();
            if (BigDecimals.isFirstSmallerOrEqual(senderShouldSend, tolerance)) {
                resolvedMembers++;
            }
            if (BigDecimals.isFirstSmallerOrEqual(recipientShouldReceive, tolerance)) {
                resolvedMembers++;
            }
        }
        // limit transactions by tolerance
        Iterator<Debt> iterator = debts.iterator();
        while (iterator.hasNext()) {
            Debt debt = iterator.next();
            if (BigDecimals.isFirstSmallerOrEqual(debt.amount, tolerance)) {
                iterator.remove();
            }
        }
        return debts;
    }

    /**
     * Converts list of payments into common currency.
     *
     * @return if it's possible
     */
    public static synchronized List<String> getConvertedCurrencies(List<Payment> payments, Context c) {
        // reset
        for (Payment payment : payments) {
            payment.converted = false;
        }
        String singleCurrency = Preferences.getString(PrefType.SINGLE_CURRENCY, c);
        if (!singleCurrency.equals(Preferences.MULTIPLE_CURRENCIES)) {
            for (Payment payment : payments) {
                if (payment.getCurrency().equals(singleCurrency)) {
                    // already converted
                    payment.convertedAmounts = payment.amounts;
                } else {
                    Currency currency = Currencies.getByCode(payment.getCurrency());
                    if (currency == null || currency.exchangeRate <= 0 || !currency.exchangeCode.equals(singleCurrency)) {
                        // not enough exchange rates, aborting
                        for (Payment payment2 : payments) {
                            payment2.converted = false;
                        }
                        return Currencies.getDistinctCurrencies(payments);
                    }
                    payment.convertedAmounts = new ArrayList<BigDecimal>();
                    for (BigDecimal amount : payment.amounts) {
                        payment.convertedAmounts.add(BigDecimals.safeDivide(amount, new BigDecimal(currency.exchangeRate)));
                    }
                }
                payment.convertedCurrency = singleCurrency;
                payment.converted = true;
            }
            // return only single currency
            List<String> justOne = new ArrayList<String>();
            justOne.add(singleCurrency);
            return justOne;
        }
        return Currencies.getDistinctCurrencies(payments);
    }

    /**
     * Generates transactions which doesn't transfer any debt between people.
     */
    public synchronized static List<Debt> simpleDebts(List<Payment> payments, List<Member> members, String currency,
                                                      Context c) {
        List<Debt> transactions = new ArrayList<Debt>();
        // traverse all payments and reverse them into debt
        for (Payment payment : payments) {
            if (!payment.converted && !payment.getCurrency().equals(currency)) {
                continue;
            }
            // calculate paid, spent for just one payment
            List<Payment> singlePayment = new ArrayList<Payment>();
            singlePayment.add(payment);
            updateBalances(c, members, singlePayment, currency);
            // generate transactions using basicDebts
            List<Debt> debts = calculate(members, BigDecimal.ZERO, currency);
            // merge debts
            for (Debt debt : debts) {
                boolean found = false;
                Iterator<Debt> iterator = transactions.iterator();
                while (iterator.hasNext()) {
                    Debt transaction = iterator.next();
                    // add same debts
                    if (debt.from.equals(transaction.from) && debt.to.equals(transaction.to)) {
                        BigDecimal addition = transaction.amount.add(debt.amount);
                        transaction.amount = addition;
                        found = true;
                        break;
                    }
                    // subtract opposite debts
                    else if (debt.from.equals(transaction.to) && debt.to.equals(transaction.from)) {
                        BigDecimal subtraction = transaction.amount.subtract(debt.amount);
                        if (BigDecimals.isNegative(subtraction)) {
                            // debt is negative = other way around
                            subtraction = subtraction.negate();
                            transaction.to = debt.to;
                            transaction.from = debt.from;
                        }

                        //if (subtraction < DOUBLE_PRECISION_TOLERANCE) {
                        //    iterator.remove();
                        //} else {
                        transaction.amount = subtraction;
                        //}
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    transactions.add(debt);
                }
            }
        }
        return transactions;
    }

    /**
     * Class for representing debt transaction.
     *
     * @author Destil
     */
    public static class Debt implements Comparable<Debt> {
        public Member from;
        public Member to;
        public BigDecimal amount;
        public String currency;

        public Debt(Member from, Member to, BigDecimal amount, String currency) {
            this.from = from;
            this.to = to;
            this.amount = amount;
            this.currency = currency;
        }

        @Override
        public int compareTo(Debt another) {
            return this.from.name.compareTo(another.from.name);
        }

        @Override
        public String toString() {
            return from.name + " -> " + to.name + ": " + amount + " " + currency;
        }
    }
}
