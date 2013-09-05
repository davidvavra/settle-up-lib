package cz.destil.settleup.data.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.destil.settleup.util.BigDecimals;

public class Member implements Comparable<Member> {

	public static String TAG = "Member";

	public long id;
	public String onlineId;
	public String name;
	public String email;
	public double defaultWeight;
	public boolean groupShared;
	public long groupId;
	// for debt calculation
	public BigDecimal paid;
	public BigDecimal spent;
	public BigDecimal gave;
	public BigDecimal received;
	public boolean removedFromCalculation;
	public BigDecimal paidInCalculation;
	public BigDecimal spentInCalculation;
	public String sharedToEmail;
	public boolean disabled;
	public String bankAccount;

	public Member(long id, String name, String email, long groupId, String bankAccount) {
		this.id = id; // will be changed by insert operation
		this.name = name;
		this.email = email;
		this.defaultWeight = 1;
		this.groupShared = false;
		this.groupId = groupId;
		this.removedFromCalculation = false;
		this.sharedToEmail = email;
		this.disabled = false;
		this.bankAccount = bankAccount;
	}

	public Member(long id, String name, String email, double defaultWeight, int groupShared, long groupId,
			String onlineId, String sharedToEmail, int disabled, String bankAccount) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.defaultWeight = defaultWeight;
		this.groupShared = (groupShared == 1);
		this.groupId = groupId;
		this.onlineId = onlineId;
		this.removedFromCalculation = false;
		this.sharedToEmail = sharedToEmail;
		this.disabled = (disabled == 1);
		this.bankAccount = bankAccount;
	}

	public BigDecimal getBalance() {
        BigDecimal balance = BigDecimal.ZERO;
        balance = balance.add(paid);
        balance = balance.add(gave);
        balance = balance.subtract(spent);
        balance = balance.subtract(received);
        balance = BigDecimals.roundAlmostZero(balance);
		return balance;
	}

	public BigDecimal getBalanceInCalculation() {
        BigDecimal balance = BigDecimal.ZERO;
        balance = balance.add(paidInCalculation);
        balance = balance.add(gave);
        balance = balance.subtract(spentInCalculation);
        balance = balance.subtract(received);
        balance = BigDecimals.roundAlmostZero(balance);
		return balance;
	}

	@Override
	public String toString() {
		return name + ":" + " p: " + paid + " g: " + gave + " s: " + spent + " r: " + received + " balance: "
				+ getBalance();
	}

	@Override
	public int compareTo(Member anotherMember) {
		return getBalanceInCalculation().compareTo(anotherMember.getBalanceInCalculation());
	}

	public static final class Members {
		private Members() {
		}

		public static int getSizeInCalculation(List<Member> members) {
			int size = members.size();
			for (Member member : members) {
				if (member.removedFromCalculation) {
					size--;
				}
			}
			return size;
		}

		public static Member getMemberInCalculation(List<Member> members, int i) {
			int j = 0;
			for (Member member : members) {
				if (member.removedFromCalculation) {
					continue;
				}
				if (i == j) {
					return member;
				}
				j++;
			}
			return null;
		}

		public static Member getLastMemberInCalculation(List<Member> members) {
			return getMemberInCalculation(members, getSizeInCalculation(members) - 1);
		}
	}
}
